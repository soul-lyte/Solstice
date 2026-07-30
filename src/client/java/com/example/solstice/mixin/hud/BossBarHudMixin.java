package com.example.solstice.mixin.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels vanilla's own fixed, screen-centered boss bar layout - Solstice's
 * {@code BossBarHudElement} draws the same real bars (via
 * {@link BossBarHudAccessor}) at a repositionable location instead, the
 * same way {@code FpsWidget}/{@code RamWidget} already work. Unconditional:
 * if the user hides the Boss Bar element in Solstice's HUD editor, it
 * simply doesn't draw at all, matching how hiding FPS/RAM already behaves.
 */
@Mixin(BossBarHud.class)
public abstract class BossBarHudMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void solstice$cancelVanillaLayout(DrawContext context, CallbackInfo ci) {
        ci.cancel();
    }
}
