package io.klib.swtbot.fixture;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Shell;

import io.klib.swtbot.support.CaptureMode;
import io.klib.swtbot.support.CaptureOptions;
import io.klib.swtbot.support.CaptureResult;
import io.klib.swtbot.support.Screenshots;

public final class SmokeChecks {
    private SmokeChecks() {}

    public static Screenshots screenshots(String testId) throws Exception {
        return new Screenshots(new CaptureOptions(Path.of(System.getProperty("swtbot.screenshots.dir")),
            Duration.ofSeconds(5), List.of(CaptureMode.values())), testId, () -> {
                try {
                    return UiFixture.display();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(interrupted);
                }
            });
    }

    public static void captureAndCheck(Screenshots screenshots, Shell shell) throws Exception {
        for (CaptureResult result : screenshots.checkpoint("01_rendered", () -> shell)) {
            result.requireCaptured();
            ImageData image = new ImageData(result.outputPath().toString());
            require(image.width == result.width() && image.height == result.height(), "PNG dimensions differ");
            require(Files.size(result.outputPath()) > 100, "PNG empty");
            if (result.mode() == CaptureMode.SHELL_CLIENT_AREA) {
                require(image.palette.getRGB(image.getPixel(image.width / 2, image.height / 2)).green > 150,
                    "Rendered client area does not contain fixture pixels");
            }
        }
        UiFixture.ui(() -> {
            screenshots.capture("02_ui_thread", CaptureMode.SHELL_CLIENT_AREA, () -> shell).requireCaptured();
            return null;
        });
    }

    public static void checkFailureGuard(Screenshots screenshots, Shell shell) throws Throwable {
        AssertionError original = new AssertionError("controlled fixture failure");
        try {
            screenshots.guard("03_failure", () -> shell, () -> { throw original; });
            throw new AssertionError("Guard swallowed assertion");
        } catch (AssertionError actual) {
            require(actual == original, "Original assertion replaced");
        }
        try (var files = Files.list(screenshots.directory())) {
            require(files.filter(file -> file.getFileName().toString().startsWith("03_failure")).count() == 3,
                "Failure guard did not capture all modes before cleanup");
        }
    }

    public static void checkTimeout(Shell shell) throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        UiFixture.display().asyncExec(() -> {
            entered.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        });
        require(entered.await(5, TimeUnit.SECONDS), "UI blocking fixture did not start");
        try {
            Screenshots screenshots = new Screenshots(new CaptureOptions(
                Path.of(System.getProperty("swtbot.screenshots.dir")), Duration.ofMillis(30),
                List.of(CaptureMode.SHELL_CLIENT_AREA)), "timeout", () -> {
                    try {
                        return UiFixture.display();
                    } catch (InterruptedException interrupted) {
                        throw new IllegalStateException(interrupted);
                    }
                });
            CaptureResult result = screenshots.capture("timeout", CaptureMode.SHELL_CLIENT_AREA, () -> shell);
            require(result.status() == CaptureResult.Status.FAILED && result.reason().contains("timed out"),
                "Blocked UI did not produce bounded timeout");
        } finally {
            release.countDown();
        }
    }

    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}