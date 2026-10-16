# Changelog

## Unreleased

- Added Gaia-style tag-driven release versioning (`vX.Y.Z[-pre]`) and validated tag reachability from `main`, while retaining snapshot publication on `main`.
- Added Maven GAV metadata (`io.klib.tools`, EPL-2.0, SCM, developers), sources/javadoc/GPG release configuration and `Release`/`Sonatype Snapshots` repositories.
- Added reusable GitHub Actions CI (ubuntu/windows/macos, Java 21), Sonatype Central Maven publishing and GitHub Releases with repository ZIP/checksums.
- Restored `cnf/bnd.bnd` so `-nobundles` applies only to `cnf`.

## 0.1.0 - 2026-10-15

- Added SWT-only client-area, window-rectangle and opt-in desktop screenshots.
- Added safe unique artifact paths, explicit outcomes, bounded UI dispatch and original-error-preserving failure guard.
- Added separate JUnit 4 rule and Jupiter 5/6 test/lifecycle extension.
- Added Windows SWT/OSGi matrix, unit tests, local indexed bnd publication and RCP example integration.