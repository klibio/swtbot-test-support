package io.klib.swtbot.jupiter;

import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.opentest4j.TestAbortedException;

import io.klib.swtbot.support.Screenshots;

/**
 * Jupiter 5/6 exception handler capturing test and lifecycle failures before rethrowing
 * the same original throwable. Aborted tests are not captured. Register an instance
 * with RegisterExtension; static registration covers class lifecycle failures.
 * Requires a live application Display and shell; does not create UI or a test engine.
 */
public final class ScreenshotExtension implements TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler {
    private final Supplier<Screenshots> screenshots;
    private final Supplier<Shell> shell;

    /**
     * Creates an extension using application-owned capture support.
     * @param screenshots non-null supplier evaluated during failure handling; null result skips capture
     * @param shell non-null shell supplier evaluated on the application UI thread
     * @throws NullPointerException if either supplier is null
     */
    public ScreenshotExtension(Supplier<Screenshots> screenshots, Supplier<Shell> shell) {
        this.screenshots = Objects.requireNonNull(screenshots, "screenshots");
        this.shell = Objects.requireNonNull(shell, "shell");
    }

    /**
     * Captures a test-method failure, except an aborted test.
     * @param context current invocation identity
     * @param original original failure
     * @throws Throwable the same original failure, with diagnostic problems suppressed
     */
    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable original) throws Throwable {
        capture(context, "test", original);
    }

    /**
     * Captures a before-all failure, except an aborted test.
     * @param context current container identity
     * @param original original failure
     * @throws Throwable the same original failure, with diagnostic problems suppressed
     */
    @Override
    public void handleBeforeAllMethodExecutionException(ExtensionContext context, Throwable original) throws Throwable {
        capture(context, "beforeAll", original);
    }

    /**
     * Captures a before-each failure, except an aborted test.
     * @param context current invocation identity
     * @param original original failure
     * @throws Throwable the same original failure, with diagnostic problems suppressed
     */
    @Override
    public void handleBeforeEachMethodExecutionException(ExtensionContext context, Throwable original) throws Throwable {
        capture(context, "beforeEach", original);
    }

    /**
     * Captures an after-each failure if application UI remains available.
     * @param context current invocation identity
     * @param original original failure
     * @throws Throwable the same original failure; aborted tests are not captured
     */
    @Override
    public void handleAfterEachMethodExecutionException(ExtensionContext context, Throwable original) throws Throwable {
        capture(context, "afterEach", original);
    }

    /**
     * Captures an after-all failure if application UI remains available.
     * @param context current container identity
     * @param original original failure
     * @throws Throwable the same original failure; aborted tests are not captured
     */
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