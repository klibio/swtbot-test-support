# Troubleshooting

## Capture Skipped

Check that the supplied Display is alive and belongs to your application. Supply a visible, non-minimized, non-disposed shell on that Display. Never use `Display.getDefault()` as a readiness probe: it can create a hidden Display.

## UI Capture Timed Out

Do not block the UI thread while a background test waits for capture. Check UI responsiveness and teardown ordering. Increasing the budget does not fix a deadlock. Already executing native work can finish after timeout; retain UI long enough for safe completion.

## Failure Has No Screenshot

JUnit 4 rule callbacks follow cleanup. Use `guard` around the failing action or delay UI disposal. With Jupiter, register class-scoped diagnostics when class lifecycle evidence is needed. Check original failure's suppressed exceptions and stderr for capture problems.

## Missing Dependencies in p2

This repository contains only screenshot support and optional adapters. Add Eclipse/SWT and matching JUnit target locations. Do not resolve Jupiter 5 and 6 together or mix unrelated P2/Maven versions. Use the exact release URL when reproducibility matters.

## Empty or Unexpected Images

Check that target UI is rendered and visible. Screen capture includes occlusion and platform decorations; use client-area mode where appropriate. Native Linux/macOS execution has not been verified; resolution/build success is not native capture verification.

## Reporting an Issue

Include library version, Java/Eclipse/SWT/JUnit versions, OS, capture mode, status/reason, timeout and lifecycle timing. Review logs and screenshots for private information before attaching them to a [GitHub issue](https://github.com/klibio/swtbot-test-support/issues).