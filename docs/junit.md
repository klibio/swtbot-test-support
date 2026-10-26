# JUnit Adapters

## JUnit 4

```java
@Rule
public ScreenshotRule diagnostics =
    new ScreenshotRule(() -> screenshots, () -> applicationShell);
```

Import `io.klib.swtbot.junit4.ScreenshotRule`. Keep your existing runner, including `SWTBotJunit4ClassRunner`. Successful/skipped tests do not trigger capture. Rule callbacks run after `@After`; retain UI until the callback or use `Screenshots.guard` for early evidence. Class setup failures require explicit handling.

## Jupiter 5 and 6

```java
@RegisterExtension
public static ScreenshotExtension diagnostics =
    new ScreenshotExtension(() -> screenshots, () -> applicationShell);
```

Import `io.klib.swtbot.jupiter.ScreenshotExtension`. Static extension and suppliers cover class lifecycle failures; instance registration suits per-method setup. There is no no-argument `@ExtendWith` construction.

The extension handles test and before/after-each/all method exceptions, rethrowing the original throwable with diagnostic problems suppressed. Aborted/disabled tests are not captured. Other extensions that swallow errors or dispose UI first can prevent capture.

Supply a compatible engine and OSGi tester separately. Keep Jupiter 5/Platform 1 and Jupiter 6/Platform 6 runtime configurations coherent and separate. Verified runtimes are JUnit 4.13.2, Jupiter 5.13.4/Platform 1.13.4, and Jupiter/Platform 6.0.1 on Windows. The adapter's `[5.13,7)` range is not a claim that every future version has been tested.