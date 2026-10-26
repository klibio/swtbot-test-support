package io.klib.swtbot.support;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Immutable capture settings. Screen-based modes require explicit opt-in.
 * @param outputDirectory non-null root for isolated run/test directories
 * @param timeout positive completion budget of at least one millisecond; cannot stop executing native work
 * @param modes nonempty, distinct capture modes, defensively copied in capture order
 */
public record CaptureOptions(Path outputDirectory, Duration timeout, List<CaptureMode> modes) {
    /**
     * Validates options and copies the mode list.
     * @param outputDirectory screenshot output root
     * @param timeout capture completion budget
     * @param modes ordered modes to capture
     * @throws NullPointerException if an argument or mode is null
     * @throws IllegalArgumentException if timeout is below one millisecond or modes are empty or repeated
     * @throws ArithmeticException if timeout cannot be represented in milliseconds
     */
    public CaptureOptions {
        Objects.requireNonNull(outputDirectory, "outputDirectory");
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isNegative() || timeout.isZero() || timeout.toMillis() < 1) {
            throw new IllegalArgumentException("Capture timeout must be at least one millisecond");
        }
        modes = List.copyOf(modes);
        if (modes.isEmpty() || modes.stream().distinct().count() != modes.size()) {
            throw new IllegalArgumentException("Capture modes must be nonempty and distinct");
        }
    }

    /**
     * Uses client-area capture only and a five-second completion budget.
     * @param outputDirectory non-null screenshot output root
     * @return validated default options
     * @throws NullPointerException if the output directory is null
     */
    public static CaptureOptions defaults(Path outputDirectory) {
        return new CaptureOptions(outputDirectory, Duration.ofSeconds(5), List.of(CaptureMode.SHELL_CLIENT_AREA));
    }
}