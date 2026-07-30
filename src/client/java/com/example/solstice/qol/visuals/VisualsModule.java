package com.example.solstice.qol.visuals;

import com.example.solstice.core.config.ConfigManager;
import com.example.solstice.core.module.AbstractModule;
import com.example.solstice.core.module.ModuleCategory;
import com.example.solstice.textures.TexturePackManager;
import com.example.solstice.ui.EditableModule;
import com.example.solstice.ui.VisualsEditScreen;
import net.minecraft.client.gui.screen.Screen;

/**
 * Bundles a set of small, independent visual tweaks behind one card. Each
 * sub-feature is its own persisted toggle, on by default, and only takes
 * effect while both this module AND the sub-toggle are enabled - see the
 * {@code isXActive()} helpers, which every {@code mixin.visuals} class reads
 * from instead of the raw fields directly.
 */
public final class VisualsModule extends AbstractModule implements EditableModule {

    private static final VisualsModule INSTANCE = new VisualsModule();

    /** Bundled resource pack overriding shield.json/shield_blocking.json's firstperson transforms. */
    public static final String SHIELD_PACK_ID = "visuals_shield_side_low";
    /** Bundled resource pack overriding the fire_1 overlay texture with a shorter flame graphic. */
    public static final String LOW_FIRE_PACK_ID = "visuals_low_fire";
    /** Bundled resource pack overriding totem_of_undying.json's display transforms. */
    public static final String SMALL_TOTEM_POP_PACK_ID = "visuals_small_totem_pop";
    /** Bundled resource pack overriding fishing_hook.png with a fully transparent texture. */
    public static final String NO_FISHING_BOBBER_PACK_ID = "visuals_no_fishing_bobber";

    /** Render-distance/sky fog - never fully removed (see mixin.visuals.AtmosphericFogModifierMixin for why), just pushed far out. */
    public static boolean noFogAtmospheric = true;
    public static boolean noFogWater = true;
    public static boolean noFogLava = true;
    public static boolean lowFire = true;
    public static boolean shieldSideLow = true;
    public static boolean smallTotemPop = true;
    public static boolean bowCrossbowIndicator = true;
    public static boolean noFoodParticles = true;
    public static boolean noPumpkinBlur = true;
    public static boolean noFishingBobber = true;

    private VisualsModule() {}

    public static VisualsModule getInstance() { return INSTANCE; }

    @Override public String getId()          { return "visuals"; }
    @Override public String getDisplayName() { return "Visuals"; }
    @Override public String getDescription() { return "A bundle of small visual tweaks - fog, fire overlay, shield position, totem size, and more. Edit to configure each one individually."; }

    @Override
    public java.util.List<String> getSearchKeywords() {
        return java.util.List.of("no fog", "low fire", "small fire", "totem animation", "shield position",
                "no fishing bobber", "no pumpkin blur", "no bobber", "clear water");
    }
    @Override public ModuleCategory getCategory() { return ModuleCategory.QUALITY_OF_LIFE; }

    @Override
    public Screen createEditScreen(Screen parent) {
        return new VisualsEditScreen(parent);
    }

    /** True only while both the bundle and this specific sub-feature are on. */
    public boolean isFeatureActive(boolean feature) {
        return isEnabled() && feature;
    }

    /**
     * Registers the shield/low-fire texture packs (safe this early - doesn't touch
     * {@code MinecraftClient.getInstance()}). Called from {@code init()} below.
     */
    private void registerTexturePacks() {
        TexturePackManager.getInstance().registerBuiltinPack(SHIELD_PACK_ID);
        TexturePackManager.getInstance().registerBuiltinPack(LOW_FIRE_PACK_ID);
        TexturePackManager.getInstance().registerBuiltinPack(SMALL_TOTEM_POP_PACK_ID);
        TexturePackManager.getInstance().registerBuiltinPack(NO_FISHING_BOBBER_PACK_ID);
    }

    /**
     * Enables the shield/low-fire packs if they were persisted on - needs the
     * resource pack manager, so called from {@code ClientLifecycleEvents.CLIENT_STARTED}
     * in {@code SolsticeClient}, same as the Textures tab's own persisted selections.
     * Deliberately does NOT reload itself - see {@code TexturePackManager.applyPersistedSelections}'s
     * Javadoc, the caller reloads once for every persisted-selection source together.
     *
     * @return true if any pack was actually enabled (i.e. a reload is needed)
     */
    public boolean applyPersistedTexturePacks() {
        boolean anyEnabled = false;
        if (shieldSideLow) {
            TexturePackManager.getInstance().setPackEnabled(SHIELD_PACK_ID, true);
            anyEnabled = true;
        }
        if (lowFire) {
            TexturePackManager.getInstance().setPackEnabled(LOW_FIRE_PACK_ID, true);
            anyEnabled = true;
        }
        if (smallTotemPop) {
            TexturePackManager.getInstance().setPackEnabled(SMALL_TOTEM_POP_PACK_ID, true);
            anyEnabled = true;
        }
        if (noFishingBobber) {
            TexturePackManager.getInstance().setPackEnabled(NO_FISHING_BOBBER_PACK_ID, true);
            anyEnabled = true;
        }
        return anyEnabled;
    }

    @Override
    protected void init() {
        ConfigManager cfg = ConfigManager.getInstance();
        registerTexturePacks();
        noFogAtmospheric = cfg.getBoolean("visuals.no_fog_atmospheric", true);
        noFogWater = cfg.getBoolean("visuals.no_fog_water", true);
        noFogLava = cfg.getBoolean("visuals.no_fog_lava", true);
        lowFire = cfg.getBoolean("visuals.low_fire", true);
        shieldSideLow = cfg.getBoolean("visuals.shield_side_low", true);
        smallTotemPop = cfg.getBoolean("visuals.small_totem_pop", true);
        bowCrossbowIndicator = cfg.getBoolean("visuals.bow_crossbow_indicator", true);
        noFoodParticles = cfg.getBoolean("visuals.no_food_particles", true);
        noPumpkinBlur = cfg.getBoolean("visuals.no_pumpkin_blur", true);
        noFishingBobber = cfg.getBoolean("visuals.no_fishing_bobber", true);
    }

    public static void setNoFogAtmospheric(boolean v) { noFogAtmospheric = v; ConfigManager.getInstance().set("visuals.no_fog_atmospheric", v); }
    public static void setNoFogWater(boolean v) { noFogWater = v; ConfigManager.getInstance().set("visuals.no_fog_water", v); }
    public static void setNoFogLava(boolean v) { noFogLava = v; ConfigManager.getInstance().set("visuals.no_fog_lava", v); }
    public static void setLowFire(boolean v) {
        if (lowFire == v) return;
        lowFire = v;
        ConfigManager.getInstance().set("visuals.low_fire", v);
        TexturePackManager.getInstance().setPackEnabled(LOW_FIRE_PACK_ID, v);
        TexturePackManager.getInstance().reload();
    }
    public static void setShieldSideLow(boolean v) {
        if (shieldSideLow == v) return;
        shieldSideLow = v;
        ConfigManager.getInstance().set("visuals.shield_side_low", v);
        TexturePackManager.getInstance().setPackEnabled(SHIELD_PACK_ID, v);
        TexturePackManager.getInstance().reload();
    }
    public static void setSmallTotemPop(boolean v) {
        if (smallTotemPop == v) return;
        smallTotemPop = v;
        ConfigManager.getInstance().set("visuals.small_totem_pop", v);
        TexturePackManager.getInstance().setPackEnabled(SMALL_TOTEM_POP_PACK_ID, v);
        TexturePackManager.getInstance().reload();
    }
    public static void setBowCrossbowIndicator(boolean v) { bowCrossbowIndicator = v; ConfigManager.getInstance().set("visuals.bow_crossbow_indicator", v); }
    public static void setNoFoodParticles(boolean v) { noFoodParticles = v; ConfigManager.getInstance().set("visuals.no_food_particles", v); }
    public static void setNoPumpkinBlur(boolean v) { noPumpkinBlur = v; ConfigManager.getInstance().set("visuals.no_pumpkin_blur", v); }
    public static void setNoFishingBobber(boolean v) {
        if (noFishingBobber == v) return;
        noFishingBobber = v;
        ConfigManager.getInstance().set("visuals.no_fishing_bobber", v);
        TexturePackManager.getInstance().setPackEnabled(NO_FISHING_BOBBER_PACK_ID, v);
        TexturePackManager.getInstance().reload();
    }
}
