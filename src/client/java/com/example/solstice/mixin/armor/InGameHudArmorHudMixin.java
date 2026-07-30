package com.example.solstice.mixin.armor;

import com.example.solstice.ui.ArmorHudElement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws Armor HUD right after vanilla's own hotbar - the same real hook
 * point uku3lig/armor-hud itself uses ({@code Hud.extractItemHotbar}'s
 * TAIL, this project's equivalent is {@code InGameHud.renderHotbar}).
 * Purely additive, nothing vanilla is cancelled - matches the reference
 * mod's own behavior of drawing alongside the hotbar, not replacing it.
 */
@Mixin(InGameHud.class)
public abstract class InGameHudArmorHudMixin {

    @Inject(method = "renderHotbar", at = @At("TAIL"))
    private void solstice$renderArmorHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        ArmorHudElement.getInstance().render(context, tickCounter);
    }
}
