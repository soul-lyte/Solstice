package com.example.solstice.qol.effects;

import com.example.solstice.core.module.AbstractModule;
import com.example.solstice.core.module.ModuleCategory;

import java.util.List;

/**
 * Forces the lightmap's gamma input to a very high value while enabled,
 * washing out the brightness curve so everything renders fully lit
 * regardless of actual light level - the same technique OptiFine/Sodium's
 * own "Fullbright" options use. See {@code mixin.effects.FullbrightMixin}
 * for the actual override (a {@code SimpleOption.getValue()} redirect
 * matched by the option instance's own identity, not by guessing a local
 * variable slot - {@code LightmapTextureManager.update(float)}'s real body
 * has 9 other float locals before the gamma one, confirmed via decompile,
 * which would have made ordinal-based local-variable matching fragile).
 * Never touches the real, persisted Video Settings gamma value itself -
 * only what reaches the render uniform while this module is on.
 */
public final class FullbrightModule extends AbstractModule {

    private static final FullbrightModule INSTANCE = new FullbrightModule();

    private FullbrightModule() {}

    public static FullbrightModule getInstance() { return INSTANCE; }

    @Override public String getId()          { return "fullbright"; }
    @Override public String getDisplayName() { return "Fullbright"; }
    @Override public String getDescription() { return "Forces full brightness everywhere, ignoring the actual light level."; }
    @Override public ModuleCategory getCategory() { return ModuleCategory.QUALITY_OF_LIFE; }

    @Override
    public List<String> getSearchKeywords() {
        return List.of("night vision", "brightness", "gamma", "full bright", "see in the dark");
    }

    @Override protected boolean defaultEnabled() { return false; }

    @Override
    protected void init() {
        // No state to wire up - FullbrightMixin reads isEnabled() directly every frame.
    }
}
