# Changelog

## Unreleased

- Bounded latest stable Java CI selection to `>=21 <26`, matching Gradle 9.2.0 runtime support and preventing Java 26+ build-script failures.
- Added complete public API/package Javadoc for all three libraries, with strict doclint/warnings-as-errors checks and cross-linked versioned API references.
- Added tagged p2 export with separately selectable core, JUnit 4 and Jupiter features; third-party dependencies remain supplied by consumer target platforms.
- Added release user guides, downloadable p2/site assets, CI previews and GitHub Pages deployment retaining future versions with stable-only `latest` aliases.
- Added release archive/checksum validation, immutable site payload reuse and complete-history rebuilds for safe deployment reruns.
- Expanded CI to Java 21, 25 and latest stable Temurin across Linux, Windows and macOS; Ubuntu/Java 21 is the minimum canonical build and publication environment, with other entries validating compatibility only. Java 17 runtime support is excluded because pinned Eclipse/SWT dependencies require Java 21.
- Added Gaia-style tag-driven release versioning (`vX.Y.Z[-pre]`) and validated tag reachability from `main`, while retaining snapshot publication on `main`.
- Added Maven GAV metadata (`io.klib.tools`, EPL-2.0, SCM, developers), sources/javadoc/GPG release configuration and `Release`/`Sonatype Snapshots` repositories.
- Added reusable GitHub Actions CI (ubuntu/windows/macos, Java 21), Sonatype Central Maven publishing and GitHub Releases with repository ZIP/checksums.
- Restored `cnf/bnd.bnd` so `-nobundles` applies only to `cnf`.

## 0.1.0 - 2026-10-15

- Added SWT-only client-area, window-rectangle and opt-in desktop screenshots.
- Added safe unique artifact paths, explicit outcomes, bounded UI dispatch and original-error-preserving failure guard.
- Added separate JUnit 4 rule and Jupiter 5/6 test/lifecycle extension.
- Added Windows SWT/OSGi matrix, unit tests, local indexed bnd publication and RCP example integration.