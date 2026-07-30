package com.example.solstice.qol.hunger;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.util.Identifier;

/**
 * Renders a food item's tooltip as real hunger drumstick icons and a
 * saturation bar - the same sprites/icon sheet {@code HungerHudModule}'s HUD
 * overlay already uses - instead of plain "Hunger: +3 / Saturation: +3.0"
 * text lines.
 */
public class FoodValueTooltipComponent implements TooltipComponent {

    private static final Identifier ICONS = Identifier.of("solstice", "textures/icons.png");
    private static final Identifier FOOD_FULL = Identifier.ofVanilla("hud/food_full");
    private static final Identifier FOOD_HALF = Identifier.ofVanilla("hud/food_half");

    private static final int ICON_SIZE = 9;
    private static final int ICON_STRIDE = 8;
    private static final int ROW_GAP = 2;
    private static final int MAX_ICONS = 10;

    private final int nutrition;
    private final float saturation;

    public FoodValueTooltipComponent(FoodValueTooltipData data) {
        this.nutrition = data.nutrition();
        this.saturation = data.saturation();
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        int nutritionIcons = Math.min(MAX_ICONS, (int) Math.ceil(nutrition / 2f));
        int saturationIcons = Math.min(MAX_ICONS, (int) Math.ceil(saturation / 2f));
        int icons = Math.max(1, Math.max(nutritionIcons, saturationIcons));
        return icons * ICON_STRIDE + 1;
    }

    @Override
    public int getHeight(TextRenderer textRenderer) {
        return saturation > 0 ? ICON_SIZE * 2 + ROW_GAP : ICON_SIZE;
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
        drawFoodRow(context, x, y);
        if (saturation > 0) {
            drawSaturationRow(context, x, y + ICON_SIZE + ROW_GAP);
        }
    }

    private void drawFoodRow(DrawContext context, int x, int y) {
        int icons = Math.min(MAX_ICONS, (int) Math.ceil(nutrition / 2f));
        for (int i = 0; i < icons; i++) {
            boolean isHalf = i * 2 + 1 == nutrition;
            Identifier icon = isHalf ? FOOD_HALF : FOOD_FULL;
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, icon, x + i * ICON_STRIDE, y, ICON_SIZE, ICON_SIZE, 0xFFFFFFFF);
        }
    }

    /** Same icon sheet/U-tile scheme as {@code HungerHudModule.drawSaturationBars}. */
    private void drawSaturationRow(DrawContext context, int x, int y) {
        int icons = Math.min(MAX_ICONS, (int) Math.ceil(saturation / 2f));
        for (int i = 0; i < icons; i++) {
            float effective = (saturation / 2f) - i;
            int u = effective >= 1f ? 27 : effective > 0.5f ? 18 : effective > 0.25f ? 9 : 0;
            context.drawTexture(RenderPipelines.GUI_TEXTURED, ICONS,
                    x + i * ICON_STRIDE, y, (float) u, 0f, ICON_SIZE, ICON_SIZE, 256, 256, 0xFFFFFFFF);
        }
    }
}
