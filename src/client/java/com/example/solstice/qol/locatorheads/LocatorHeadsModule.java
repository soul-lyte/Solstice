package com.example.solstice.qol.locatorheads;

import com.example.solstice.core.config.ConfigManager;
import com.example.solstice.core.module.AbstractModule;
import com.example.solstice.core.module.ModuleCategory;
import com.example.solstice.core.module.ModuleSetting;

import java.util.List;

/**
 * Renders nearby players' real locator-bar waypoint icons (the small
 * directional markers above the XP bar, confirmed via decompile:
 * {@code LocatorBar.renderAddons}) as their actual player heads instead of
 * the generic waypoint-style sprite. See {@code
 * mixin.locatorheads.LocatorBarMixin} for the actual swap.
 *
 * <p>Ported from Haage001/locator-heads (LGPL-3.0-only, see NOTICE.md) -
 * scoped to its core feature (head-instead-of-icon, distance-based size
 * falloff, size multiplier, optional name label) rather than its full
 * feature set (team-color borders, name fade animations, look-at/
 * player-list-key name-display modes, player include/exclude filters) -
 * real, deliberate scope cut for a "not default anywhere" module, not a
 * partial/broken port.</p>
 */
public final class LocatorHeadsModule extends AbstractModule {

    private static final LocatorHeadsModule INSTANCE = new LocatorHeadsModule();

    public static double headSizeMultiplier = 1.0;
    public static boolean showNames = false;

    private LocatorHeadsModule() {}

    public static LocatorHeadsModule getInstance() { return INSTANCE; }

    @Override public String getId()          { return "locator_heads"; }
    @Override public String getDisplayName() { return "Locator Heads"; }
    @Override public String getDescription() { return "Shows nearby players' real heads on the locator bar instead of the generic waypoint icon."; }
    @Override public ModuleCategory getCategory() { return ModuleCategory.QUALITY_OF_LIFE; }

    @Override
    public List<String> getSearchKeywords() {
        return List.of("radar", "player tracker", "compass heads", "locatorheads", "waypoint heads");
    }

    @Override protected boolean defaultEnabled() { return false; }

    /** Not default anywhere, including PVP - per explicit request. */
    @Override public boolean excludeFromPvpProfile() { return true; }

    @Override
    public List<ModuleSetting> getSettings() {
        return List.of(
                new ModuleSetting.DoubleSetting(
                        "Head Size",
                        "Scales the head icon relative to the original waypoint marker's size.",
                        0.5, 2.0,
                        () -> headSizeMultiplier,
                        v -> { headSizeMultiplier = v; ConfigManager.getInstance().set("locator_heads.size_multiplier", v); }),
                new ModuleSetting.BooleanSetting(
                        "Show Names",
                        "Shows the player's name above their head icon.",
                        () -> showNames,
                        v -> { showNames = v; ConfigManager.getInstance().set("locator_heads.show_names", v); })
        );
    }

    @Override
    protected void init() {
        headSizeMultiplier = ConfigManager.getInstance().getDouble("locator_heads.size_multiplier", 1.0);
        showNames = ConfigManager.getInstance().getBoolean("locator_heads.show_names", false);
    }
}
