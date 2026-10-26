package io.klib.swtbot.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

/** Creates isolated run/test directories and reserves unique PNG artifact paths. */
public final class ArtifactPaths {
    private final Path directory;

    /**
     * Creates a new run directory below the normalized absolute output root.
     * Test identifiers are sanitized and limited to 80 characters.
     * @param root writable output root; created if absent
     * @param testId non-null test identifier
     * @throws IOException if directories cannot be created or the root is not writable
     * @throws NullPointerException if an argument is null
     */
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

    /**
     * Returns this run's test directory.
     * @return the absolute directory dedicated to this run and test
     */
    public Path directory() {
        return directory;
    }

    /**
     * Reserves a unique empty file, not a completed screenshot.
     * @param checkpoint non-null checkpoint name, sanitized and bounded
     * @param mode non-null mode label, sanitized and bounded
     * @return path to a newly created empty PNG file within {@link #directory()}
     * @throws IOException if the file cannot be created
     * @throws NullPointerException if a name is null
     */
    public Path allocate(String checkpoint, String mode) throws IOException {
        return Files.createTempFile(directory, safeName(checkpoint) + "_" + safeName(mode) + "_", ".png");
    }

    private static String safeName(String value) {
        String name = Objects.requireNonNull(value, "artifact name").replaceAll("[^a-zA-Z0-9._-]", "_");
        return name.isBlank() ? "unnamed" : name.substring(0, Math.min(name.length(), 80));
    }
}