package io.klib.swtbot.jupiter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.opentest4j.TestAbortedException;

public class ScreenshotExtensionTest {
    @Test
    public void preservesFailureWhenDiagnosticSupplierFails() throws Throwable {
        AssertionError original = new AssertionError("original assertion");
        ScreenshotExtension extension = new ScreenshotExtension(
            () -> { throw new AssertionError("diagnostic"); }, () -> null);
        try {
            extension.handleTestExecutionException(null, original);
            fail("Expected original failure");
        } catch (AssertionError actual) {
            assertSame(original, actual);
            assertEquals(1, actual.getSuppressed().length);
        }
    }

    @Test
    public void doesNotCaptureAbortedTest() throws Throwable {
        TestAbortedException original = new TestAbortedException("skip");
        ScreenshotExtension extension = new ScreenshotExtension(
            () -> { throw new AssertionError("must not capture"); }, () -> null);
        try {
            extension.handleTestExecutionException(null, original);
            fail("Expected abort");
        } catch (TestAbortedException actual) {
            assertSame(original, actual);
            assertEquals(0, actual.getSuppressed().length);
        }
    }
}