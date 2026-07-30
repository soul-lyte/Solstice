package com.example.solstice.performance.memory;

import com.example.solstice.core.module.AbstractModule;
import com.example.solstice.core.module.ModuleCategory;
import com.example.solstice.core.util.MemoryUtil;
import com.example.solstice.core.util.TimeUtil;
import com.example.solstice.SolsticeMod;

/**
 * MemoryModule - reduces heap pressure and GC pause impact.
 *
 * <p>Strategies:
 * <ul>
 *   <li>Periodic GC hint when heap utilization exceeds a configurable threshold</li>
 *   <li>Texture/model cache trimming when unused for {@code cacheMaxAgeMs}</li>
 *   <li>Lazy initialization guards (delegated to Mixins in startup/ and memory/)</li>
 * </ul>
 *
 * <p>Inspired by FerriteCore and Memory Leak Fix, re-implemented natively.</p>
 */
public final class MemoryModule extends AbstractModule {

    private static final MemoryModule INSTANCE = new MemoryModule();

    /** Heap fraction above which Solstice emits a GC hint. Default: 85 %. */
    public static double gcHintThreshold = 0.85;

    /** Minimum interval between GC hints, in ms (avoids spamming). */
    public static long gcHintIntervalMs = 30_000; // 30 seconds

    private long lastGcHintMs = 0;

    private MemoryModule() {}

    public static MemoryModule getInstance() { return INSTANCE; }

    @Override public String getId()          { return "memory"; }
    @Override public String getDisplayName() { return "Memory Optimization"; }
    @Override public String getDescription() { return "Trims heap usage via intelligent GC hints and texture cache pruning to reduce GC pause stutters."; }

    @Override
    public java.util.List<String> getSearchKeywords() {
        return java.util.List.of("ferritecore", "memory leak fix", "ram usage", "gc pause", "garbage collection", "less ram");
    }
    @Override public ModuleCategory getCategory() { return ModuleCategory.ADVANCED; }

    /** Always active - see class Javadoc. Ignores persisted config and UI toggles. */
    @Override public boolean isEnabled() { return true; }
    @Override public void setEnabled(boolean enabled) { /* always on - no-op */ }
    @Override public boolean isAlwaysOn() { return true; }

    @Override
    public java.util.List<com.example.solstice.core.module.ModuleSetting> getSettings() {
        return java.util.List.of(
                new com.example.solstice.core.module.ModuleSetting.DoubleSetting(
                        "GC Hint Heap Threshold (%)",
                        "How full your game's memory can get before Solstice asks Java to clean up memory that's no longer being used.",
                        0.50, 0.99,
                        () -> gcHintThreshold,
                        v -> { gcHintThreshold = v; com.example.solstice.core.config.ConfigManager.getInstance().set("memory.gc_threshold", v); }),
                new com.example.solstice.core.module.ModuleSetting.IntSetting(
                        "GC Hint Interval (ms)",
                        "The minimum time Solstice waits between memory clean-up requests, so it doesn't ask too often.",
                        5_000, 120_000,
                        () -> (int) gcHintIntervalMs,
                        v -> { gcHintIntervalMs = v; com.example.solstice.core.config.ConfigManager.getInstance().set("memory.gc_interval_ms", v); })
        );
    }

    @Override
    protected void init() {
        gcHintThreshold = com.example.solstice.core.config.ConfigManager.getInstance()
                .getDouble("memory.gc_threshold", 0.85);
        gcHintIntervalMs = com.example.solstice.core.config.ConfigManager.getInstance()
                .getInt("memory.gc_interval_ms", 30_000);
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        if (!TimeUtil.elapsed(lastGcHintMs, gcHintIntervalMs)) return;

        double fraction = MemoryUtil.heapFraction();
        if (fraction >= gcHintThreshold) {
            SolsticeMod.LOGGER.debug(
                "[Solstice/Memory] Heap at {:.1f}% - requesting GC.", fraction * 100);
            MemoryUtil.suggestGc();
            lastGcHintMs = TimeUtil.nowMs();
        }
    }

    /** Returns a formatted memory status string for the HUD. */
    public String getHudString() {
        long used = MemoryUtil.toMiB(MemoryUtil.heapUsed());
        long max  = MemoryUtil.toMiB(MemoryUtil.heapMax());
        return used + "/" + max + " MiB";
    }
}
