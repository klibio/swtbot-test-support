package io.klib.swtbot.support;

public enum CaptureMode {
    DESKTOP("screen"),
    SHELL_CLIENT_AREA("shell"),
    SHELL_DISPLAY_BOUNDS("swt");

    private final String suffix;

    CaptureMode(String suffix) {
        this.suffix = suffix;
    }

    public String suffix() {
        return suffix;
    }
}