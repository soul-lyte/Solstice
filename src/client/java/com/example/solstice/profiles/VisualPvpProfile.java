package com.example.solstice.profiles;

import com.example.solstice.core.module.Module;
import com.example.solstice.core.module.ModuleCategory;
import com.example.solstice.core.module.ModuleRegistry;
import com.example.solstice.qol.crosshair.CrosshairModule;

/**
 * Default visual profile: every QOL module on, Custom Texture crosshair.
 *
 * <p>Deliberately does not touch the Textures tab (GUI skin or anything
 * else there) at all, and {@link #matchesCurrentState()} never checks it
 * either - per explicit direction, a Visual profile's identity is about QOL
 * modules and the crosshair, not textures, so switching a texture never
 * makes this profile stop matching, and applying it never changes one.</p>
 */
public final class VisualPvpProfile implements Profile {

    @Override public String getName() { return "PVP"; }
    @Override public String getDescription() { return "Every Quality of Life module on, Custom Texture crosshair."; }
    @Override public ProfileCategory getCategory() { return ProfileCategory.VISUAL; }

    @Override
    public void apply() {
        for (Module module : ModuleRegistry.getInstance().getByCategory(ModuleCategory.QUALITY_OF_LIFE)) {
            if (!module.isAlwaysOn() && !module.excludeFromPvpProfile()) {
                module.setEnabled(true);
            }
        }
        CrosshairModule.setStyleIndex(CrosshairModule.STYLE_CUSTOM_TEXTURE);
    }

    @Override
    public boolean matchesCurrentState() {
        for (Module module : ModuleRegistry.getInstance().getByCategory(ModuleCategory.QUALITY_OF_LIFE)) {
            if (!module.isAlwaysOn() && !module.excludeFromPvpProfile() && !module.isEnabled()) {
                return false;
            }
        }
        return CrosshairModule.styleIndex == CrosshairModule.STYLE_CUSTOM_TEXTURE;
    }
}
