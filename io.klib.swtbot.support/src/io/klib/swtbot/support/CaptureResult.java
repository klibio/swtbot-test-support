package io.klib.swtbot.support;

import java.nio.file.Path;

/**
 * Capture outcome; no constructor validation is performed on caller-created results.
 * @param status capture outcome
 * @param mode requested capture region
 * @param outputPath completed PNG path, or null when no artifact was produced
 * @param width captured width in pixels, zero for unsuccessful library results
 * @param height captured height in pixels, zero for unsuccessful library results
 * @param reason human-readable outcome explanation
 * @param cause underlying failure, or null when none is available
 */
public record CaptureResult(Status status, CaptureMode mode, Path outputPath, int width, int height,
        String reason, Throwable cause) {
    /** Whether evidence was written, unavailable, or failed during capture. */
    public enum Status {
        /** A PNG was written successfully. */
        CAPTURED,
        /** No usable visible application shell/display was available. */
        SKIPPED,
        /** Capture, dispatch or file writing failed. */
        FAILED
    }

    /**
     * Requires actual evidence rather than treating skipped capture as success.
     * @return this result when captured
     * @throws IllegalStateException if status is not {@link Status#CAPTURED}, retaining the cause
     */
    public CaptureResult requireCaptured() {
        if (status != Status.CAPTURED) {
            throw new IllegalStateException("Screenshot " + mode + " " + status + ": " + reason, cause);
        }
        return this;
    }
}