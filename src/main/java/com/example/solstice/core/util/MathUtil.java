package com.example.solstice.core.util;

/**
 * Lightweight math helpers that avoid allocations.
 */
public final class MathUtil {

    private MathUtil() {}

    /** Linearly interpolate between a and b by t in [0,1]. */
    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /** Clamp value to [min, max]. */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Squared distance between two 3-D integer positions. */
    public static long distSq(int x1, int y1, int z1, int x2, int y2, int z2) {
        long dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        return dx * dx + dy * dy + dz * dz;
    }

    /** Returns true when {@code value} is a power of two. */
    public static boolean isPowerOfTwo(int value) {
        return value > 0 && (value & (value - 1)) == 0;
    }
}
