package com.example.solstice.core.util;

/**
 * Time helpers. All values are in milliseconds unless stated.
 */
public final class TimeUtil {

    private TimeUtil() {}

    public static long nowMs() {
        return System.currentTimeMillis();
    }

    public static long nowNs() {
        return System.nanoTime();
    }

    /** True if at least {@code intervalMs} ms have elapsed since {@code lastMs}. */
    public static boolean elapsed(long lastMs, long intervalMs) {
        return nowMs() - lastMs >= intervalMs;
    }
}
