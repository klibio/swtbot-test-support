package io.klib.swtbot.support;

/** Selects the captured pixel region; screen-based modes can expose private content. */
public enum CaptureMode {
    /** Captures display bounds, potentially including other applications and monitors. */
    DESKTOP("screen"),
    /** Captures the shell client area directly, excluding window decorations. */
    SHELL_CLIENT_AREA("shell"),
    /** Captures the shell's display rectangle, including decorations and possible occlusion. */
    SHELL_DISPLAY_BOUNDS("swt");

    private final String suffix;

    CaptureMode(String suffix) {
        this.suffix = suffix;
    }

    /**
     * Returns the artifact label for this region.
     * @return the stable artifact label: screen, shell, or swt
     */
    public String suffix() {
        return suffix;
    }
}