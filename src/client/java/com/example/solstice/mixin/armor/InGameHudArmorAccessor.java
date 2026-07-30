package com.example.solstice.mixin.armor;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes {@link InGameHud}'s private per-slot hotbar item draw (icon +
 * count + durability bar, including the pickup "pop" animation) so Armor
 * HUD can draw armor pieces with the exact same real vanilla slot
 * rendering uku3lig/armor-hud itself reuses, instead of reimplementing
 * item icon/durability rendering from scratch.
 */
@Mixin(InGameHud.class)
public interface InGameHudArmorAccessor {

    @Invoker("renderHotbarItem")
    void solstice$renderHotbarItem(DrawContext context, int x, int y, RenderTickCounter tickCounter,
                                    PlayerEntity player, ItemStack stack, int seed);
}
