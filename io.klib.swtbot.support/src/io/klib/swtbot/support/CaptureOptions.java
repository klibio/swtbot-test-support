package io.klib.swtbot.support;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public record CaptureOptions(Path outputDirectory, Duration timeout, List<CaptureMode> modes) {
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

    public static CaptureOptions defaults(Path outputDirectory) {
        return new CaptureOptions(outputDirectory, Duration.ofSeconds(5), List.of(CaptureMode.SHELL_CLIENT_AREA));
    }
}