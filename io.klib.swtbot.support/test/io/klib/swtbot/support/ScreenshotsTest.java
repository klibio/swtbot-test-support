package io.klib.swtbot.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ScreenshotsTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void missingDisplayIsExplicitlySkipped() throws Exception {
        Screenshots screenshots = new Screenshots(CaptureOptions.defaults(temporary.getRoot().toPath()), "test", () -> null);
        CaptureResult result = screenshots.capture("failure", CaptureMode.SHELL_CLIENT_AREA, () -> null);
        assertEquals(CaptureResult.Status.SKIPPED, result.status());
        assertTrue(result.reason().contains("Display"));
    }

    @Test
    public void guardPreservesOriginalAssertionEvenWhenDisplaySupplierFails() throws Throwable {
        Screenshots screenshots = new Screenshots(CaptureOptions.defaults(temporary.getRoot().toPath()), "test",
            () -> { throw new AssertionError("no display"); });
        AssertionError original = new AssertionError("expected failure");
        try {
            screenshots.guard("failure", () -> null, () -> { throw original; });
            fail("Guard must rethrow original failure");
        } catch (AssertionError actual) {
            assertSame(original, actual);
            assertEquals(1, actual.getSuppressed().length);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsZeroTimeout() {
        new CaptureOptions(temporary.getRoot().toPath(), Duration.ZERO, List.of(CaptureMode.DESKTOP));
    }

    @Test(expected = IllegalStateException.class)
    public void requiredCheckpointDoesNotSilentlySkip() throws Exception {
        Screenshots screenshots = new Screenshots(CaptureOptions.defaults(temporary.getRoot().toPath()), "test", () -> null);
        screenshots.requireCheckpoint("required", () -> null);
    }
}