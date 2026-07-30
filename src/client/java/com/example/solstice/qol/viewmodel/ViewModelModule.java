package com.example.solstice.qol.viewmodel;

import com.example.solstice.core.config.ConfigManager;
import com.example.solstice.core.module.AbstractModule;
import com.example.solstice.core.module.ModuleCategory;
import com.example.solstice.ui.EditableModule;
import com.example.solstice.ui.ViewModelEditScreen;
import net.minecraft.client.gui.screen.Screen;

/**
 * Standalone first-person held-item scale/offset - split out of the Visuals
 * bundle into its own module/card. Applies to the held ITEM only, not the
 * player's arm/hand model (see {@code HeldItemRendererMixin}, which wraps
 * only {@code HeldItemRenderer}'s item-model render call, not the whole
 * first-person render pass) - and never touches a held shield, since the
 * shield's own position comes from a bundled resource pack that's meant to
 * be the final word regardless of these settings (see {@code VisualsModule.SHIELD_PACK_ID}).
 */
public final class ViewModelModule extends AbstractModule implements EditableModule {

    private static final ViewModelModule INSTANCE = new ViewModelModule();

    public static final double DEFAULT_SCALE = 1.0;
    public static final double DEFAULT_OFFSET = 0.0;

    /** 1.0 = vanilla size. */
    public static double scale = DEFAULT_SCALE;
    public static double offsetX = DEFAULT_OFFSET;
    public static double offsetY = DEFAULT_OFFSET;
    public static double offsetZ = DEFAULT_OFFSET;

    private ViewModelModule() {}

    public static ViewModelModule getInstance() { return INSTANCE; }

    @Override public String getId()          { return "view_model"; }
    @Override public String getDisplayName() { return "View Model Customization"; }
    @Override public String getDescription() { return "Resizes and repositions whatever item you're holding in first person."; }

    @Override
    public java.util.List<String> getSearchKeywords() {
        return java.util.List.of("hand scale", "fov hand", "item scale", "viewmodel", "held item size");
    }
    @Override public ModuleCategory getCategory() { return ModuleCategory.QUALITY_OF_LIFE; }

    @Override
    public Screen createEditScreen(Screen parent) {
        return new ViewModelEditScreen(parent);
    }

    @Override
    protected void init() {
        ConfigManager cfg = ConfigManager.getInstance();
        scale = cfg.getDouble("view_model.scale", DEFAULT_SCALE);
        offsetX = cfg.getDouble("view_model.offset_x", DEFAULT_OFFSET);
        offsetY = cfg.getDouble("view_model.offset_y", DEFAULT_OFFSET);
        offsetZ = cfg.getDouble("view_model.offset_z", DEFAULT_OFFSET);
    }

    public static void setScale(double v) { scale = v; ConfigManager.getInstance().set("view_model.scale", v); }
    public static void setOffsetX(double v) { offsetX = v; ConfigManager.getInstance().set("view_model.offset_x", v); }
    public static void setOffsetY(double v) { offsetY = v; ConfigManager.getInstance().set("view_model.offset_y", v); }
    public static void setOffsetZ(double v) { offsetZ = v; ConfigManager.getInstance().set("view_model.offset_z", v); }

    public static void reset() {
        setScale(DEFAULT_SCALE);
        setOffsetX(DEFAULT_OFFSET);
        setOffsetY(DEFAULT_OFFSET);
        setOffsetZ(DEFAULT_OFFSET);
    }
}
