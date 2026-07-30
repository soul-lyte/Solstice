package com.example.solstice.qol.armor;

import com.example.solstice.core.config.ConfigManager;
import com.example.solstice.core.module.AbstractModule;
import com.example.solstice.core.module.ModuleCategory;
import com.example.solstice.core.module.ModuleSetting;

import java.util.List;

/**
 * Real armor-piece HUD, ported to match uku3lig/armor-hud's actual system
 * (MIT, see NOTICE.md) - anchored/offset positioning relative to the
 * hotbar/screen edges (not a free-drag box like Solstice's other HUD
 * elements), drawing the real armor items via vanilla's own hotbar-slot
 * renderer (see {@code InGameHudArmorAccessor}/{@code
 * InGameHudArmorHudMixin}), not a reimplemented icon.
 *
 * <p>Two of the original mod's settings are deliberately not ported:
 * {@code pushBossbars}/{@code pushStatusEffectIcons}/{@code pushSubtitles}
 * (shoving other HUD overlays out of the way when they'd overlap) assume
 * vanilla's own fixed overlay positions - Solstice's Boss Bar is already
 * independently repositionable via its own {@code HudLayoutManager} entry,
 * so the "push" concept doesn't map onto this project's architecture the
 * same way. {@code playBreakSound} (a custom sound cue when a piece is
 * about to break) is cut for scope - everything else, including all the
 * positioning/style/durability-display options, is a faithful port.</p>
 */
public final class ArmorHudModule extends AbstractModule {

    private static final ArmorHudModule INSTANCE = new ArmorHudModule();

    public static final List<String> ANCHORS = List.of("Hotbar", "Bottom", "Top", "Top Center");
    public static final List<String> SIDES = List.of("Left", "Right");
    public static final List<String> STYLES = List.of("Hotbar", "Rounded Corners", "Rounded", "None");
    public static final List<String> ORIENTATIONS = List.of("Horizontal", "Vertical");
    public static final List<String> WIDGET_SHOWN_OPTIONS = List.of("Always", "If Any Present", "Not Empty", "Damaged Pieces");
    public static final List<String> OFFHAND_BEHAVIORS = List.of("Always Ignore", "Adhere", "Always Leave Space");
    public static final List<String> DURABILITY_DISPLAYS = List.of("Bar", "Numeric", "Percentage");

    public static int anchor = 0;
    public static int side = 0;
    public static int offsetX = 0;
    public static int offsetY = 0;
    public static int style = 0;
    public static int orientation = 0;
    public static int widgetShown = 2;
    public static int offhandBehavior = 1;
    public static int durabilityDisplay = 0;
    public static boolean reversed = false;
    public static boolean iconsShown = true;
    public static boolean warningShown = true;
    public static int minDurabilityValue = 20;
    public static double minDurabilityPercentage = 0.1;
    public static int warningBobIntensity = 3;

    private ArmorHudModule() {}

    public static ArmorHudModule getInstance() { return INSTANCE; }

    @Override public String getId()          { return "armor_hud"; }
    @Override public String getDisplayName() { return "Armor HUD"; }
    @Override public String getDescription() { return "Shows your worn armor pieces next to the hotbar, matching Uku's Armor HUD."; }
    @Override public ModuleCategory getCategory() { return ModuleCategory.QUALITY_OF_LIFE; }

    @Override
    public List<String> getSearchKeywords() {
        return List.of("armor bar", "armor pieces", "armorhud", "uku", "worn armor");
    }

    @Override protected boolean defaultEnabled() { return false; }

    @Override
    public List<ModuleSetting> getSettings() {
        ConfigManager cfg = ConfigManager.getInstance();
        return List.of(
                new ModuleSetting.ChoiceSetting("Anchor", "Which part of the screen the armor HUD is positioned relative to.",
                        ANCHORS, () -> anchor, v -> { anchor = v; cfg.set("armor_hud.anchor", v); }),
                new ModuleSetting.ChoiceSetting("Side", "Which side of the anchor point the armor HUD sits on.",
                        SIDES, () -> side, v -> { side = v; cfg.set("armor_hud.side", v); }),
                new ModuleSetting.IntSetting("Offset X", "Extra horizontal pixels away from the anchor point.",
                        -100, 100, () -> offsetX, v -> { offsetX = v; cfg.set("armor_hud.offset_x", v); }),
                new ModuleSetting.IntSetting("Offset Y", "Extra vertical pixels away from the anchor point.",
                        -100, 100, () -> offsetY, v -> { offsetY = v; cfg.set("armor_hud.offset_y", v); }),
                new ModuleSetting.ChoiceSetting("Style", "The background art drawn behind the armor slots.",
                        STYLES, () -> style, v -> { style = v; cfg.set("armor_hud.style", v); }),
                new ModuleSetting.ChoiceSetting("Orientation", "Whether armor slots line up in a row or a column.",
                        ORIENTATIONS, () -> orientation, v -> { orientation = v; cfg.set("armor_hud.orientation", v); }),
                new ModuleSetting.ChoiceSetting("Widget Shown", "Which armor slots actually get drawn.",
                        WIDGET_SHOWN_OPTIONS, () -> widgetShown, v -> { widgetShown = v; cfg.set("armor_hud.widget_shown", v); }),
                new ModuleSetting.ChoiceSetting("Offhand Slot Behavior", "How the armor HUD avoids overlapping the offhand item/attack cooldown indicator.",
                        OFFHAND_BEHAVIORS, () -> offhandBehavior, v -> { offhandBehavior = v; cfg.set("armor_hud.offhand_behavior", v); }),
                new ModuleSetting.ChoiceSetting("Durability Display", "How each armor piece's remaining durability is shown.",
                        DURABILITY_DISPLAYS, () -> durabilityDisplay, v -> { durabilityDisplay = v; cfg.set("armor_hud.durability_display", v); }),
                new ModuleSetting.BooleanSetting("Reversed", "Reverses the armor slot order (boots to helmet instead of helmet to boots).",
                        () -> reversed, v -> { reversed = v; cfg.set("armor_hud.reversed", v); }),
                new ModuleSetting.BooleanSetting("Show Empty Slot Icons", "Draws a faint empty-slot outline where an armor piece isn't worn.",
                        () -> iconsShown, v -> { iconsShown = v; cfg.set("armor_hud.icons_shown", v); }),
                new ModuleSetting.BooleanSetting("Show Low Durability Warning", "Draws a small warning icon over armor pieces about to break.",
                        () -> warningShown, v -> { warningShown = v; cfg.set("armor_hud.warning_shown", v); }),
                new ModuleSetting.IntSetting("Low Durability Threshold (Points)", "Shows the warning once remaining durability drops to this many points or fewer.",
                        0, 100, () -> minDurabilityValue, v -> { minDurabilityValue = v; cfg.set("armor_hud.min_durability_value", v); }),
                new ModuleSetting.DoubleSetting("Low Durability Threshold (Percent)", "Shows the warning once remaining durability drops to this percentage or lower.",
                        0.0, 1.0, () -> minDurabilityPercentage, v -> { minDurabilityPercentage = v; cfg.set("armor_hud.min_durability_percentage", v); }),
                new ModuleSetting.IntSetting("Warning Wobble Intensity", "How much the low-durability warning icon randomly jitters. 0 disables the wobble.",
                        0, 10, () -> warningBobIntensity, v -> { warningBobIntensity = v; cfg.set("armor_hud.warning_bob_intensity", v); })
        );
    }

    @Override
    protected void init() {
        ConfigManager cfg = ConfigManager.getInstance();
        anchor = cfg.getInt("armor_hud.anchor", 0);
        side = cfg.getInt("armor_hud.side", 0);
        offsetX = cfg.getInt("armor_hud.offset_x", 0);
        offsetY = cfg.getInt("armor_hud.offset_y", 0);
        style = cfg.getInt("armor_hud.style", 0);
        orientation = cfg.getInt("armor_hud.orientation", 0);
        widgetShown = cfg.getInt("armor_hud.widget_shown", 2);
        offhandBehavior = cfg.getInt("armor_hud.offhand_behavior", 1);
        durabilityDisplay = cfg.getInt("armor_hud.durability_display", 0);
        reversed = cfg.getBoolean("armor_hud.reversed", false);
        iconsShown = cfg.getBoolean("armor_hud.icons_shown", true);
        warningShown = cfg.getBoolean("armor_hud.warning_shown", true);
        minDurabilityValue = cfg.getInt("armor_hud.min_durability_value", 20);
        minDurabilityPercentage = cfg.getDouble("armor_hud.min_durability_percentage", 0.1);
        warningBobIntensity = cfg.getInt("armor_hud.warning_bob_intensity", 3);
    }
}
