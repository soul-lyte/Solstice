package com.example.solstice.qol.combathitbox;

import com.example.solstice.core.config.ConfigManager;
import com.example.solstice.core.module.AbstractModule;
import com.example.solstice.core.module.ModuleCategory;
import com.example.solstice.ui.CombatHitboxEditScreen;
import com.example.solstice.ui.EditableModule;
import net.minecraft.client.gui.screen.Screen;

import java.util.List;

/**
 * Outline-width/color settings and on/off switch for {@link CombatHitboxRenderer} -
 * its own standalone Quality of Life card, split out from the Visuals bundle
 * since it needs a real settings screen (color picker + preview), not a flat
 * toggle row. Off by default.
 */
public final class CombatHitboxModule extends AbstractModule implements EditableModule {

    private static final CombatHitboxModule INSTANCE = new CombatHitboxModule();

    public static float outlineWidth = 1.5f;
    public static int outlineColor = 0xFFFFFFFF;

    private CombatHitboxModule() {}

    public static CombatHitboxModule getInstance() { return INSTANCE; }

    @Override public String getId()          { return "combat_hitbox"; }
    @Override public String getDisplayName() { return "Combat Hitbox"; }
    @Override public String getDescription() { return "Draws an outline around nearby entities, turning red for whichever one you're aiming at. Visual only - never changes actual hitboxes, reach, or combat."; }
    @Override public ModuleCategory getCategory() { return ModuleCategory.QUALITY_OF_LIFE; }

    @Override
    public List<String> getSearchKeywords() {
        return List.of("combat hitbox", "hitboxes", "entity outline", "target outline", "sootysplash");
    }

    @Override protected boolean defaultEnabled() { return false; }

    /** Not default anywhere, including PVP - an opt-in overlay, not something the PVP profile should force on. */
    @Override public boolean excludeFromPvpProfile() { return true; }

    @Override
    public Screen createEditScreen(Screen parent) {
        return new CombatHitboxEditScreen(parent);
    }

    @Override
    protected void init() {
        ConfigManager cfg = ConfigManager.getInstance();
        outlineWidth = (float) cfg.getDouble("combat_hitbox.outline_width", 1.5);
        outlineColor = cfg.getInt("combat_hitbox.outline_color", 0xFFFFFFFF);
    }

    public static void setOutlineWidth(float v) { outlineWidth = v; ConfigManager.getInstance().set("combat_hitbox.outline_width", (double) v); }
    public static void setOutlineColor(int v) { outlineColor = v; ConfigManager.getInstance().set("combat_hitbox.outline_color", v); }
}
