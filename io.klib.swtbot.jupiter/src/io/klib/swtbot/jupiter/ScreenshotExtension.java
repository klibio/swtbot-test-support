package io.klib.swtbot.jupiter;

import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.opentest4j.TestAbortedException;

import io.klib.swtbot.support.Screenshots;

public final class ScreenshotExtension implements TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler {
    private final Supplier<Screenshots> screenshots;
    private final Supplier<Shell> shell;

    public ScreenshotExtension(Supplier<Screenshots> screenshots, Supplier<Shell> shell) {
        this.screenshots = Objects.requireNonNull(screenshots, "screenshots");
        this.shell = Objects.requireNonNull(shell, "shell");
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable original) throws Throwable {
        capture(context, "test", original);
    }

    @Override
    public void handleBeforeAllMethodExecutionException(ExtensionContext context, Throwable original) throws Throwable {
        capture(context, "beforeAll", original);
    }

    @Override
    public void handleBeforeEachMethodExecutionException(ExtensionContext context, Throwable original) throws Throwable {
        capture(context, "beforeEach", original);
    }

    @Override
    public void handleAfterEachMethodExecutionException(ExtensionContext context, Throwable original) throws Throwable {
        capture(context, "afterEach", original);
    }

    @Override
    public void handleAfterAllMethodExecutionException(ExtensionContext context, Throwable original) throws Throwable {
        capture(context, "afterAll", original);
    }

    private void capture(ExtensionContext context, String phase, Throwable original) throws Throwable {
        if (!(original instanceof TestAbortedException)) {
            try {
                Screenshots capture = screenshots.get();
                if (capture != null) {
                    capture.failure("failure_" + phase + "_" + context.getUniqueId(), shell, original);
                }
            } catch (Throwable diagnosticFailure) {
                if (diagnosticFailure != original) {
                    original.addSuppressed(diagnosticFailure);
                }
            }
        }
        throw original;
    }
}