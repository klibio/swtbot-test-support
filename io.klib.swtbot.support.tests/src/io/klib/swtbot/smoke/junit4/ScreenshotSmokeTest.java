package io.klib.swtbot.smoke.junit4;

import java.nio.file.Files;

import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.JUnitCore;

import io.klib.swtbot.fixture.SmokeChecks;
import io.klib.swtbot.fixture.UiFixture;
import io.klib.swtbot.junit4.ScreenshotRule;
import io.klib.swtbot.support.CaptureMode;
import io.klib.swtbot.support.CaptureResult;
import io.klib.swtbot.support.Screenshots;

public class ScreenshotSmokeTest {
    private static Screenshots nestedScreenshots;
    private static Shell nestedShell;
    private static AssertionError nestedFailure;
    private Shell shell;
    private Screenshots screenshots;

    @Rule
    public ScreenshotRule failure = new ScreenshotRule(() -> screenshots, () -> shell);

    @Before
    public void open() throws Exception {
        shell = UiFixture.openShell();
        screenshots = SmokeChecks.screenshots("junit4");
    }

    @After
    public void close() throws Exception {
        UiFixture.ui(() -> { if (!shell.isDisposed()) shell.dispose(); return null; });
    }

    @Test
    public void capturesNativePixelsOnBothThreads() throws Throwable {
        SmokeChecks.captureAndCheck(screenshots, shell);
        SmokeChecks.checkFailureGuard(screenshots, shell);
        SmokeChecks.checkTimeout(shell);
    }

    @Test
    public void reportsDisposedShell() throws Exception {
        close();
        CaptureResult result = screenshots.capture("disposed", CaptureMode.SHELL_CLIENT_AREA, () -> shell);
        SmokeChecks.require(result.status() == CaptureResult.Status.SKIPPED, "Disposed shell must be skipped");
    }

    @Test
    public void ruleAutomaticallyCapturesOriginalFailure() throws Exception {
        nestedScreenshots = screenshots;
        nestedShell = shell;
        nestedFailure = new AssertionError("automatic JUnit4 diagnostic fixture");
        var result = JUnitCore.runClasses(FailureFixture.class);
        SmokeChecks.require(result.getFailureCount() == 1, "Nested fixture must fail once");
        SmokeChecks.require(result.getFailures().get(0).getException() == nestedFailure, "Original failure replaced");
        try (var files = Files.list(screenshots.directory())) {
            SmokeChecks.require(files.count() == 3, "Rule must capture three diagnostic images");
        }
    }

    public static class FailureFixture {
        @Rule
        public ScreenshotRule diagnostics = new ScreenshotRule(() -> nestedScreenshots, () -> nestedShell);

        @Test
        public void controlledFailure() {
            throw nestedFailure;
        }
    }
}