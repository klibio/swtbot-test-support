package io.klib.swtbot.junit4;

import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.swt.widgets.Shell;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import io.klib.swtbot.support.Screenshots;

/**
 * JUnit 4 failure screenshots without replacing the original test failure.
 * TestWatcher runs after ordinary teardown; retain UI or use
 * {@link Screenshots#guard(String, Supplier, Screenshots.Action)} for early evidence.
 */
public final class ScreenshotRule extends TestWatcher {
    private final Supplier<Screenshots> screenshots;
    private final Supplier<Shell> shell;

    /**
     * Creates a rule using application-owned screenshot support and shell.
     * @param screenshots non-null supplier evaluated during failure handling; null result skips capture
     * @param shell non-null shell supplier evaluated on the application UI thread
     * @throws NullPointerException if either supplier is null
     */
    public ScreenshotRule(Supplier<Screenshots> screenshots, Supplier<Shell> shell) {
        this.screenshots = Objects.requireNonNull(screenshots, "screenshots");
        this.shell = Objects.requireNonNull(shell, "shell");
    }

    /**
     * Captures a failed test after cleanup, attaching diagnostic errors as suppressed.
     * @param original test failure, never replaced by capture errors
     * @param description failed test identity used in the artifact label
     */
    @Override
    protected void failed(Throwable original, Description description) {
        try {
            Screenshots capture = screenshots.get();
            if (capture != null) {
                capture.failure("failure_" + description.getDisplayName(), shell, original);
            }
        } catch (Throwable diagnosticFailure) {
            if (diagnosticFailure != original) {
                original.addSuppressed(diagnosticFailure);
            }
        }
    }
}