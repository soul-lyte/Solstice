package com.example.solstice.qol.shulker;

import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;

import java.util.List;

/**
 * Marker data for a container item's (shulker box, etc.) tooltip - carries
 * the real contents so {@link ShulkerTooltipComponent} can draw a real
 * icon grid instead of vanilla's own plain-text "and N more" listing. See
 * {@code ItemTooltipDataMixin} (attaches this) and {@code
 * TooltipComponentMixin} (maps it to {@link ShulkerTooltipComponent}).
 *
 * <p>{@code compact} mirrors MisterPeModder/ShulkerBoxTooltip's own
 * Compact/Full preview distinction - see {@code ShulkerTooltipDataMixin}
 * for which held key selects which mode.</p>
 *
 * <p>{@code totalSlots} is NOT always {@code contents.size()} - confirmed
 * via decompile that {@code ContainerComponent}'s own stored list is
 * trimmed to the last non-empty slot ({@code findLastNonEmptyIndex}), not
 * padded to the container's real capacity. For a real shulker box this is
 * hardcoded to 27 (its real, stable slot count) so Full mode always shows
 * every slot including trailing empties, matching the original mod's own
 * {@code provider.getInventoryMaxSize()} technique.</p>
 *
 * <p>{@code backgroundColor} is the shulker box's own dye-derived color
 * (opaque ARGB), or {@code -1} (opaque white, no tint) for non-shulker
 * containers - see {@code ShulkerTooltipDataMixin} for how it's derived.</p>
 */
public record ShulkerTooltipData(List<ItemStack> contents, boolean compact, int totalSlots, int backgroundColor)
        implements TooltipData {
}
