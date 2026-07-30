package com.example.solstice.mixin.hunger;

import com.example.solstice.qol.hunger.HungerHudModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws {@link HungerHudModule}'s AppleSkin-style overlay directly on the
 * vanilla hunger bar: the exhaustion bar behind vanilla's icons (HEAD, before
 * they're drawn), the saturation glint and held-food hunger preview on top
 * of them (RETURN, after), and the held-food estimated-health preview on
 * top of the health bar (RETURN of renderHealthBar).
 */
@Mixin(InGameHud.class)
public abstract class InGameHudFoodMixin {

    @Inject(method = "renderFood", at = @At("HEAD"))
    private void solstice$preFood(DrawContext context, PlayerEntity player, int top, int right, CallbackInfo ci) {
        HungerHudModule.getInstance().renderExhaustionBar(context, player, top, right);
    }

    @Inject(method = "renderFood", at = @At("RETURN"))
    private void solstice$postFood(DrawContext context, PlayerEntity player, int top, int right, CallbackInfo ci) {
        HungerHudModule.getInstance().renderSaturationGlint(context, player, top, right);
    }

    @Inject(method = "renderHealthBar", at = @At("RETURN"))
    private void solstice$postHealth(DrawContext context, PlayerEntity player, int left, int top, int lines,
                                      int regeneratingHeartIndex, float maxHealth, int lastHealth, int health,
                                      int absorption, boolean blinking, CallbackInfo ci) {
        HungerHudModule.getInstance().renderHealthPreview(context, player, left, top);
    }
}
