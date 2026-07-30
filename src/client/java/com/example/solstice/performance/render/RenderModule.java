package com.example.solstice.performance.render;

import com.example.solstice.core.config.ConfigManager;
import com.example.solstice.core.module.AbstractModule;
import com.example.solstice.core.module.ModuleCategory;
import com.example.solstice.viewdistance.ViewDistanceModule;
import net.minecraft.client.MinecraftClient;

/**
 * RenderModule - global render pipeline optimizations.
 *
 * <p>Client-only: uses {@link MinecraftClient} directly.
 * Lives in src/client/java.</p>
 *
 * <p>Covers:
 * <ul>
 *   <li>Sleep render thread when window is minimized (saves CPU/GPU)</li>
 *   <li>Dynamic render distance scaling when FPS drops below threshold</li>
 * </ul>
 */
public final class RenderModule extends AbstractModule {

    private static final RenderModule INSTANCE = new RenderModule();

    /** ms to sleep per frame when the window is minimized (≈ 4 FPS cap). */
    public static int minimizedSleepMs = 250;

    /**
     * Off by default - actually shrinking/growing the real view distance option
     * means real chunks get unloaded then re-requested from the server as it
     * adjusts, which reads as "chunks randomly reloading" if your FPS hovers
     * near the threshold (repeated shrink/grow cycles). A real, working
     * optimization, but disruptive enough that it shouldn't be forced on
     * every player by default - opt in explicitly.
     */
    public static boolean dynamicRenderDistanceEnabled = false;

    /** FPS below which render distance is nudged down by 1 chunk. */
    public static int dynamicRenderDistanceFpsThreshold = 30;

    /** Consecutive frames below threshold before adjusting render distance. */
    public static int dynamicRenderDistanceGraceFrames = 60;

    private int lowFpsFrameCounter = 0;
    private int highFpsFrameCounter = 0;
    private int baselineViewDistance = -1;
    private int lastSetValue = -1;

    private RenderModule() {}

    public static RenderModule getInstance() { return INSTANCE; }

    @Override public String getId()          { return "render"; }
    @Override public String getDisplayName() { return "Render Optimizations"; }
    @Override public String getDescription() { return "Reduces GPU overhead: minimized-sleep, dynamic render distance scaling."; }

    @Override
    public java.util.List<String> getSearchKeywords() {
        return java.util.List.of("fps boost", "background fps", "minimized sleep", "dynamic render distance");
    }
    @Override public ModuleCategory getCategory() { return ModuleCategory.ADVANCED; }

    /** Always active - see class Javadoc. Ignores persisted config and UI toggles. */
    @Override public boolean isEnabled() { return true; }
    @Override public void setEnabled(boolean enabled) { /* always on - no-op */ }
    @Override public boolean isAlwaysOn() { return true; }

    @Override
    public java.util.List<com.example.solstice.core.module.ModuleSetting> getSettings() {
        return java.util.List.of(
                new com.example.solstice.core.module.ModuleSetting.IntSetting(
                        "Minimized Sleep (ms)",
                        "How long the game pauses rendering each frame while the window is minimized, to save CPU when you're not looking at it.",
                        0, 1000,
                        () -> minimizedSleepMs,
                        v -> { minimizedSleepMs = v; ConfigManager.getInstance().set("render.minimized_sleep_ms", v); }),
                new com.example.solstice.core.module.ModuleSetting.BooleanSetting(
                        "Dynamic Render Distance",
                        "Temporarily shrinks your render distance when FPS is low, growing it back once it recovers. Off by default - it really unloads/reloads chunks from the server as it adjusts, which can look like random chunk reloading if your FPS hovers near the threshold.",
                        () -> dynamicRenderDistanceEnabled,
                        v -> { dynamicRenderDistanceEnabled = v; ConfigManager.getInstance().set("render.dynamic_rd_enabled", v); }),
                new com.example.solstice.core.module.ModuleSetting.IntSetting(
                        "Dynamic Render Distance FPS Threshold",
                        "If your FPS drops below this number for a while, Solstice temporarily shrinks your render distance to help it recover.",
                        5, 120,
                        () -> dynamicRenderDistanceFpsThreshold,
                        v -> { dynamicRenderDistanceFpsThreshold = v; ConfigManager.getInstance().set("render.dynamic_rd_fps_threshold", v); }),
                new com.example.solstice.core.module.ModuleSetting.IntSetting(
                        "Dynamic Render Distance Grace Frames",
                        "How many frames in a row your FPS has to stay low before Solstice shrinks your render distance. Higher means it waits longer before acting.",
                        1, 300,
                        () -> dynamicRenderDistanceGraceFrames,
                        v -> { dynamicRenderDistanceGraceFrames = v; ConfigManager.getInstance().set("render.dynamic_rd_grace_frames", v); })
        );
    }

    @Override
    protected void init() {
        minimizedSleepMs = ConfigManager.getInstance()
                .getInt("render.minimized_sleep_ms", 250);
        dynamicRenderDistanceEnabled = ConfigManager.getInstance()
                .getBoolean("render.dynamic_rd_enabled", false);
        dynamicRenderDistanceFpsThreshold = ConfigManager.getInstance()
                .getInt("render.dynamic_rd_fps_threshold", 30);
        dynamicRenderDistanceGraceFrames = ConfigManager.getInstance()
                .getInt("render.dynamic_rd_grace_frames", 60);
    }

    /**
     * Called from {@code GameRendererMixin} each rendered frame.
     * Manages the dynamic render-distance logic.
     *
     * <p>Tracks a "baseline" view distance separate from whatever value is
     * currently active, so a manual change in vanilla's video settings is
     * recognized as the user's real preference instead of being eroded again
     * the next time FPS dips - and restores back toward that baseline once
     * FPS recovers, since previously there was no path back up at all.</p>
     */
    public void onFrameRendered(MinecraftClient client, float fps) {
        if (!isEnabled()) return;
        if (!dynamicRenderDistanceEnabled) return;

        // ViewDistanceModule and this feature both want to control the same vanilla
        // view-distance setting for opposite reasons (one extends it, this one shrinks
        // it under load) - fighting over it produces constant real chunk load/unload
        // churn that looks like random reloading. Defer to the user's explicit choice.
        if (ViewDistanceModule.getInstance().isEnabled()) return;

        int current = client.options.getViewDistance().getValue();

        // First run, or the value changed since we last set it ourselves: the
        // user changed it manually - adopt it as the new baseline.
        if (baselineViewDistance == -1 || (lastSetValue != -1 && current != lastSetValue)) {
            baselineViewDistance = current;
            lowFpsFrameCounter = 0;
            highFpsFrameCounter = 0;
        }

        if (fps < dynamicRenderDistanceFpsThreshold) {
            highFpsFrameCounter = 0;
            lowFpsFrameCounter++;
            if (lowFpsFrameCounter >= dynamicRenderDistanceGraceFrames && current > 4) {
                setViewDistance(client, current - 1);
                lowFpsFrameCounter = 0;
            }
        } else {
            lowFpsFrameCounter = Math.max(0, lowFpsFrameCounter - 2);
            if (current < baselineViewDistance) {
                highFpsFrameCounter++;
                if (highFpsFrameCounter >= dynamicRenderDistanceGraceFrames) {
                    setViewDistance(client, current + 1);
                    highFpsFrameCounter = 0;
                }
            }
        }
    }

    private void setViewDistance(MinecraftClient client, int value) {
        client.options.getViewDistance().setValue(value);
        lastSetValue = value;
    }
}
