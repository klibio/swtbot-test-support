import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';
import AdmZip from 'adm-zip';
import MarkdownIt from 'markdown-it';
import semver from 'semver';
import { XMLParser, XMLValidator } from 'fast-xml-parser';

export const libraries = ['io.klib.swtbot.support', 'io.klib.swtbot.junit4', 'io.klib.swtbot.jupiter'];
const pages = ['index', 'installation', 'screenshots', 'junit', 'troubleshooting'];
const labels = ['Overview', 'Installation', 'Screenshots', 'JUnit Adapters', 'Troubleshooting'];
const markdown = new MarkdownIt({ html: false, linkify: true });
const xml = new XMLParser({ ignoreAttributes: false, attributeNamePrefix: '', isArray: name => ['unit', 'artifact', 'requires', 'required', 'plugin', 'import'].includes(name) });
const digest = data => crypto.createHash('sha256').update(data).digest('hex');
const escape = value => String(value).replace(/[&<>"']/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[character]);
const check = (condition, message) => { if (!condition) throw new Error(message); };

export function releaseVersion(version) {
  check(semver.valid(version) === version && !version.includes('+'), `Invalid release version: ${version}`);
  const qualifier = semver.prerelease(version)?.join('.').toUpperCase();
  check(qualifier !== 'SNAPSHOT' && !qualifier?.endsWith('-SNAPSHOT'), 'Snapshots are not releases');
  return version;
}

function safeName(name) {
  check(typeof name === 'string' && name.length > 0 && !name.includes('\\') && !name.includes('\0') &&
    !path.posix.isAbsolute(name) && !name.includes(':') && !name.includes('//') && !name.split('/').some(part => part === '..' || part === '.'), `Unsafe archive path: ${name}`);
  return name;
}

export function readZip(input) {
  check(Buffer.isBuffer(input), 'Missing ZIP archive');
  const result = new Map();
  let total = 0;
  for (const entry of new AdmZip(input).getEntries()) {
    safeName(entry.entryName);
    check(((entry.header.attr >>> 16) & 0o170000) !== 0o120000, 'Archive symlinks are forbidden');
    if (entry.isDirectory) continue;
    total += entry.header.size;
    check(total < 500_000_000 && !result.has(entry.entryName), 'Oversized archive or duplicate path');
    result.set(entry.entryName, entry.getData());
  }
  return result;
}

function writeZip(files, target) {
  const archive = new AdmZip();
  for (const [name, data] of [...files].sort(([left], [right]) => left.localeCompare(right))) {
    archive.addFile(safeName(name), data);
    archive.getEntry(name).header.time = new Date(2000, 0, 1);
  }
  fs.mkdirSync(path.dirname(target), { recursive: true });
  archive.writeZip(target);
}

function walk(directory, prefix = '') {
  const files = new Map();
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    check(!entry.isSymbolicLink(), `Symlink in documentation: ${entry.name}`);
    const name = `${prefix}${entry.name}`;
    const source = path.join(directory, entry.name);
    if (entry.isDirectory()) for (const item of walk(source, `${name}/`)) files.set(...item);
    else files.set(name, fs.readFileSync(source));
  }
  return files;
}

function document(title, body, navigation = '') {
  return `<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${escape(title)} | SWTBot Test Support</title><link rel="stylesheet" href="style.css"></head><body><header><a href="index.html">SWTBot Test Support</a></header><div class="layout"><nav aria-label="Documentation">${navigation}</nav><main>${body}</main></div></body></html>`;
}

function parseXml(data, label) {
  check(data && XMLValidator.validate(data.toString()) === true, `Invalid XML: ${label}`);
  return xml.parse(data.toString());
}

export function validateP2(files, bundleVersion) {
  const metadata = readZip(files.get('content.jar'));
  const artifacts = readZip(files.get('artifacts.jar'));
  const units = parseXml(metadata.get('content.xml'), 'content.xml').repository.units.unit;
  const artifactList = parseXml(artifacts.get('artifacts.xml'), 'artifacts.xml').repository.artifacts.artifact;
  const plugins = [...files.keys()].filter(name => name.startsWith('plugins/'));
  check(plugins.length === 3, 'p2 must contain exactly three library bundles');
  for (const library of libraries) {
    check(units.some(unit => unit.id === library && unit.version === bundleVersion), `Missing exact p2 unit ${library}`);
    check(artifactList.some(item => item.id === library && item.version === bundleVersion && item.classifier === 'osgi.bundle'), `Missing artifact ${library}`);
    check(plugins.some(name => name === `plugins/${library}_${bundleVersion}.jar` || name === `plugins/${library}-${bundleVersion}.jar`), `Unexpected plugin path for ${library}`);
    const feature = units.find(unit => unit.id === `${library}.feature.feature.group`);
    check(feature?.version === bundleVersion, `Missing feature for ${library}`);
    if (library !== libraries[0]) check(feature.requires?.[0]?.required?.some(requirement => requirement.name === `${libraries[0]}.feature.feature.group`), `Missing core requirement in ${library}`);
  }
  check(files.has('p2.index') && [...files.keys()].filter(name => name.startsWith('features/')).length === 3, 'Missing p2 index/features');
}

function validateManifest(manifest) {
  check(manifest.schema === 1, 'Unsupported site manifest');
  releaseVersion(manifest.version);
  check(manifest.tag === `v${manifest.version}` && /^[0-9a-f]{40}$/.test(manifest.commit), 'Invalid site release identity');
  const expectedBundle = manifest.version.replace(/-/, '.').replace(/(?<=\d\.\d+\.\d+\.).*/, qualifier => qualifier.replaceAll('.', '-'));
  check(manifest.bundleVersion === expectedBundle, 'OSGi/Maven version mismatch');
  check(manifest.prerelease === Boolean(semver.prerelease(manifest.version)), 'Prerelease mismatch');
  check(manifest.files && Object.keys(manifest.files).length > 0, 'Missing file checksums');
}

export function payload(input) {
  const files = readZip(input);
  check(files.has('release.json'), 'Missing site manifest');
  const manifest = JSON.parse(files.get('release.json'));
  validateManifest(manifest);
  check(files.size === Object.keys(manifest.files).length + 1, 'Untracked files in payload');
  for (const [name, hash] of Object.entries(manifest.files)) {
    safeName(name);
    check(name.startsWith('docs/') || name.startsWith('p2/'), 'Unexpected payload root');
    check(files.has(name) && digest(files.get(name)) === hash, `Checksum mismatch: ${name}`);
  }
  for (const library of libraries) check(files.has(`docs/api/${library}/index.html`), `Missing API reference ${library}`);
  check(files.has('docs/index.html'), 'Missing user guide');
  validateP2(new Map([...files].filter(([name]) => name.startsWith('p2/')).map(([name, data]) => [name.slice(3), data])), manifest.bundleVersion);
  return { manifest, files };
}

export function prepare(root, output, version, bundleVersion, commit) {
  releaseVersion(version);
  const p2 = readZip(fs.readFileSync(path.join(root, 'build/p2/p2')));
  p2.delete('META-INF/MANIFEST.MF');
  validateP2(p2, bundleVersion);
  p2.set('index.html', Buffer.from(document(`p2 ${version}`, `<h1>SWTBot Test Support ${escape(version)}</h1><p>p2 update site</p><code>https://klibio.github.io/swtbot-test-support/p2/${escape(version)}/</code><p><a href="../../docs/${escape(version)}/installation.html">Installation and dependencies</a></p>`).replace('href="style.css"', 'href="../../style.css"')));
  const files = new Map([...p2].map(([name, data]) => [`p2/${name}`, data]));
  const navigation = pages.map((page, index) => `<a href="${page}.html">${labels[index]}</a>`).join('') + libraries.map(library => `<a href="api/${library}/index.html">${escape(library)} API</a>`).join('');
  for (const page of pages) {
    const source = fs.readFileSync(path.join(root, `docs/${page}.md`), 'utf8').replaceAll('@VERSION@', version);
    files.set(`docs/${page}.html`, Buffer.from(document(`${labels[pages.indexOf(page)]} ${version}`, `<p class="version">Release ${escape(version)} · <a href="../../index.html">All versions</a></p>${markdown.render(source)}`, navigation)));
  }
  files.set('docs/style.css', fs.readFileSync(path.join(root, 'docs/style.css')));
  for (const library of libraries) {
    const api = walk(path.join(root, library, 'generated/api-javadoc'));
    check(api.has('index.html'), `Missing Javadoc ${library}`);
    for (const [name, data] of api) files.set(`docs/api/${library}/${name}`, data);
    const plugin = [...p2].find(([name]) => name.startsWith(`plugins/${library}_`) || name.startsWith(`plugins/${library}-`));
    const jar = fs.readFileSync(path.join(root, library, `generated/${library}.jar`));
    check(plugin && digest(plugin[1]) === digest(jar), `p2 differs from tagged bundle ${library}`);
  }
  const manifest = { schema: 1, version, bundleVersion, tag: `v${version}`, commit, prerelease: Boolean(semver.prerelease(version)), files: Object.fromEntries([...files].map(([name, data]) => [name, digest(data)])) };
  files.set('release.json', Buffer.from(JSON.stringify(manifest, null, 2)));
  validateManifest(manifest);
  writeZip(files, path.join(output, `swtbot-test-support-site-${version}.zip`));
  writeZip(p2, path.join(output, `swtbot-test-support-p2-${version}.zip`));
}

export function latestStable(versions) {
  return versions.filter(version => !semver.prerelease(releaseVersion(version))).sort(semver.rcompare)[0];
}

export function composite(version, kind) {
  releaseVersion(version);
  const type = kind === 'Content' ? 'metadata' : 'artifact';
  return `<?xml version="1.0" encoding="UTF-8"?>\n<?composite${kind}Repository version="1.0.0"?>\n<repository name="SWTBot Test Support latest stable" type="org.eclipse.equinox.internal.p2.${type}.repository.Composite${kind}Repository" version="1.0.0"><properties size="1"><property name="p2.timestamp" value="0"/></properties><children size="1"><child location="../${escape(version)}/"/></children></repository>\n`;
}

export function assemble(inputs, output, root) {
  check(!fs.existsSync(output) || fs.readdirSync(output).length === 0, 'Pages output must be empty');
  const releases = inputs.map(input => payload(fs.readFileSync(input))).sort((left, right) => semver.rcompare(left.manifest.version, right.manifest.version));
  check(releases.length > 0, 'No future release site assets found');
  check(new Set(releases.map(release => release.manifest.version)).size === releases.length, 'Duplicate release version');
  const latest = latestStable(releases.map(release => release.manifest.version));
  let size = 0;
  function write(name, data) {
    size += Buffer.byteLength(data);
    check(size < 950_000_000, 'Pages size limit approached; explicit retention decision required');
    const target = path.join(output, safeName(name));
    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.writeFileSync(target, data);
  }
  for (const { manifest, files } of releases) {
    for (const [name, data] of files) {
      if (name === 'release.json') continue;
      const [section, ...rest] = name.split('/');
      write(`${section}/${manifest.version}/${rest.join('/')}`, data);
      if (section === 'docs' && manifest.version === latest) write(`docs/latest/${rest.join('/')}`, data);
    }
  }
  if (latest) {
    write('p2/latest/compositeContent.xml', composite(latest, 'Content'));
    write('p2/latest/compositeArtifacts.xml', composite(latest, 'Artifact'));
    write('p2/latest/p2.index', 'version = 1\nmetadata.repository.factory.order = compositeContent.xml,\\!\nartifact.repository.factory.order = compositeArtifacts.xml,\\!\n');
    write('p2/latest/index.html', document('Latest Stable p2', `<h1>Latest Stable p2</h1><p>Current stable release: <a href="../${latest}/">${latest}</a></p><p><a href="../../docs/latest/installation.html">Installation and dependencies</a></p>`).replace('href="style.css"', 'href="../../style.css"'));
  }
  const rows = releases.map(({ manifest }) => `<tr><th scope="row">${escape(manifest.version)}${manifest.prerelease ? ' (prerelease)' : ''}</th><td><a href="docs/${manifest.version}/index.html">Guide and API</a></td><td><a href="p2/${manifest.version}/">p2 repository</a></td></tr>`).join('');
  write('index.html', document('Documentation and Releases', `<h1>SWTBot Test Support</h1><p>SWT screenshots and optional JUnit failure diagnostics.</p>${latest ? '<p><a href="docs/latest/index.html">Latest stable documentation</a> · <a href="p2/latest/">Latest stable p2 repository</a></p>' : '<p>No stable release published yet.</p>'}<h2>Releases</h2><table><thead><tr><th>Version</th><th>Documentation</th><th>Installation</th></tr></thead><tbody>${rows}</tbody></table>`, '<a href="https://github.com/klibio/swtbot-test-support">Source and issues</a>'));
  write('style.css', fs.readFileSync(path.join(root, 'docs/style.css')));
  write('.nojekyll', '');
  write('releases.json', JSON.stringify({ latest: latest ?? null, versions: releases.map(({ manifest }) => manifest.version) }, null, 2));
}

export function download(repository, directory) {
  check(/^[\w.-]+\/[\w.-]+$/.test(repository), 'Invalid repository');
  fs.mkdirSync(directory, { recursive: true });
  const response = JSON.parse(execFileSync('gh', ['api', '--paginate', '--slurp', `repos/${repository}/releases?per_page=100`], { encoding: 'utf8' })).flat();
  let count = 0;
  for (const release of response.filter(item => !item.draft)) {
    const asset = release.assets.find(item => /^swtbot-test-support-site-.+\.zip$/.test(item.name));
    if (!asset) continue;
    const version = releaseVersion(release.tag_name.slice(1));
    check(asset.name === `swtbot-test-support-site-${version}.zip`, 'Release asset/tag mismatch');
    for (const name of [`swtbot-test-support-p2-${version}.zip`, `swtbot-test-support-maven-${version}.zip`, 'SHA256SUMS.txt']) check(release.assets.some(item => item.name === name), `Incomplete release ${version}: ${name}`);
    const target = path.join(directory, asset.name);
    const data = execFileSync('gh', ['api', '-H', 'Accept: application/octet-stream', `repos/${repository}/releases/assets/${asset.id}`], { maxBuffer: 500_000_000 });
    const { manifest } = payload(data);
    check(manifest.tag === release.tag_name && manifest.prerelease === release.prerelease, 'Release metadata mismatch');
    check(asset.digest?.startsWith('sha256:') && digest(data) === asset.digest.slice(7), 'GitHub asset digest mismatch');
    fs.writeFileSync(target, data);
    count++;
  }
  check(count > 0, 'No published site releases to deploy');
}

export function reuse(archive, output, version, commit) {
  const data = fs.readFileSync(archive);
  const { manifest, files } = payload(data);
  check(manifest.version === version && manifest.commit === commit, 'Published version belongs to another tag commit');
  fs.mkdirSync(output, { recursive: true });
  fs.writeFileSync(path.join(output, `swtbot-test-support-site-${version}.zip`), data);
  writeZip(new Map([...files].filter(([name]) => name.startsWith('p2/')).map(([name, contents]) => [name.slice(3), contents])), path.join(output, `swtbot-test-support-p2-${version}.zip`));
}

export function verifyMaven(archive, repository) {
  const { manifest, files } = payload(fs.readFileSync(archive));
  for (const library of libraries) {
    const jar = fs.readFileSync(path.join(repository, 'io/klib/tools', library, manifest.version, `${library}-${manifest.version}.jar`));
    const plugin = [...files].find(([name]) => name.startsWith(`p2/plugins/${library}_`) || name.startsWith(`p2/plugins/${library}-`));
    check(plugin && digest(plugin[1]) === digest(jar), `Published Maven/p2 bundle mismatch: ${library}`);
  }
}

const invoked = process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);
if (invoked) {
  const [command, ...args] = process.argv.slice(2);
  if (command === 'prepare') prepare(...args);
  else if (command === 'assemble') {
    const [input, output, root] = args;
    assemble(fs.readdirSync(input).filter(name => /^swtbot-test-support-site-.+\.zip$/.test(name)).map(name => path.join(input, name)), output, root);
  } else if (command === 'download') download(...args);
  else if (command === 'reuse') reuse(...args);
  else if (command === 'verify-maven') verifyMaven(...args);
  else if (command === 'verify') payload(fs.readFileSync(args[0]));
  else throw new Error('Usage: site.mjs prepare|assemble|download|verify ...');
}