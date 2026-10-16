package io.klib.swtbot.junit4;

import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.swt.widgets.Shell;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import io.klib.swtbot.support.Screenshots;

public final class ScreenshotRule extends TestWatcher {
    private final Supplier<Screenshots> screenshots;
    private final Supplier<Shell> shell;

    public ScreenshotRule(Supplier<Screenshots> screenshots, Supplier<Shell> shell) {
        this.screenshots = Objects.requireNonNull(screenshots, "screenshots");
        this.shell = Objects.requireNonNull(shell, "shell");
    }

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