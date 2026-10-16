package io.klib.swtbot.junit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class ScreenshotRuleTest {
    @Test
    public void preservesFailureIfDiagnosticSupplierFails() throws Throwable {
        AssertionError original = new AssertionError("original assertion");
        ScreenshotRule rule = new ScreenshotRule(() -> { throw new AssertionError("diagnostic"); }, () -> null);
        Statement failing = new Statement() {
            @Override
            public void evaluate() {
                throw original;
            }
        };
        try {
            rule.apply(failing, Description.createTestDescription(getClass(), "failure")).evaluate();
            fail("Expected original failure");
        } catch (AssertionError actual) {
            assertSame(original, actual);
            assertEquals(1, actual.getSuppressed().length);
        }
    }

    @Test
    public void doesNotCaptureSuccessfulTest() throws Throwable {
        ScreenshotRule rule = new ScreenshotRule(() -> { throw new AssertionError("must not capture"); }, () -> null);
        rule.apply(new Statement() {
            @Override
            public void evaluate() {}
        }, Description.createTestDescription(getClass(), "success")).evaluate();
    }
}