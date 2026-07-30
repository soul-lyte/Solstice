package com.example.solstice.core.util;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * JVM memory introspection helpers.
 */
public final class MemoryUtil {

    private static final MemoryMXBean MX = ManagementFactory.getMemoryMXBean();

    private MemoryUtil() {}

    /** Heap used, in bytes. */
    public static long heapUsed() {
        return MX.getHeapMemoryUsage().getUsed();
    }

    /** Heap committed (reserved by JVM), in bytes. */
    public static long heapCommitted() {
        return MX.getHeapMemoryUsage().getCommitted();
    }

    /** Heap max (configured with -Xmx), in bytes. */
    public static long heapMax() {
        return MX.getHeapMemoryUsage().getMax();
    }

    /** Heap utilization as a fraction in [0, 1]. */
    public static double heapFraction() {
        MemoryUsage u = MX.getHeapMemoryUsage();
        long max = u.getMax();
        return max > 0 ? (double) u.getUsed() / max : 0.0;
    }

    /** Convert bytes → mebibytes (rounded). */
    public static long toMiB(long bytes) {
        return bytes / (1024L * 1024L);
    }

    /** Request a GC hint. Only call when utilization is critically high. */
    public static void suggestGc() {
        System.gc();
    }
}
