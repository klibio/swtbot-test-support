import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import { pathToFileURL } from 'node:url';
import { chromium } from 'playwright';

const directory = path.resolve(process.argv[2]);
assert.ok(fs.existsSync(path.join(directory, 'index.html')), 'Assemble a Pages preview first');
const history = JSON.parse(fs.readFileSync(path.join(directory, 'releases.json')));
const version = history.latest ?? history.versions[0];
const browser = await chromium.launch({ headless: true, channel: process.env.PLAYWRIGHT_CHANNEL });
try {
  for (const viewport of [{ width: 1280, height: 900 }, { width: 390, height: 844 }]) {
    const page = await browser.newPage({ viewport });
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    for (const name of ['index.html', ...['index', 'installation', 'screenshots', 'junit', 'troubleshooting'].map(item => `docs/${version}/${item}.html`), 'p2/latest/index.html', `docs/${version}/api/io.klib.swtbot.support/index.html`, `docs/${version}/api/io.klib.swtbot.support/io/klib/swtbot/support/CaptureOptions.html`, `docs/${version}/api/io.klib.swtbot.junit4/io/klib/swtbot/junit4/ScreenshotRule.html`, `docs/${version}/api/io.klib.swtbot.jupiter/io/klib/swtbot/jupiter/ScreenshotExtension.html`]) {
      if (!fs.existsSync(path.join(directory, name))) continue;
      await page.goto(pathToFileURL(path.join(directory, name)).href);
      assert.ok((await page.locator('h1').innerText()).trim(), `Missing page heading: ${name}`);
      assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1), `Horizontal overflow: ${viewport.width} ${name}`);
      const links = await page.locator('a[href]').evaluateAll(elements => elements.map(element => element.href));
      for (const link of links) {
        const url = new URL(link);
        if (url.protocol !== 'file:') continue;
        url.hash = '';
        url.search = '';
        assert.ok(fs.existsSync(decodeURIComponent(url.pathname).replace(/^\/([A-Za-z]:)/, '$1')), `Broken local link: ${name}: ${link}`);
      }
      if (name === `docs/${version}/index.html`) await page.screenshot({ path: path.join(directory, `../docs-${viewport.width}.png`), fullPage: true });
    }
    assert.deepEqual(errors, [], 'Browser JavaScript errors');
    await page.close();
  }
  console.log('Desktop/mobile guides, API references and local links passed.');
} finally {
  await browser.close();
}