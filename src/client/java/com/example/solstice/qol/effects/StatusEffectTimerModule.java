package com.example.solstice.qol.effects;

import com.example.solstice.core.config.ConfigManager;
import com.example.solstice.core.module.AbstractModule;
import com.example.solstice.core.module.ModuleCategory;
import com.example.solstice.core.module.ModuleSetting;
import com.google.common.collect.Ordering;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.Collection;
import java.util.List;

/**
 * Draws the remaining duration on top of each status-effect icon vanilla
 * already renders. Not a {@link com.example.solstice.core.hud.HudElement} -
 * there's nowhere meaningful to reposition this to independent of the
 * icons themselves, so it just draws numbers at the exact same positions
 * vanilla computes.
 *
 * <p>Deliberately not a Mixin - everything needed ({@code
 * StatusEffectInstance.shouldShowIcon/getEffectType/getDuration}, {@code
 * StatusEffect.isBeneficial}, {@code MinecraftClient.isDemo}) is public, so
 * this independently re-walks the same sorted collection and replicates
 * {@code InGameHud.renderStatusEffectOverlay}'s own icon-position formula
 * (confirmed via decompile: beneficial icons stack right-to-left along one
 * row, harmful icons along a second row 26px below, each 25px apart) rather
 * than injecting into vanilla's own rendering at all.</p>
 */
public final class StatusEffectTimerModule extends AbstractModule {

    private static final StatusEffectTimerModule INSTANCE = new StatusEffectTimerModule();

    private static final int DEFAULT_COLOR = 0xFFFFFF;
    private static final int DEFAULT_WARN_COLOR = 0xFF5555;
    private static final int DEFAULT_WARN_SECONDS = 5;

    /** Packed 0xRRGGBB, edited via a real color picker. */
    public static int textColor = DEFAULT_COLOR;
    /** Color used once an effect's remaining time drops below {@link #warnSeconds}. */
    public static int warnColor = DEFAULT_WARN_COLOR;
    /** 0 disables the warn-color switch entirely. */
    public static int warnSeconds = DEFAULT_WARN_SECONDS;

    private StatusEffectTimerModule() {}

    public static StatusEffectTimerModule getInstance() { return INSTANCE; }

    @Override public String getId()          { return "status_effect_timer"; }
    @Override public String getDisplayName() { return "Status Effect Timer"; }
    @Override public String getDescription() { return "Shows the remaining duration on top of each active potion effect icon."; }
    @Override public ModuleCategory getCategory() { return ModuleCategory.QUALITY_OF_LIFE; }

    @Override
    public List<String> getSearchKeywords() {
        return List.of("potion timer", "effect duration", "buff timer", "duration overlay");
    }

    @Override protected boolean defaultEnabled() { return false; }

    @Override
    public List<ModuleSetting> getSettings() {
        return List.of(
                new ModuleSetting.ColorSetting("Text Color",
                        "Color of the duration text on each effect icon.",
                        () -> textColor,
                        v -> { textColor = v; ConfigManager.getInstance().set("status_effect_timer.text_color", v); }),
                new ModuleSetting.ColorSetting("Low Duration Color",
                        "Color the text switches to once an effect is about to run out.",
                        () -> warnColor,
                        v -> { warnColor = v; ConfigManager.getInstance().set("status_effect_timer.warn_color", v); }),
                new ModuleSetting.IntSetting("Low Duration Threshold (Seconds)",
                        "How many seconds of remaining duration count as \"about to run out\". Set to 0 to never switch color.",
                        0, 30,
                        () -> warnSeconds,
                        v -> { warnSeconds = v; ConfigManager.getInstance().set("status_effect_timer.warn_seconds", v); })
        );
    }

    @Override
    protected void init() {
        textColor = ConfigManager.getInstance().getInt("status_effect_timer.text_color", DEFAULT_COLOR);
        warnColor = ConfigManager.getInstance().getInt("status_effect_timer.warn_color", DEFAULT_WARN_COLOR);
        warnSeconds = ConfigManager.getInstance().getInt("status_effect_timer.warn_seconds", DEFAULT_WARN_SECONDS);
    }

    /** Called from {@link com.example.solstice.SolsticeClient} via {@code HudRenderCallback.EVENT}. */
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!isEnabled() || client.player == null || client.currentScreen != null) {
            return;
        }

        Collection<StatusEffectInstance> effects = client.player.getStatusEffects();
        if (effects.isEmpty()) {
            return;
        }

        int beneficialCount = 0;
        int harmfulCount = 0;
        for (StatusEffectInstance instance : Ordering.natural().reverse().sortedCopy(effects)) {
            if (!instance.shouldShowIcon()) {
                continue;
            }

            RegistryEntry<StatusEffect> type = instance.getEffectType();
            int x = context.getScaledWindowWidth();
            int y = 1;
            if (client.isDemo()) {
                y += 15;
            }

            if (type.value().isBeneficial()) {
                beneficialCount++;
                x -= 25 * beneficialCount;
            } else {
                harmfulCount++;
                x -= 25 * harmfulCount;
                y += 26;
            }

            String label = formatDuration(instance.getDuration());
            int textWidth = client.textRenderer.getWidth(label);
            int textX = x + 12 - textWidth / 2;
            int textY = y + 15;
            int seconds = Math.max(0, instance.getDuration()) / 20;
            boolean warn = warnSeconds > 0 && seconds < warnSeconds;
            int color = 0xFF000000 | (warn ? warnColor : textColor);
            context.drawText(client.textRenderer, label, textX, textY, color, true);
        }
    }

    private static String formatDuration(int ticks) {
        int seconds = Math.max(0, ticks) / 20;
        if (seconds >= 60) {
            int minutes = seconds / 60;
            int remSeconds = seconds % 60;
            return minutes + ":" + (remSeconds < 10 ? "0" + remSeconds : String.valueOf(remSeconds));
        }
        return seconds + "s";
    }
}
