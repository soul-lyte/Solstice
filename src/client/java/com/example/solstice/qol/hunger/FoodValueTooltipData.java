package com.example.solstice.qol.hunger;

import net.minecraft.item.tooltip.TooltipData;

/**
 * Marker data for a food item's tooltip - carries just the two numbers the
 * real hunger/saturation bar icons are drawn from. See
 * {@code ItemTooltipDataMixin} (attaches this to food items) and
 * {@code TooltipComponentMixin} (maps it to {@link FoodValueTooltipComponent}).
 */
public record FoodValueTooltipData(int nutrition, float saturation) implements TooltipData {
}
