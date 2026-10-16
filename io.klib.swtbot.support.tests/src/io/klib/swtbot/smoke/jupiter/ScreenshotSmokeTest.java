package io.klib.swtbot.smoke.jupiter;

import java.nio.file.Files;

import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import io.klib.swtbot.fixture.SmokeChecks;
import io.klib.swtbot.fixture.UiFixture;
import io.klib.swtbot.jupiter.ScreenshotExtension;
import io.klib.swtbot.support.CaptureMode;
import io.klib.swtbot.support.CaptureResult;
import io.klib.swtbot.support.Screenshots;

public class ScreenshotSmokeTest {
    private static Screenshots nestedScreenshots;
    private static Shell nestedShell;
    private static AssertionError nestedFailure;
    private static String failurePhase;
    private Shell shell;
    private Screenshots screenshots;

    @RegisterExtension
    public ScreenshotExtension failure = new ScreenshotExtension(() -> screenshots, () -> shell);

    @BeforeEach
    public void open() throws Exception {
        shell = UiFixture.openShell();
        screenshots = SmokeChecks.screenshots("jupiter");
    }

    @AfterEach
    public void close() throws Exception {
        UiFixture.ui(() -> { if (!shell.isDisposed()) shell.dispose(); return null; });
    }

    @ParameterizedTest
    @ValueSource(strings = {"first", "second/../parameter"})
    public void capturesNativePixelsOnBothThreads(String invocation) throws Throwable {
        SmokeChecks.captureAndCheck(screenshots, shell);
        SmokeChecks.checkFailureGuard(screenshots, shell);
        screenshots.requireCheckpoint(invocation, () -> shell);
    }

    @Test
    public void reportsDisposedShell() throws Exception {
        close();
        CaptureResult result = screenshots.capture("disposed", CaptureMode.SHELL_CLIENT_AREA, () -> shell);
        SmokeChecks.require(result.status() == CaptureResult.Status.SKIPPED, "Disposed shell must be skipped");
    }

    @Test
    public void extensionCapturesTestAndLifecycleFailuresBeforeCleanup() throws Exception {
        var launcher = LauncherFactory.create(LauncherConfig.builder()
            .enableTestEngineAutoRegistration(false).addTestEngines(new JupiterTestEngine()).build());
        for (String phase : new String[] {"test", "beforeEach", "afterEach", "beforeAll"}) {
            failurePhase = phase;
            nestedShell = UiFixture.openShell();
            nestedScreenshots = SmokeChecks.screenshots("jupiter-" + phase);
            nestedFailure = new AssertionError("automatic Jupiter " + phase + " fixture");
            try {
                var listener = new SummaryGeneratingListener();
                launcher.execute(LauncherDiscoveryRequestBuilder.request()
                    .selectors(DiscoverySelectors.selectClass(FailureFixture.class)).build(), listener);
                var failures = listener.getSummary().getFailures();
                SmokeChecks.require(failures.size() == 1, "Nested " + phase + " fixture must fail once");
                SmokeChecks.require(failures.get(0).getException() == nestedFailure, "Original failure replaced");
                try (var files = Files.list(nestedScreenshots.directory())) {
                    SmokeChecks.require(files.count() == 3, "Extension must capture three " + phase + " images");
                }
            } finally {
                UiFixture.ui(() -> { if (!nestedShell.isDisposed()) nestedShell.dispose(); return null; });
            }
        }
    }

    public static class FailureFixture {
        @RegisterExtension
        public static ScreenshotExtension diagnostics = new ScreenshotExtension(() -> nestedScreenshots, () -> nestedShell);

        @BeforeAll
        public static void beforeAll() {
            if ("beforeAll".equals(failurePhase)) throw nestedFailure;
        }

        @BeforeEach
        public void beforeEach() {
            if ("beforeEach".equals(failurePhase)) throw nestedFailure;
        }

        @Test
        public void controlledFailure() {
            if ("test".equals(failurePhase)) throw nestedFailure;
        }

        @AfterEach
        public void afterEach() throws Exception {
            if ("afterEach".equals(failurePhase)) throw nestedFailure;
            try (var files = Files.list(nestedScreenshots.directory())) {
                SmokeChecks.require(files.count() == 3, "Diagnostic must run before fixture cleanup");
            }
            UiFixture.ui(() -> { nestedShell.dispose(); return null; });
        }
    }
}