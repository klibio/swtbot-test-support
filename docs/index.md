# SWTBot Test Support

Release **@VERSION@** provides SWT-only screenshots and optional JUnit failure diagnostics.

Start with [installation](installation.html), then choose [screenshots](screenshots.html) and the appropriate [JUnit adapter](junit.html).

## API Reference

- [Core screenshot API](api/io.klib.swtbot.support/index.html)
- [JUnit 4 API](api/io.klib.swtbot.junit4/index.html)
- [Jupiter 5/6 API](api/io.klib.swtbot.jupiter/index.html)

Core needs SWT and Java 17 or newer. The chosen Eclipse/SWT runtime may require a newer Java version. No service registration, workbench, E4 application or SWTBot finder is required by core.

JUnit adapters remain independent: install only the adapter matching your tests. Runtime dependencies are supplied by your application or target platform, not bundled into this update site.