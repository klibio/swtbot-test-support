package io.klib.swtbot.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public final class Screenshots {
    @FunctionalInterface
    public interface Action {
        void run() throws Throwable;
    }

    private final CaptureOptions options;
    private final ArtifactPaths paths;
    private final Supplier<Display> displaySupplier;
    private final Set<Throwable> diagnosed = Collections.newSetFromMap(new IdentityHashMap<>());

    public Screenshots(CaptureOptions options, String testId, Supplier<Display> displaySupplier) throws IOException {
        this.options = Objects.requireNonNull(options, "options");
        this.paths = new ArtifactPaths(options.outputDirectory(), testId);
        this.displaySupplier = Objects.requireNonNull(displaySupplier, "displaySupplier");
    }

    public Path directory() {
        return paths.directory();
    }

    public List<CaptureResult> checkpoint(String checkpoint, Supplier<Shell> shell) {
        List<CaptureResult> results = new ArrayList<>();
        for (CaptureMode mode : options.modes()) {
            results.add(capture(checkpoint, mode, shell));
        }
        return List.copyOf(results);
    }

    public void requireCheckpoint(String checkpoint, Supplier<Shell> shell) {
        checkpoint(checkpoint, shell).forEach(CaptureResult::requireCaptured);
    }

    public void guard(String checkpoint, Supplier<Shell> shell, Action action) throws Throwable {
        try {
            action.run();
        } catch (Throwable failure) {
            failure(checkpoint, shell, failure);
            throw failure;
        }
    }

    public void failure(String checkpoint, Supplier<Shell> shell, Throwable original) {
        synchronized (diagnosed) {
            if (!diagnosed.add(original)) {
                return;
            }
        }
        try {
            for (CaptureResult result : checkpoint(checkpoint, shell)) {
                System.err.println("[SWTBot screenshot] " + result);
                if (result.status() != CaptureResult.Status.CAPTURED) {
                    original.addSuppressed(new IOException("Screenshot " + result.status() + ": " + result.reason(),
                        result.cause()));
                }
            }
        } catch (Throwable diagnosticFailure) {
            if (diagnosticFailure != original) {
                original.addSuppressed(diagnosticFailure);
            }
        }
    }

    public CaptureResult capture(String checkpoint, CaptureMode mode, Supplier<Shell> shellSupplier) {
        Objects.requireNonNull(mode, "mode");
        try {
            Display display = displaySupplier.get();
            if (display == null || display.isDisposed()) {
                return result(CaptureResult.Status.SKIPPED, mode, null, "No live application Display", null);
            }
            FutureTask<CaptureResult> task = new FutureTask<>(() -> captureOnUi(display, shellSupplier, mode, checkpoint));
            if (Display.getCurrent() == display) {
                task.run();
            } else {
                display.asyncExec(task);
            }
            try {
                return task.get(options.timeout().toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException interrupted) {
                task.cancel(false);
                Thread.currentThread().interrupt();
                return result(CaptureResult.Status.FAILED, mode, null, "Capture interrupted", interrupted);
            } catch (TimeoutException timeout) {
                task.cancel(false);
                return result(CaptureResult.Status.FAILED, mode, null, "UI capture timed out", timeout);
            }
        } catch (Exception | LinkageError failure) {
            Throwable cause = failure instanceof ExecutionException ? failure.getCause() : failure;
            return result(CaptureResult.Status.FAILED, mode, null, "Capture failed: " + cause, cause);
        }
    }

    private CaptureResult captureOnUi(Display display, Supplier<Shell> shellSupplier, CaptureMode mode, String checkpoint)
            throws IOException {
        if (display.isDisposed()) {
            return result(CaptureResult.Status.SKIPPED, mode, null, "Application Display disposed", null);
        }
        Shell shell = mode == CaptureMode.DESKTOP ? null : shellSupplier.get();
        if (mode != CaptureMode.DESKTOP && (shell == null || shell.isDisposed())) {
            return result(CaptureResult.Status.SKIPPED, mode, null, "Application shell unavailable or disposed", null);
        }
        if (shell != null && (shell.getDisplay() != display || !shell.isVisible() || shell.getMinimized())) {
            return result(CaptureResult.Status.SKIPPED, mode, null, "Shell hidden, minimized or on another Display", null);
        }
        Rectangle area = switch (mode) {
            case DESKTOP -> display.getBounds();
            case SHELL_CLIENT_AREA -> shell.getClientArea();
            case SHELL_DISPLAY_BOUNDS -> shell.getBounds();
        };
        if (area.width <= 0 || area.height <= 0) {
            return result(CaptureResult.Status.SKIPPED, mode, null, "Empty capture area", null);
        }
        Path output = paths.allocate(checkpoint, mode.suffix());
        boolean captured = false;
        try {
            Image image = new Image(display, area.width, area.height);
            try {
                GC graphics = mode == CaptureMode.SHELL_CLIENT_AREA ? new GC(shell) : new GC(display);
                try {
                    graphics.copyArea(image, mode == CaptureMode.SHELL_CLIENT_AREA ? 0 : area.x,
                        mode == CaptureMode.SHELL_CLIENT_AREA ? 0 : area.y);
                    ImageLoader loader = new ImageLoader();
                    loader.data = new ImageData[] {image.getImageData()};
                    loader.save(output.toString(), SWT.IMAGE_PNG);
                } finally {
                    graphics.dispose();
                }
            } finally {
                image.dispose();
            }
            if (Files.size(output) == 0) {
                throw new IOException("Capture produced an empty image: " + output);
            }
            captured = true;
            return new CaptureResult(CaptureResult.Status.CAPTURED, mode, output, area.width, area.height, "", null);
        } finally {
            if (!captured) {
                Files.deleteIfExists(output);
            }
        }
    }

    private static CaptureResult result(CaptureResult.Status status, CaptureMode mode, Path output,
            String reason, Throwable cause) {
        return new CaptureResult(status, mode, output, 0, 0, reason, cause);
    }
}