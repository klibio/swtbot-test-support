package io.klib.swtbot.support;

import java.nio.file.Path;

public record CaptureResult(Status status, CaptureMode mode, Path outputPath, int width, int height,
        String reason, Throwable cause) {
    public enum Status {
        CAPTURED, SKIPPED, FAILED
    }

    public CaptureResult requireCaptured() {
        if (status != Status.CAPTURED) {
            throw new IllegalStateException("Screenshot " + mode + " " + status + ": " + reason, cause);
        }
        return this;
    }
}