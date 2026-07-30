package com.example.solstice.qol.inventoryhud;

import com.example.solstice.core.module.AbstractModule;
import com.example.solstice.core.module.ModuleCategory;

import java.util.List;

/**
 * On/off switch for {@link com.example.solstice.ui.InventoryHudElement} - its
 * own standalone Quality of Life card, split out from the Visuals bundle
 * since it's a real HUD element (position/size live in the HUD editor, not
 * here) rather than a small visual tweak. Off by default.
 */
public final class InventoryHudModule extends AbstractModule {

    private static final InventoryHudModule INSTANCE = new InventoryHudModule();

    private InventoryHudModule() {}

    public static InventoryHudModule getInstance() { return INSTANCE; }

    @Override public String getId()          { return "inventory_hud"; }
    @Override public String getDisplayName() { return "Inventory HUD"; }
    @Override public String getDescription() { return "Shows your inventory as a small icon grid on screen, moveable from the HUD editor."; }
    @Override public ModuleCategory getCategory() { return ModuleCategory.QUALITY_OF_LIFE; }

    @Override
    public List<String> getSearchKeywords() {
        return List.of("inventory hud", "inventoryhud+", "inventory overlay", "item grid hud");
    }

    @Override protected boolean defaultEnabled() { return false; }

    /** Not default anywhere, including PVP - shows your real inventory contents, which the PVP profile shouldn't force on. */
    @Override public boolean excludeFromPvpProfile() { return true; }

    @Override
    protected void init() {
        // No settings of its own - position/size/visibility live in HudLayoutManager,
        // driven by the HUD editor, same as every other free-drag HudElement.
    }
}
