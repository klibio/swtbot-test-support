# SWTBot Test Support

- Standalone bnd workspace. Java 17 API/classfile baseline; use Java 21 for the pinned Eclipse 4.41 build and UI fixtures.
- SWT-only core `io.klib.swtbot.support`; independent optional `io.klib.swtbot.junit4` and `io.klib.swtbot.jupiter` adapters. Never move JUnit/E4 dependencies into the core.
- Public packages/bundles start at 0.1.0. Preserve public contracts and update versions/CHANGELOG for subsequent changes.
- Gradle wrapper is pinned. Focused checks: `./gradlew :io.klib.swtbot.support:test :io.klib.swtbot.junit4:test :io.klib.swtbot.jupiter:test`.
- Actual Windows SWT/OSGi gate: `bash scripts/verify-win32.sh`; requires a rendered desktop and compatible tester described in README. Count executed tests and inspect failures/errors.
- JUnit 5 and 6 runtime configurations must be coherent and separate; avoid mixed P2/Maven versions.
- Use explicit existing Display/shell suppliers; no `Display.getDefault()` readiness probe or hidden Display creation. Bound queued UI waits and dispose native resources.
- Client-area screenshots are default; desktop is opt-in and may contain private/occluded content.
- Never replace original errors with diagnostics. JUnit 4 rule callbacks follow cleanup; early evidence needs the guard.
- Publish locally using `bash scripts/publish-local.sh`; no remote publication, commit or push without authorization. Never delete the user's shared local repository as cleanup.
- Versioning: `base.version` is the local and `main` snapshot fallback; release versions come from validated `vX.Y.Z[-pre]` tags through `.github/workflows/release.yml`. Remote publication runs only through GitHub Actions; never run `ci-publish.sh` with publication secrets locally. `-nobundles` belongs in `cnf/bnd.bnd`, not `cnf/build.bnd`.
- Preserve unrelated worktree changes. Read README before changing tooling or claiming support for unexecuted native platforms.