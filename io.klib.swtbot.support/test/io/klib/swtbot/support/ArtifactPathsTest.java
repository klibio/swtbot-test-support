package io.klib.swtbot.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ArtifactPathsTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void createsContainedUniqueArtifacts() throws Exception {
        Path root = temporary.getRoot().toPath().resolve("new output");
        ArtifactPaths paths = new ArtifactPaths(root, "../parameter/../../test");
        Path first = paths.allocate("../../checkpoint", "screen");
        Path second = paths.allocate("../../checkpoint", "screen");
        assertTrue(first.startsWith(root));
        assertEquals(paths.directory(), first.getParent());
        assertNotEquals(first, second);
        assertTrue(Files.exists(first));
        assertNotEquals(paths.directory(), new ArtifactPaths(root, "../parameter/../../test").directory());
    }

    @Test
    public void sanitizesEmptyAndLongNames() throws Exception {
        ArtifactPaths paths = new ArtifactPaths(temporary.getRoot().toPath(), "");
        Path artifact = paths.allocate("checkpoint".repeat(100), "shell");
        assertTrue(artifact.getFileName().toString().length() < 200);
        assertEquals("test-unnamed", paths.directory().getFileName().toString());
    }

    @Test(expected = IOException.class)
    public void rejectsFileAsOutputDirectory() throws Exception {
        new ArtifactPaths(temporary.newFile().toPath(), "test");
    }
}