package io.klib.swtbot.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

public final class ArtifactPaths {
    private final Path directory;

    public ArtifactPaths(Path root, String testId) throws IOException {
        Path output = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
        Files.createDirectories(output);
        if (!Files.isWritable(output)) {
            throw new IOException("Screenshot directory is not writable: " + output);
        }
        directory = Files.createDirectory(output.resolve("run-" + UUID.randomUUID()))
            .resolve("test-" + safeName(testId));
        Files.createDirectories(directory);
    }

    public Path directory() {
        return directory;
    }

    public Path allocate(String checkpoint, String mode) throws IOException {
        return Files.createTempFile(directory, safeName(checkpoint) + "_" + safeName(mode) + "_", ".png");
    }

    private static String safeName(String value) {
        String name = Objects.requireNonNull(value, "artifact name").replaceAll("[^a-zA-Z0-9._-]", "_");
        return name.isBlank() ? "unnamed" : name.substring(0, Math.min(name.length(), 80));
    }
}