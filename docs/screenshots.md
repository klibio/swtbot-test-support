# Screenshots

Create one instance per test/session using your application's existing Display:

```java
Screenshots screenshots = new Screenshots(
    CaptureOptions.defaults(outputDirectory), testIdentity, () -> applicationDisplay);
screenshots.requireCheckpoint("01_precondition", () -> applicationShell);
screenshots.guard("failure_action", () -> applicationShell, () -> {
    performUserAction();
    assertTargetState();
});
screenshots.requireCheckpoint("02_target_state", () -> applicationShell);
```

Import `Screenshots` and `CaptureOptions` from `io.klib.swtbot.support`. Application variables/actions above belong to your test. The library does not create a Display, find windows or assert application state. Shell suppliers run on the supplied Display's UI thread. Actions passed to `guard` run on the calling thread.

## Capture Options

Defaults capture client area only, with a five-second completion budget per mode. Explicitly opt into other regions:

```java
new CaptureOptions(outputDirectory, Duration.ofSeconds(10),
    List.of(CaptureMode.SHELL_CLIENT_AREA, CaptureMode.SHELL_DISPLAY_BOUNDS));
```

| Mode | Artifact label | Region |
| --- | --- | --- |
| `SHELL_CLIENT_AREA` | `shell` | Shell client area via SWT graphics |
| `SHELL_DISPLAY_BOUNDS` | `swt` | Shell rectangle including window decorations |
| `DESKTOP` | `screen` | Display bounds, potentially multiple monitors |

Screen-based capture can include occlusion, other applications and private information. Keep the target visible. Scaling and window manager affect pixel dimensions. Do not publish test screenshots without reviewing their contents.

## Outcomes and Artifacts

`capture` returns one `CaptureResult`; `checkpoint` returns outcomes for configured modes. Status is `CAPTURED`, `SKIPPED`, or `FAILED`. `requireCaptured` and `requireCheckpoint` reject unavailable evidence explicitly.

Each instance creates a separate `run-<UUID>/test-<sanitized-id>/` directory. Unique PNG paths include checkpoint and mode; names are bounded. `directory()` gives the actual artifact directory. `ArtifactPaths.allocate` creates an empty reserved file, not screenshot evidence.

`failure` logs outcomes and suppresses diagnostic errors onto the original throwable. `guard` captures before caller teardown and rethrows that same throwable. Each instance diagnoses a throwable once and retains it for duplicate suppression.

## Threading and Timeouts

UI-thread capture runs immediately. Other callers queue work and wait for the configured budget. Timeout/interruption cancels queued work; executing native operations cannot be forcibly stopped and may finish later. Interrupted callers retain interruption status. Native graphics and images are disposed after capture. Capture does not modify global SWTBot preferences.