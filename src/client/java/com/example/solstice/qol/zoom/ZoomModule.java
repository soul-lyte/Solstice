package com.example.solstice.qol.zoom;

import com.example.solstice.core.config.ConfigManager;
import com.example.solstice.core.module.AbstractModule;
import com.example.solstice.core.module.ModuleCategory;
import com.example.solstice.core.module.ModuleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import java.util.List;

/**
 * ZoomModule - hold-to-zoom camera FOV control.
 *
 * <p>Inspired by Zoomify, re-implemented natively via a single FOV-return
 * Mixin ({@code GameRendererFovMixin}). The zoom factor is smoothed with a
 * simple time-based exponential lerp recomputed every rendered frame, so
 * behavior is frame-rate independent and the zoom eases in/out instead of
 * snapping. The zoom amount itself is no longer a fixed slider - it's a
 * baseline default, adjustable live by scrolling while the zoom key is held
 * (see {@code ClientHotbarScrollEvents} registration in {@code SolsticeClient}).</p>
 */
public final class ZoomModule extends AbstractModule {

    private static final ZoomModule INSTANCE = new ZoomModule();

    private static final int MIN_DIVISOR = 2;
    private static final int MAX_DIVISOR = 10;

    /** FOV divisor while zoomed in fully, e.g. 4 = zoom in to 1/4 the normal FOV. Adjusted by scrolling. */
    public static int zoomDivisor = 4;

    /**
     * How quickly the zoom eases toward its target each second. Higher =
     * snappier. Defaults to the top of its own range - {@code t = min(1.0,
     * smoothingSpeed * dtSeconds)} in {@link #updateAndGetFactor()} reaches
     * its 1.0 clamp within a single frame at any normal framerate once
     * this is around 60+, so 100 makes dezooming read as instant rather
     * than eased, per explicit request.
     */
    public static double smoothingSpeed = 100.0;

    private volatile boolean keyHeld = false;
    private float currentFactor = 1.0f;
    private long lastUpdateNs = System.nanoTime();

    /** The real, live keybinding - set once from {@code SolsticeClient} so this setting edits the actual binding. */
    private KeyBinding keyBinding;

    private ZoomModule() {}

    public static ZoomModule getInstance() { return INSTANCE; }

    @Override public String getId()          { return "zoom"; }
    @Override public String getDisplayName() { return "Zoom"; }
    @Override public String getDescription() { return "Hold the zoom key to smoothly zoom in your camera view. Scroll while zooming to adjust how far."; }

    @Override
    public java.util.List<String> getSearchKeywords() {
        return java.util.List.of("ok zoomer", "scope", "spyglass", "binoculars", "camera zoom");
    }
    @Override public ModuleCategory getCategory() { return ModuleCategory.QUALITY_OF_LIFE; }

    @Override
    protected void init() {
        zoomDivisor = ConfigManager.getInstance().getInt("zoom.divisor", 4);
        smoothingSpeed = ConfigManager.getInstance().getDouble("zoom.smoothing_speed", 100.0);
    }

    @Override
    public List<ModuleSetting> getSettings() {
        return List.of(
                new ModuleSetting.KeySetting(
                        "Zoom Key",
                        "Which key you hold down to zoom in.",
                        () -> keyBinding != null ? keyBinding.getBoundKeyLocalizedText().getString() : "Unbound",
                        this::rebind),
                new ModuleSetting.DoubleSetting(
                        "Smoothing Speed",
                        "How quickly the zoom eases in and out. Higher is snappier, lower is smoother.",
                        1.0, 100.0,
                        () -> smoothingSpeed,
                        v -> { smoothingSpeed = v; ConfigManager.getInstance().set("zoom.smoothing_speed", v); })
        );
    }

    /** Called once from {@code SolsticeClient} at init so the keybind setting edits the real binding. */
    public void bindKeybinding(KeyBinding keyBinding) {
        this.keyBinding = keyBinding;
    }

    private void rebind(int keyCode) {
        if (keyBinding == null) return;
        keyBinding.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(keyCode));
        KeyBinding.updateKeysByCode();
        MinecraftClient.getInstance().options.write();
    }

    /** Called each tick from {@link com.example.solstice.SolsticeClient} with the zoom key's held state. */
    public void setKeyHeld(boolean held) {
        this.keyHeld = held;
    }

    public boolean isKeyHeld() {
        return keyHeld;
    }

    /** Called from the scroll-to-zoom listener in {@code SolsticeClient} while the zoom key is held. */
    public void adjustZoomDivisor(double scrollDelta) {
        int next = zoomDivisor + (scrollDelta > 0 ? 1 : -1);
        zoomDivisor = Math.max(MIN_DIVISOR, Math.min(MAX_DIVISOR, next));
        ConfigManager.getInstance().set("zoom.divisor", zoomDivisor);
    }

    /**
     * Called every rendered frame from {@code GameRendererFovMixin}. Advances the
     * smoothed zoom factor and returns it - callers divide the vanilla FOV by this.
     * Always advances the clock (even when disabled) so re-enabling never causes
     * a stale-timestamp jump, the same class of bug fixed in {@code HudOverlay}.
     */
    public float updateAndGetFactor() {
        long now = System.nanoTime();
        double dtSeconds = (now - lastUpdateNs) / 1_000_000_000.0;
        lastUpdateNs = now;

        if (!isEnabled()) {
            currentFactor = 1.0f;
            return 1.0f;
        }

        float target = keyHeld ? zoomDivisor : 1.0f;
        double t = Math.min(1.0, smoothingSpeed * dtSeconds);
        currentFactor += (target - currentFactor) * t;
        return currentFactor;
    }
}
