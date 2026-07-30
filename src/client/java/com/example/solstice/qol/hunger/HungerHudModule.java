package com.example.solstice.qol.hunger;

import com.example.solstice.core.config.ConfigManager;
import com.example.solstice.core.module.AbstractModule;
import com.example.solstice.core.module.ModuleCategory;
import com.example.solstice.core.module.ModuleSetting;
import com.example.solstice.mixin.hunger.HungerManagerAccessor;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.world.Difficulty;

import java.util.List;

/**
 * HungerHudModule - overlays saturation (and optionally exhaustion) directly
 * on the vanilla hunger bar, matching real AppleSkin's actual look, plus a
 * preview of the saturation/hunger/health you'd gain from the food you're
 * holding.
 *
 * <p>Inspired by AppleSkin, re-implemented natively: the actual drawing is
 * driven by {@code InGameHudFoodMixin} (HEAD/RETURN injections into
 * {@code InGameHud.renderFood}/{@code renderHealthBar}), using the same
 * public-domain icon sheet AppleSkin itself ships for saturation/exhaustion
 * (copied into Solstice's own assets - a texture asset, not source code) and
 * vanilla's own food/heart sprites for the held-food preview, matching the
 * real mod's own source exactly.</p>
 *
 * <p>Two disclosed simplifications versus the real mod: the natural-regen
 * health estimate below doesn't check a synced {@code naturalRegeneration}
 * gamerule flag (assumed true, the vanilla default) and doesn't add extra
 * health for foods that grant a Regeneration status effect (golden apples
 * etc.) - both are real behavior the original mod has, just narrower edge
 * cases than the core saturation/hunger preview.</p>
 */
public final class HungerHudModule extends AbstractModule {

    private static final HungerHudModule INSTANCE = new HungerHudModule();

    private static final Identifier ICONS = Identifier.of("solstice", "textures/icons.png");
    private static final Identifier FOOD_FULL = Identifier.ofVanilla("hud/food_full");
    private static final Identifier FOOD_HALF = Identifier.ofVanilla("hud/food_half");
    private static final Identifier FOOD_EMPTY = Identifier.ofVanilla("hud/food_empty");
    private static final Identifier HEART_FULL = Identifier.ofVanilla("hud/heart/full");
    private static final Identifier HEART_HALF = Identifier.ofVanilla("hud/heart/half");
    private static final Identifier HEART_CONTAINER = Identifier.ofVanilla("hud/heart/container");

    private static final int ICON_SIZE = 9;
    private static final int ICON_STRIDE = 8;
    private static final int EXHAUSTION_BAR_MAX_WIDTH = 81;
    private static final float MAX_EXHAUSTION = 4.0f;
    private static final float REGEN_EXHAUSTION_INCREMENT = 6.0f;

    public static boolean showExhaustion = false;

    private HungerHudModule() {}

    public static HungerHudModule getInstance() { return INSTANCE; }

    @Override public String getId()          { return "hunger_hud"; }
    @Override public String getDisplayName() { return "Hunger & Saturation HUD"; }
    @Override public String getDescription() { return "Overlays your hidden saturation (and optionally exhaustion) directly on the hunger bar, and previews what held food would restore, AppleSkin-style."; }

    @Override
    public java.util.List<String> getSearchKeywords() {
        return java.util.List.of("appleskin", "food", "saturation", "hunger bar", "exhaustion");
    }
    @Override public ModuleCategory getCategory() { return ModuleCategory.QUALITY_OF_LIFE; }

    @Override
    protected void init() {
        showExhaustion = ConfigManager.getInstance().getBoolean("hunger_hud.show_exhaustion", false);
    }

    @Override
    public List<ModuleSetting> getSettings() {
        return List.of(
                new ModuleSetting.BooleanSetting(
                        "Show Exhaustion",
                        "Exhaustion is a hidden meter that fills as you move and act. Once it's full, it drains your saturation "
                                + "(and eventually your hunger). Most players can safely ignore this - it's shown for reference.",
                        () -> showExhaustion,
                        v -> { showExhaustion = v; ConfigManager.getInstance().set("hunger_hud.show_exhaustion", v); })
        );
    }

    /** Called from {@code InGameHudFoodMixin} at HEAD of renderFood - draws behind vanilla's icons. */
    public void renderExhaustionBar(DrawContext context, PlayerEntity player, int top, int right) {
        if (!isEnabled() || !showExhaustion) return;

        float exhaustion = ((HungerManagerAccessor) player.getHungerManager()).solstice$getExhaustion();
        float ratio = Math.min(1f, Math.max(0f, exhaustion / MAX_EXHAUSTION));
        int width = (int) (ratio * EXHAUSTION_BAR_MAX_WIDTH);
        if (width <= 0) return;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, ICONS,
                right - width, top, (float) (EXHAUSTION_BAR_MAX_WIDTH - width), 18f, width, ICON_SIZE, 256, 256, 0xC0FFFFFF);
    }

    /** Called from {@code InGameHudFoodMixin} at RETURN of renderFood - draws on top of vanilla's icons. */
    public void renderSaturationGlint(DrawContext context, PlayerEntity player, int top, int right) {
        if (!isEnabled()) return;

        HungerManager hunger = player.getHungerManager();
        float saturation = hunger.getSaturationLevel();
        drawSaturationBars(context, right, top, 0f, saturation, 0xFFFFFFFF);

        FoodComponent food = getHeldFood(player);
        if (food == null) return;

        float alpha = pulseAlpha();
        drawSaturationBars(context, right, top, food.saturation(), saturation, alphaTint(alpha));
        renderHungerPreview(context, right, top, hunger.getFoodLevel(), food.nutrition(), alpha);
    }

    /** Called from {@code InGameHudFoodMixin} at RETURN of renderHealthBar - draws the estimated-health preview. */
    public void renderHealthPreview(DrawContext context, PlayerEntity player, int left, int top) {
        if (!isEnabled()) return;

        FoodComponent food = getHeldFood(player);
        if (food == null) return;
        if (!shouldShowEstimatedHealth(player)) return;

        float healthIncrement = getEstimatedHealthIncrement(player, food);
        float currentHealth = player.getHealth();
        float modifiedHealth = Math.min(currentHealth + healthIncrement, player.getMaxHealth());
        if (modifiedHealth <= currentHealth) return;

        float alpha = pulseAlpha();
        int tint = alphaTint(alpha);
        int bgTint = alphaTint(alpha * 0.25f);

        int fixedModifiedHealth = (int) Math.ceil(modifiedHealth);
        int startBars = (int) Math.max(0, Math.ceil(currentHealth) / 2f);
        int endBars = (int) Math.max(0, Math.ceil(modifiedHealth / 2f));
        for (int i = startBars; i < endBars; i++) {
            int x = left + i * ICON_STRIDE;
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HEART_CONTAINER, x, top, ICON_SIZE, ICON_SIZE, bgTint);
            boolean isHalf = i * 2 + 1 == fixedModifiedHealth;
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, isHalf ? HEART_HALF : HEART_FULL, x, top, ICON_SIZE, ICON_SIZE, tint);
        }
    }

    private void drawSaturationBars(DrawContext context, int right, int top, float saturationGained, float saturationLevel, int tint) {
        if (saturationLevel + saturationGained < 0) return;
        float modified = Math.max(0f, Math.min(saturationLevel + saturationGained, 20f));
        int startBar = saturationGained != 0 ? Math.max(0, (int) (saturationLevel / 2f)) : 0;
        int endBar = (int) Math.ceil(modified / 2f);
        for (int i = startBar; i < endBar; i++) {
            int x = right - i * ICON_STRIDE - 9;
            float effective = (modified / 2f) - i;
            int u = effective >= 1f ? 27 : effective > 0.5f ? 18 : effective > 0.25f ? 9 : 0;
            context.drawTexture(RenderPipelines.GUI_TEXTURED, ICONS, x, top, (float) u, 0f, ICON_SIZE, ICON_SIZE, 256, 256, tint);
        }
    }

    private void renderHungerPreview(DrawContext context, int right, int top, int foodLevel, int hungerGained, float alpha) {
        if (hungerGained <= 0) return;
        int modifiedFood = Math.max(0, Math.min(20, foodLevel + hungerGained));
        int startBars = Math.max(0, foodLevel / 2);
        int endBars = (int) Math.ceil(modifiedFood / 2f);
        int tint = alphaTint(alpha);
        int bgTint = alphaTint(alpha * 0.25f);
        for (int i = startBars; i < endBars; i++) {
            int x = right - i * ICON_STRIDE - 9;
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, FOOD_EMPTY, x, top, ICON_SIZE, ICON_SIZE, bgTint);
            boolean isHalf = i * 2 + 1 == modifiedFood;
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, isHalf ? FOOD_HALF : FOOD_FULL, x, top, ICON_SIZE, ICON_SIZE, tint);
        }
    }

    private FoodComponent getHeldFood(PlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        if (!stack.contains(DataComponentTypes.FOOD) || !stack.contains(DataComponentTypes.CONSUMABLE)) return null;
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        if (food == null || !player.canConsume(food.canAlwaysEat())) return null;
        return food;
    }

    private boolean shouldShowEstimatedHealth(PlayerEntity player) {
        if (player.getHungerManager().getFoodLevel() >= 18) return false;
        if (player.getEntityWorld().getDifficulty() == Difficulty.PEACEFUL) return false;
        if (player.hasStatusEffect(StatusEffects.POISON)) return false;
        if (player.hasStatusEffect(StatusEffects.WITHER)) return false;
        if (player.hasStatusEffect(StatusEffects.REGENERATION)) return false;
        return true;
    }

    /** Ported from AppleSkin's FoodHelper.getEstimatedHealthIncrement - the natural-regen-from-food-saturation path only. */
    private float getEstimatedHealthIncrement(PlayerEntity player, FoodComponent food) {
        if (!player.canFoodHeal()) return 0f;

        HungerManager hunger = player.getHungerManager();
        int foodLevel = Math.min(hunger.getFoodLevel() + food.nutrition(), 20);
        if (foodLevel < 18) return 0f;

        float saturationLevel = Math.min(hunger.getSaturationLevel() + food.saturation(), (float) foodLevel);
        float exhaustionLevel = ((HungerManagerAccessor) hunger).solstice$getExhaustion();
        return simulateRegenHealth(foodLevel, saturationLevel, exhaustionLevel);
    }

    /**
     * Ported as-is from AppleSkin's FoodHelper - the shortcut here isn't optional
     * polish: a naive loop can pathologically balloon in iteration count for
     * small saturation values, which is exactly why the original mod computes
     * the number of iterations directly instead of looping through each one.
     */
    private float simulateRegenHealth(int foodLevel, float saturationLevel, float exhaustionLevel) {
        float health = 0f;
        if (!Float.isFinite(exhaustionLevel) || !Float.isFinite(saturationLevel)) return 0f;

        while (foodLevel >= 18) {
            while (exhaustionLevel > MAX_EXHAUSTION) {
                exhaustionLevel -= MAX_EXHAUSTION;
                if (saturationLevel > 0) {
                    saturationLevel = Math.max(saturationLevel - 1, 0);
                } else {
                    foodLevel -= 1;
                }
            }
            if (foodLevel >= 20 && Float.compare(saturationLevel, Float.MIN_NORMAL) > 0) {
                float limitedSaturationLevel = Math.min(saturationLevel, REGEN_EXHAUSTION_INCREMENT);
                float exhaustionUntilAboveMax = Math.nextUp(MAX_EXHAUSTION) - exhaustionLevel;
                int numIterationsUntilAboveMax = Math.max(1, (int) Math.ceil(exhaustionUntilAboveMax / limitedSaturationLevel));
                health += (limitedSaturationLevel / REGEN_EXHAUSTION_INCREMENT) * numIterationsUntilAboveMax;
                exhaustionLevel += limitedSaturationLevel * numIterationsUntilAboveMax;
            } else if (foodLevel >= 18) {
                health += 1;
                exhaustionLevel += REGEN_EXHAUSTION_INCREMENT;
            }
        }
        return health;
    }

    private float pulseAlpha() {
        return 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 300.0);
    }

    private static int alphaTint(float alpha) {
        int a = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f);
        return (a << 24) | 0xFFFFFF;
    }
}
