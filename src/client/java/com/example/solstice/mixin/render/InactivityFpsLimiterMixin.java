package com.example.solstice.mixin.render;

import net.minecraft.client.option.InactivityFpsLimiter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla's {@code InactivityFpsLimiter} silently overrides the Max Framerate
 * setting to a hardcoded 60 whenever no world is loaded and a screen is open
 * (title screen, any pre-world menu - see {@code LimitReason.OUT_OF_LEVEL_MENU}
 * in the decompiled source). That makes the slider's real, configured value
 * (e.g. 120) look desynced from observed FPS even though nothing else is wrong -
 * confirmed by decompile, not guessed. Minimized-window and AFK throttling are
 * untouched here since they're a separate, deliberately-labeled vanilla feature.
 */
@Mixin(InactivityFpsLimiter.class)
public abstract class InactivityFpsLimiterMixin {

    @Shadow private int maxFps;

    @Shadow public abstract InactivityFpsLimiter.LimitReason getLimitReason();

    @Inject(method = "update", at = @At("RETURN"), cancellable = true)
    private void solstice$honorMaxFpsInMenus(CallbackInfoReturnable<Integer> cir) {
        if (getLimitReason() == InactivityFpsLimiter.LimitReason.OUT_OF_LEVEL_MENU) {
            cir.setReturnValue(maxFps);
        }
    }
}
