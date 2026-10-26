# SWTBot Test Support

[![CI Build](https://github.com/klibio/swtbot-test-support/actions/workflows/ci.yml/badge.svg)](https://github.com/klibio/swtbot-test-support/actions/workflows/ci.yml)
[![Release](https://github.com/klibio/swtbot-test-support/actions/workflows/release.yml/badge.svg)](https://github.com/klibio/swtbot-test-support/actions/workflows/release.yml)

Reusable OSGi screenshot tools for SWT/SWTBot tests. Core uses native SWT only. SWTBot finder itself requires JUnit 4, Hamcrest and JFace; this library avoids that transitive coupling.

## User Documentation and p2

Release documentation: **https://klibio.github.io/swtbot-test-support/** (available after the first release using the Pages workflow).

- User guide and API reference: `https://klibio.github.io/swtbot-test-support/docs/latest/`
- Stable update site: `https://klibio.github.io/swtbot-test-support/p2/latest/`
- Reproducible URLs: replace `latest` with the Maven release version, such as `0.2.0` or `0.2.0-rc.1`.

Every future release includes versioned guides and Javadoc for all three libraries, plus a standalone p2 repository. Existing releases are not backfilled. `latest` selects the highest stable SemVer, never a prerelease or merely the most recent release date. Before the first stable release, only versioned URLs exist.

Add the p2 URL to your Eclipse target definition and select core plus only the desired adapter. This site does not mirror SWT, Eclipse or JUnit: supply compatible target locations separately. See [installation](docs/installation.md), [screenshots](docs/screenshots.md), [JUnit adapters](docs/junit.md), and [troubleshooting](docs/troubleshooting.md). Source guide placeholders are replaced with the exact version when published.

## Bundles

| BSN | Public API | Dependencies |
| --- | --- | --- |
| `io.klib.swtbot.support` | `Screenshots`, `CaptureOptions`, `CaptureMode`, `CaptureResult`, `ArtifactPaths` | SWT, Java 17+ |
| `io.klib.swtbot.junit4` | `ScreenshotRule` | Core, JUnit 4 |
| `io.klib.swtbot.jupiter` | `ScreenshotExtension` | Core, public Jupiter APIs, OpenTest4J |

All initial bundles/exported packages are `0.1.0`. bnd generates manifests/POMs. Core needs no service registration, product, E4 or test runner.

## Versioning

Follows Gaia's tag-driven scheme. Local builds use `base.version` (e.g. `0.1.0`) with a `SNAPSHOT` qualifier. Release CI derives both OSGi and Maven versions from a validated `vX.Y.Z[-pre]` Git tag and injects them as `RELEASE_BUNDLE_VERSION` and `RELEASE_MAVEN_VERSION`:

| Git tag | OSGi `Bundle-Version` | Maven version | GitHub release |
| --- | --- | --- | --- |
| no tag (local/CI branch build) | `0.1.0.SNAPSHOT` | `0.1.0-SNAPSHOT` | none |
| `v0.1.0` | `0.1.0` | `0.1.0` | stable |
| `v0.2.0-rc.1` | `0.2.0.rc-1` | `0.2.0-rc.1` | prerelease |

The release guard rejects malformed tags and tags whose commit is not reachable from `main`. Exported package versions stay explicit in each `bnd.bnd`. Maven GAV is `io.klib.tools:<bsn>:<version>`; POM metadata and GPG signing live in `cnf/ext/maven-gav.bnd`, release repositories in `cnf/ext/repositories.bnd`.

## Continuous Integration

`.github/workflows/ci.yml` runs `./.github/scripts/ci-build.sh` (`./gradlew build`) across `ubuntu-latest`, `windows-latest` and `macos-latest` with Java 21, 25 and the latest stable Temurin release supported by the pinned Gradle (resolved dynamically, excluding early-access builds). Gradle 9.2.0 supports running on Java through 25, so the latest selector is bounded to `>=21 <26`; update this bound together with the Gradle wrapper after checking its [Java compatibility matrix](https://docs.gradle.org/9.2.0/userguide/compatibility.html#java_runtime). Ubuntu with Java 21 is the canonical build: Java 21 is the minimum required by the pinned Eclipse/SWT dependencies. Library classfiles still target Java 17, but Java 17 runtime support is not provided or tested. The canonical job covers the Ubuntu/Java 21 matrix entry; all other entries build and test with their selected JDK to validate compatibility only and never publish. Test reports have unique OS/JDK artifact names.

The workflow runs for pushes to `main` and pull requests and is reusable by the tag release workflow, so all matrix entries validate the exact release versions before publication. Snapshot and tag publication both use the canonical Ubuntu/Java 21 environment, never the moving latest JDK. The native Windows SWT/OSGi matrix below is not part of hosted CI because it needs a locally built bnd 7.5 tester.

### Require CI before merging

CI runs on every pull request, including documentation-only changes, so required checks are not left pending by path filters. Pushes to `main` retain their path filters.

A repository administrator must configure merge protection in [Settings → Rules → Rulesets](https://github.com/klibio/swtbot-test-support/settings/rules); the workflow alone does not enforce it:

1. Create a **New branch ruleset** named `Require CI`, set enforcement to **Active**, and target `main`.
2. Enable **Require a pull request before merging** and **Require status checks to pass**.
3. Add all three checks below, selecting **GitHub Actions** as their source. If they are not available, let CI run on a pull request first, then select the reported checks:
   - `Canonical build JDK21 ubuntu-latest`
   - `Compatibility JDK21 windows-latest`
   - `Compatibility JDK21 macos-latest`
   Remove any previously required `Build JDK21 ...` checks: those names were replaced by the canonical and compatibility jobs and will no longer be reported. To gate on the entire CI workflow, also require `Documentation and p2 validation` and every JDK25/latest compatibility check shown in the CI run.
4. Enable **Require branches to be up to date before merging**, leave the bypass list empty unless deliberately needed, and save.
5. Verify on a pull request targeting `main` (a documentation-only change also works): merging must be blocked while any required check is pending or failing. Once all three succeed, CI no longer blocks merging, provided the branch is up to date and other rules are satisfied.

## Release

Create a signed tag on `main` and push it to start `.github/workflows/release.yml`:

```bash
git tag -s v0.1.0 -m "SWTBot Test Support 0.1.0"
git push origin v0.1.0
```

The workflow validates the tag and reachability from `main`, runs the reusable cross-platform CI at the tag-derived versions, then releases the three library bundles through bnd (`jar`, `-sources`, `-javadoc`, POM, checksums, `.asc` signatures). `.github/scripts/sonatype-upload.sh` uploads the Maven repository to Sonatype Central as a `USER_MANAGED` deployment. It also creates a GitHub Release with the Maven repository ZIP and `SHA256SUMS.txt`; prerelease tags (such as `v0.2.0-rc.1`) create prereleases. Separately, `.github/workflows/publish.yml` publishes `*-SNAPSHOT` artifacts to Sonatype Central snapshots on pushes to `main`.

Before publication, CI validates Javadoc with `-Xdoclint:all -Werror`, builds three independently selectable p2 features, and packages the tagged user guide/API reference. GitHub Releases additionally carry `swtbot-test-support-site-<version>.zip` (version manifest and per-file SHA-256 hashes) and `swtbot-test-support-p2-<version>.zip`. The workflow verifies that p2 and Maven contain byte-identical library bundles. Published site payloads are reused unchanged on same-tag reruns; a different tag commit is rejected.

### GitHub Pages Setup

Once, enable **Settings > Pages > Build and deployment > Source: GitHub Actions**. Allow the `github-pages` environment to deploy from the default branch, and approve deployments if required by repository policy. No additional signing or publication secrets are needed for Pages; its deploy job uses scoped `pages:write` and `id-token:write` permissions.

After a successful tag release, [Pages workflow](.github/workflows/pages.yml) reconstructs all eligible releases from GitHub assets, verifies their metadata/checksums and deploys one complete site. It uses `workflow_run` because releases created using `GITHUB_TOKEN` do not trigger ordinary release-event workflows. Deployments are serialized; older reruns cannot roll `latest` back. Corrupt/incomplete new assets fail deployment without replacing the previous live site. Releases without site payloads are skipped, including historical releases.

To repair a failed Pages deployment, rerun it or run **Documentation and p2 Pages > Run workflow** after fixing the cause. This rebuilds history, not older tags. Keep release site assets: they are the durable source of historical documentation/repositories. Size checks stop before GitHub Pages' 1 GB site limit; history is never silently pruned. Maven Central approval remains separate from Pages deployment. Custom domains are not configured; installation examples use the canonical project URL above.

### Local Documentation Preview

Use Java 21 and Node 24. This generates local artifacts only, without signing or publication credentials:

```bash
export JAVA_HOME="$HOME/.ecdev/java/ee/JAVA21"
export PATH="$JAVA_HOME/bin:$PATH"
export RELEASE_BUNDLE_VERSION=0.2.0 RELEASE_MAVEN_VERSION=0.2.0
npm --prefix .github/scripts/site ci --ignore-scripts
npm --prefix .github/scripts/site test
bash ./gradlew verifyJavadoc :io.klib.swtbot.p2:exportP2
node .github/scripts/site/site.mjs prepare . build/site-assets "$RELEASE_MAVEN_VERSION" "$RELEASE_BUNDLE_VERSION" "$(git rev-parse HEAD)"
node .github/scripts/site/site.mjs assemble build/site-assets build/pages .
```

Open the generated `build/pages/index.html` in a browser. Assembly requires an empty output directory; use a fresh directory on repeated previews. No local server is needed. CI uploads the same structure as the `documentation-and-p2-preview` artifact. All exported packages, public types/constructors/methods, record components and enum constants must have meaningful Javadoc; documentation changes are validated by `check` as well as the dedicated task. Runtime signatures remain unchanged.

CI checks desktop/mobile layout and local links using Playwright Chromium. To repeat locally, run `node .github/scripts/site/node_modules/playwright/cli.js install chromium`, then `node .github/scripts/site/browser.mjs build/pages`. An installed Edge can be used instead with `PLAYWRIGHT_CHANNEL=msedge` if browser downloads are unavailable. Do not disable TLS verification to download browser binaries.

Required repository secrets for publication: `GPG_KEY_ID`, `GPG_PASSPHRASE`, `GPG_PRIVATE_KEY` (armored), `SONATYPE_BEARER` (Central Portal token). Portal prerequisites outside this repository: verified `io.klib` namespace, snapshot publishing enabled, and the GPG public key on a key server.

Update `CHANGELOG.md` before tagging. After the run, approve the deployment in the Central Portal. `base.version` is the local and `main` snapshot fallback; release versions come from tags.

Local dry run without credentials:

```bash
RELEASE_BUNDLE_VERSION=0.2.0.rc-1 RELEASE_MAVEN_VERSION=0.2.0-rc.1 RELEASE_TAG=v0.2.0-rc.1 \
    ./gradlew :io.klib.swtbot.support:release :io.klib.swtbot.junit4:release :io.klib.swtbot.jupiter:release
find dist/bundles -type f
```

## Build and Verify

Use Java 21 for pinned Eclipse 4.41 build/native fixtures; library classfiles target Java 17. Gradle 9.2.0 and bnd Gradle plugin 7.4.0 are pinned. Library baseline does not remove the chosen SWT version's own runtime requirements.

```bash
export JAVA_HOME="$HOME/.ecdev/java/ee/JAVA21"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :io.klib.swtbot.support:test :io.klib.swtbot.junit4:test :io.klib.swtbot.jupiter:test
```

Windows native matrix requires a rendered desktop, Java 21, a bnd CLI at `$HOME/biz.aQute.bnd.jar` (override with `BND_JAR`) and a compatible JUnit Platform tester. The verified tester is locally built `7.5.0.202610141925-SNAPSHOT`, not automatically downloaded or produced by this checkout. Explicitly install a compatible tester into the `SWTBot Tooling` repository:

```bash
java -jar "$HOME/biz.aQute.bnd.jar" repo -r 'SWTBot Tooling' put /path/to/biz.aQute.tester.junit-platform.jar
bash scripts/verify-win32.sh
```

Tooling repository: `${user.home}/.bnd/repositories/swtbot-test-support-tooling`. Script stops on build/resolve/test errors and verifies XML reports in fresh `reports/verify.*/` directories. Both manifest and launch `Test-Cases` declarations are explicit. SWT fixture owns the main thread; tests run on a background thread. Screenshots remain outside disposable launch storage.

Verified on Windows, 2026-10-15:

| Runtime | Outer OSGi tests | Result |
| --- | --- | --- |
| JUnit 4.13.2, legacy bnd tester | 3 | Passed |
| Jupiter 5.13.4 / Platform 1.13.4, compatible bnd 7.5 tester | 4 | Passed |
| Jupiter / Platform 6.0.1, compatible bnd 7.5 tester | 4 | Passed |

Eleven Gradle unit tests passed. Real SWT checks decode PNGs, verify dimensions/rendered pixels, exercise UI/test-thread capture, original-error preservation, disposal/timeouts and automatic test/lifecycle failure capture. Asserted failing nested fixtures do not fail outer tests. Parameterized invocations use separate artifact directories. Native Linux/macOS execution is not yet verified.

## Local Distribution

```bash
bash scripts/publish-local.sh
```

Publishes only three library bundles using bnd repository APIs and generated indexes to `${user.home}/.bnd/repositories/swtbot-test-support`. Independent of the Maven publication above; no P2/GitHub publication. Consumers resolve independently of producer checkout:

```bnd
-plugin.swtbot.support: aQute.bnd.deployer.repository.LocalIndexedRepo; \
    name='SWTBot Test Support'; \
    local='${user.home}/.bnd/repositories/swtbot-test-support'; \
    readonly=true
```

Add `io.klib.swtbot.support;version='[0.1.0,0.2)'` and only the desired adapter to `-buildpath`. Add matching `osgi.identity` runtime requirements, preserve actual product/framework/native settings, then resolve. Snapshot bundles carry a `<tstamp>-SNAPSHOT` qualifier and match these ranges.

## Screenshot API

```java
import io.klib.swtbot.support.CaptureOptions;
import io.klib.swtbot.support.Screenshots;

Screenshots screenshots = new Screenshots(
    CaptureOptions.defaults(outputDirectory), testIdentity, () -> applicationDisplay);
screenshots.requireCheckpoint("01_precondition", () -> applicationShell);
screenshots.guard("failure_action", () -> applicationShell, () -> {
    performUserAction();
    assertTargetState();
});
screenshots.requireCheckpoint("02_target_state", () -> applicationShell);
```

Use existing application Display and a `Supplier<Shell>` evaluated on its UI thread. Actions/variables above belong to the consuming test; `guard` accepts an action throwing `Throwable`. Library does not create a Display, discover windows, alter workbench state or assert application behavior.

Defaults select client area and a five-second queued-UI completion budget. All three modes require explicit options:

```java
new CaptureOptions(outputDirectory, Duration.ofSeconds(10), List.of(CaptureMode.values()))
```

| Mode | Suffix | Scope |
| --- | --- | --- |
| `SHELL_CLIENT_AREA` | `shell` | Client area via `GC(shell)` |
| `SHELL_DISPLAY_BOUNDS` | `swt` | Window rectangle/decorations from display pixels |
| `DESKTOP` | `screen` | Display bounds, potentially multiple monitors |

Screen-based captures can contain occlusion, other applications and private material. Hidden/minimized/disposed shells are skipped; scaling/window manager affect pixels. Keep target visible and inspect evidence.

`capture`/`checkpoint` return `CaptureResult`: `CAPTURED`, `SKIPPED` or `FAILED`, path, dimensions and reason/cause. `requireCaptured`/`requireCheckpoint` fail explicitly when evidence is unavailable. Paths are `run-<UUID>/test-<sanitized-id>/<checkpoint>_<mode>_<unique>.png`. Names are bounded and contained; each instance creates a separate run directory. `directory()` returns actual output. Core does not alter global SWTBot preferences.

Queued UI work is canceled on timeout/interruption and interruption status restored. Already executing native work cannot be forcibly stopped and may finish afterward; UI-thread callers execute immediately. `GC` and `Image` are disposed independently on error. Skipped captures create no PNG; failed writes remove the artifact. `ArtifactPaths.allocate` only reserves a filename.

`failure` logs diagnostic results and attaches capture problems as suppressed exceptions. `guard` diagnoses before teardown and rethrows the same original error. Each instance diagnoses a throwable only once, preventing guard/rule duplicates. Scope instances to tests/sessions: diagnosed failures remain referenced for that instance's lifetime.

## JUnit Adapters

JUnit 4:

```java
@Rule
public ScreenshotRule diagnostics = new ScreenshotRule(() -> screenshots, () -> applicationShell);
```

Keep existing `SWTBotJunit4ClassRunner`. Rule callbacks run after `@After`; use the guard for early evidence or retain UI until the callback. Class setup failures need explicit handling. Successful/skipped tests do not trigger capture.

Jupiter 5/6:

```java
@RegisterExtension
public static ScreenshotExtension diagnostics =
    new ScreenshotExtension(() -> screenshots, () -> applicationShell);
```

Static extension/static suppliers cover class lifecycle; instance fields suit per-method setup. No no-argument `@ExtendWith` construction. Extension handles test and before/after-each/all method exceptions, preserving errors; ordinary aborted/disabled tests are not captured. Another extension swallowing an exception, missing Display or already disposed UI prevents guaranteed evidence. `TestWatcher` alone would run too late.

Reuse compatible Jupiter engine/OSGi tester; library implements no test engine. Keep runtime lines coherent and constrain competing P2 versions. Adapter range `[5.13,7)` follows real 5.13.4/6.0.1 verification, not a claim every future version is tested.

## Reference Integration

Sibling `example.bnd.rcp` consumes local repository. Its test uses shared core/rule, explicit `swtbot.screenshots.dir`, early guard, existing-Display observation and restored SWTBot preferences. Three platform launchers resolve. Windows RCP scenario passed with five expected rows, nine checkpoint PNGs, no collected platform errors and completed CLI process.

Example's current P2/feature setup was verified using locally built bnd 7.5 CLI. Existing Gradle bnd 7.3 configuration was not silently upgraded. Linux/macOS checks are resolution-only, not native execution.