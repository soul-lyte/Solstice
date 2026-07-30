package com.example.solstice.profiles;

import com.example.solstice.core.module.Module;
import com.example.solstice.core.module.ModuleCategory;
import com.example.solstice.core.module.ModuleRegistry;
import com.example.solstice.qol.crosshair.CrosshairModule;
import com.example.solstice.textures.TexturePackManager;
import com.example.solstice.textures.TextureSlots;

/** Default visual profile: every QOL module on, dark inventory GUI skin, Custom Texture crosshair. */
public final class VisualPvpProfile implements Profile {

    private static final int GUI_SKIN_DARK_INDEX = 1;

    @Override public String getName() { return "PVP"; }
    @Override public String getDescription() { return "Every Quality of Life module on, dark inventory GUI."; }
    @Override public ProfileCategory getCategory() { return ProfileCategory.VISUAL; }

    @Override
    public void apply() {
        for (Module module : ModuleRegistry.getInstance().getByCategory(ModuleCategory.QUALITY_OF_LIFE)) {
            if (!module.isAlwaysOn() && !module.excludeFromPvpProfile()) {
                module.setEnabled(true);
            }
        }
        CrosshairModule.setStyleIndex(CrosshairModule.STYLE_CUSTOM_TEXTURE);

        TexturePackManager.getInstance().applyIndex(TextureSlots.GUI_SKIN, GUI_SKIN_DARK_INDEX);
        TexturePackManager.getInstance().reload();
    }

    @Override
    public boolean matchesCurrentState() {
        for (Module module : ModuleRegistry.getInstance().getByCategory(ModuleCategory.QUALITY_OF_LIFE)) {
            if (!module.isAlwaysOn() && !module.excludeFromPvpProfile() && !module.isEnabled()) {
                return false;
            }
        }
        if (CrosshairModule.styleIndex != CrosshairModule.STYLE_CUSTOM_TEXTURE) {
            return false;
        }
        return TexturePackManager.getInstance().getSelectedIndex(TextureSlots.GUI_SKIN) == GUI_SKIN_DARK_INDEX;
    }
}
