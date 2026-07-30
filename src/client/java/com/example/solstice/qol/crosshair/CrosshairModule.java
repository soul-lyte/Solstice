package com.example.solstice.qol.crosshair;

import com.example.solstice.core.config.ConfigManager;
import com.example.solstice.core.module.AbstractModule;
import com.example.solstice.core.module.ModuleCategory;
import com.example.solstice.core.module.ModuleSetting;

import java.util.List;

/**
 * CrosshairModule - the crosshair is always plain vanilla, except the
 * instant you're aiming at an entity you're actually in range to hit
 * ({@code MinecraftClient#crosshairTarget} is an {@code EntityHitResult} -
 * vanilla's own raycast already accounts for reach distance, so no extra
 * range math is needed), at which point it swaps to the chosen style. It
 * never changes based on blocks or empty air, and never hides.
 */
public final class CrosshairModule extends AbstractModule {

    private static final CrosshairModule INSTANCE = new CrosshairModule();

    public static final int STYLE_VANILLA_CUSTOM_COLOR = 0;
    public static final int STYLE_CUSTOM_TEXTURE = 1;
    /** Default tint when using "Vanilla (Custom Color)" - packed 0xRRGGBB, red. */
    private static final int DEFAULT_COLOR = 0xFF0000;

    /** 0 = Vanilla shape with a custom color tint, 1 = fully custom texture. */
    public static int styleIndex = STYLE_VANILLA_CUSTOM_COLOR;
    /** Packed 0xRRGGBB, edited via a real color picker instead of three R/G/B sliders. */
    public static int color = DEFAULT_COLOR;

    private CrosshairModule() {}

    public static CrosshairModule getInstance() { return INSTANCE; }

    @Override public String getId()          { return "combat_crosshair"; }
    @Override public String getDisplayName() { return "Combat Crosshair"; }
    @Override public String getDescription() { return "Swaps to a custom crosshair only while aiming at an entity within hitting range; plain vanilla otherwise."; }

    @Override
    public java.util.List<String> getSearchKeywords() {
        return java.util.List.of("dynamic crosshair", "reticle", "crosshair color", "pvp crosshair");
    }
    @Override public ModuleCategory getCategory() { return ModuleCategory.QUALITY_OF_LIFE; }

    @Override
    protected void init() {
        styleIndex = ConfigManager.getInstance().getInt("combat_crosshair.style", STYLE_VANILLA_CUSTOM_COLOR);
        color = ConfigManager.getInstance().getInt("combat_crosshair.color", DEFAULT_COLOR);
    }

    public static void setStyleIndex(int index) {
        styleIndex = index;
        ConfigManager.getInstance().set("combat_crosshair.style", index);
    }

    public static void setColor(int newColor) {
        color = newColor;
        ConfigManager.getInstance().set("combat_crosshair.color", newColor);
    }

    @Override
    public List<ModuleSetting> getSettings() {
        return List.of(
                new ModuleSetting.ChoiceSetting(
                        "Combat Crosshair Style",
                        "What the crosshair looks like the moment you're aiming at something you could hit - it's always plain vanilla the rest of the time.",
                        List.of("Vanilla (Custom Color)", "Custom Texture"),
                        () -> styleIndex,
                        CrosshairModule::setStyleIndex),
                new ModuleSetting.ColorSetting("Color",
                        "Tint color for the custom crosshair (only used in \"Vanilla (Custom Color)\" style).",
                        () -> color,
                        CrosshairModule::setColor)
        );
    }

    public int getTintColor() {
        return 0xFF000000 | color;
    }
}
