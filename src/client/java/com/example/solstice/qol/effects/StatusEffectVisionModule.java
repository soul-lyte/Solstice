package com.example.solstice.qol.effects;

import com.example.solstice.core.config.ConfigManager;
import com.example.solstice.core.module.AbstractModule;
import com.example.solstice.core.module.ModuleCategory;
import com.example.solstice.core.module.ModuleSetting;

import java.util.List;

/**
 * Clears the vision-obscuring parts of Blindness, Darkness, and Nausea -
 * split out of {@code VisualsModule}'s old "No Fog > Status Effects" toggle
 * into its own module, and widened to cover more than just fog.
 *
 * <p>Two separate real mechanisms, confirmed by decompiling the actual
 * classes rather than assumed:</p>
 * <ul>
 *   <li>Blindness and Darkness have no vision effect beyond fog in 1.21.11 -
 *   confirmed by decompiling {@code GameRenderer} (no reference to either
 *   status effect anywhere in it) and both {@code BlindnessEffectFogModifier}/
 *   {@code DarknessEffectFogModifier} (only {@code applyStartEndModifier} and
 *   the void-darkness {@code applyDarknessModifier} - no other rendering
 *   hook). {@code mixin.effects.StatusEffectFogModifierMixin} (moved from
 *   {@code mixin.visuals}, otherwise unchanged) already fully covers this by
 *   cancelling {@code StatusEffectFogModifier.shouldApply()} - safe to cancel
 *   outright unlike the water/lava crash this project already hit once,
 *   because {@code StatusEffectFogModifier.isColorSource()} overrides to
 *   {@code false}, so it was never a color-source candidate to begin with.</li>
 *   <li>Nausea's screen warp is a completely different mechanism - a
 *   projection-matrix rotate/squish in {@code GameRenderer}, driven by {@code
 *   LivingEntity.getEffectFadeFactor(StatusEffects.NAUSEA, tickDelta)}. New
 *   {@code mixin.effects.NauseaFadeFactorMixin} zeroes that specific query for
 *   the local player only. Deliberately does NOT touch {@code
 *   ClientPlayerEntity.nauseaIntensity}/{@code lastNauseaIntensity} - that's a
 *   separate field driving the nether-portal transition wobble, not the
 *   Nausea status effect, and disabling it wasn't asked for.</li>
 * </ul>
 */
public final class StatusEffectVisionModule extends AbstractModule {

    private static final StatusEffectVisionModule INSTANCE = new StatusEffectVisionModule();

    /** Whether Nausea's screen warp is also cleared - see {@code NauseaFadeFactorMixin}. */
    public static boolean clearNausea = true;

    private StatusEffectVisionModule() {}

    public static StatusEffectVisionModule getInstance() { return INSTANCE; }

    @Override public String getId()          { return "status_effect_vision"; }
    @Override public String getDisplayName() { return "No Blindness/Nausea"; }
    @Override public String getDescription() { return "Clears the vision-obscuring parts of Blindness, Darkness, and Nausea."; }

    @Override
    public java.util.List<String> getSearchKeywords() {
        return java.util.List.of("nausea", "blindness", "darkness", "no nausea", "screen warp", "potion vision");
    }
    @Override public ModuleCategory getCategory() { return ModuleCategory.QUALITY_OF_LIFE; }

    @Override
    public List<ModuleSetting> getSettings() {
        return List.of(
                new ModuleSetting.BooleanSetting(
                        "Also Clear Nausea",
                        "Clears Nausea's screen warp too, not just Blindness/Darkness fog.",
                        () -> clearNausea,
                        v -> { clearNausea = v; ConfigManager.getInstance().set("status_effect_vision.clear_nausea", v); })
        );
    }

    @Override
    protected void init() {
        clearNausea = ConfigManager.getInstance().getBoolean("status_effect_vision.clear_nausea", true);
    }
}
