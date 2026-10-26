import { test } from 'node:test';
import assert from 'node:assert/strict';
import AdmZip from 'adm-zip';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { latestStable, releaseVersion, composite, readZip, payload, assemble, reuse, libraries } from './site.mjs';

const root = fileURLToPath(new URL('../../../', import.meta.url));
function zip(files) {
  const archive = new AdmZip();
  for (const [name, data] of files) archive.addFile(name, Buffer.from(data));
  return archive.toBuffer();
}
function fixture(version) {
  const bundleVersion = version.replace('-', '.').replace('.rc.1', '.rc-1');
  const units = libraries.flatMap(library => [`<unit id="${library}" version="${bundleVersion}"/>`, `<unit id="${library}.feature.feature.group" version="${bundleVersion}">${library === libraries[0] ? '' : `<requires><required name="${libraries[0]}.feature.feature.group"/></requires>`}</unit>`]).join('');
  const files = new Map([
    ['docs/index.html', Buffer.from(version)],
    ['p2/content.jar', zip([['content.xml', `<repository><units>${units}</units></repository>`]])],
    ['p2/artifacts.jar', zip([['artifacts.xml', `<repository><artifacts>${libraries.map(library => `<artifact id="${library}" version="${bundleVersion}" classifier="osgi.bundle"/>`).join('')}</artifacts></repository>`]])],
    ['p2/p2.index', Buffer.from('version = 1')]
  ]);
  for (const library of libraries) {
    files.set(`docs/api/${library}/index.html`, Buffer.from('API'));
    files.set(`p2/plugins/${library}_${bundleVersion}.jar`, Buffer.from('bundle'));
    files.set(`p2/features/${library}.feature_${bundleVersion}.jar`, Buffer.from('feature'));
  }
  const manifest = { schema: 1, version, bundleVersion, tag: `v${version}`, commit: 'a'.repeat(40), prerelease: version.includes('-'), files: Object.fromEntries([...files].map(([name, data]) => [name, crypto.createHash('sha256').update(data).digest('hex')])) };
  files.set('release.json', Buffer.from(JSON.stringify(manifest)));
  return zip(files);
}
function temporary(testContext) {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'swtbot-site-'));
  testContext.after(() => fs.rmSync(directory, { recursive: true, force: true }));
  return directory;
}

test('latest is highest stable SemVer, independent of release order', () => {
  assert.equal(latestStable(['0.9.0', '0.10.0', '1.0.0-rc.1', '0.1.0']), '0.10.0');
  assert.equal(latestStable(['1.0.0-rc.1']), undefined);
});
test('release identity rejects traversal, snapshots and build metadata', () => {
  for (const version of ['../1.0.0', 'v1.0.0', '01.0.0', '1.0', '1.0.0+meta', '1.0.0-SNAPSHOT']) assert.throws(() => releaseVersion(version));
});
test('p2 latest uses relative repository children, not HTML redirects', () => {
  assert.match(composite('0.10.0', 'Content'), /CompositeContentRepository/);
  assert.match(composite('0.10.0', 'Artifact'), /CompositeArtifactRepository/);
  assert.match(composite('0.10.0', 'Content'), /location="\.\.\/0.10.0\/"/);
});
test('unsafe archive names and unvalidated payloads fail closed', () => {
  const archive = new AdmZip();
  archive.addFile('C:escape.txt', Buffer.from('unsafe'));
  assert.throws(() => readZip(archive.toBuffer()), /Unsafe archive path/);
  assert.throws(() => payload(new AdmZip().toBuffer()), /Missing site manifest/);
});

test('history retains versions; older reruns and prereleases cannot roll latest back', context => {
  const directory = temporary(context);
  const inputs = ['0.10.0', '0.9.0', '1.0.0-rc.1'].map(version => {
    const target = path.join(directory, `${version}.zip`);
    fs.writeFileSync(target, fixture(version));
    return target;
  });
  for (const [index, order] of [inputs, [...inputs].reverse()].entries()) {
    const output = path.join(directory, `site-${index}`);
    assemble(order, output, root);
    assert.equal(fs.readFileSync(path.join(output, 'docs/latest/index.html'), 'utf8'), '0.10.0');
    assert.ok(fs.existsSync(path.join(output, 'docs/0.9.0/index.html')));
    assert.match(fs.readFileSync(path.join(output, 'p2/latest/compositeContent.xml'), 'utf8'), /0.10.0/);
    assert.throws(() => assemble(order, output, root), /must be empty/);
  }
});
test('first prerelease does not create stable aliases', context => {
  const directory = temporary(context);
  const input = path.join(directory, 'rc.zip');
  fs.writeFileSync(input, fixture('1.0.0-rc.1'));
  const output = path.join(directory, 'site');
  assemble([input], output, root);
  assert.equal(fs.existsSync(path.join(output, 'docs/latest')), false);
  assert.equal(fs.existsSync(path.join(output, 'p2/latest')), false);
});
test('checksum corruption, manifest conflicts and duplicate versions fail closed', context => {
  const directory = temporary(context);
  const archive = new AdmZip(fixture('0.1.0'));
  archive.updateFile('docs/index.html', Buffer.from('tampered'));
  assert.throws(() => payload(archive.toBuffer()), /Checksum mismatch/);
  const input = path.join(directory, 'release.zip');
  fs.writeFileSync(input, fixture('0.1.0'));
  assert.throws(() => reuse(input, path.join(directory, 'reused'), '0.1.0', 'b'.repeat(40)), /another tag commit/);
  assert.throws(() => assemble([input, input], path.join(directory, 'site'), root), /Duplicate release/);
  reuse(input, path.join(directory, 'reused'), '0.1.0', 'a'.repeat(40));
  assert.deepEqual(fs.readFileSync(path.join(directory, 'reused/swtbot-test-support-site-0.1.0.zip')), fs.readFileSync(input));
});
test('Javadoc gate rejects undocumented public methods and invalid references', context => {
  const directory = temporary(context);
  const javadoc = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin', process.platform === 'win32' ? 'javadoc.exe' : 'javadoc') : 'javadoc';
  for (const [index, comment] of ['', '/** Returns a value. @return {@link MissingType} */'].entries()) {
    const source = path.join(directory, 'Probe.java');
    fs.writeFileSync(source, `/** Test API. */ public interface Probe { ${comment}\n int value(); }`);
    const result = spawnSync(javadoc, ['-quiet', '-Xdoclint:all', '-Werror', '-d', path.join(directory, `out-${index}`), source], { encoding: 'utf8' });
    assert.ifError(result.error);
    assert.notEqual(result.status, 0);
    assert.match(result.stderr, index === 0 ? /no comment/ : /reference not found/);
  }
});

test('API doclet rejects undocumented overrides even when inherited docs exist', context => {
  const directory = temporary(context);
  const executable = name => process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin', process.platform === 'win32' ? `${name}.exe` : name) : name;
  const compiled = spawnSync(executable('javac'), ['-d', directory, path.join(root, '.github/scripts/site/ApiDoclet.java')], { encoding: 'utf8' });
  assert.ifError(compiled.error);
  assert.equal(compiled.status, 0, compiled.stderr);
  const source = path.join(directory, 'Probe.java');
  fs.writeFileSync(source, '/** Test API. */ public interface Probe { /** Parent API. */ interface Parent { /** Gets value.\n @return value */ int value(); } /** Child API. */ interface Child extends Parent { @Override int value(); } }');
  const result = spawnSync(executable('javadoc'), ['-quiet', '-doclet', 'ApiDoclet', '-docletpath', directory, '-d', path.join(directory, 'docs'), source], { encoding: 'utf8' });
  assert.ifError(result.error);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /Missing explicit public API Javadoc: value\(\)/);
});