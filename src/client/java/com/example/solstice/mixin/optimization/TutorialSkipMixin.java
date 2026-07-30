package com.example.solstice.mixin.optimization;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.tutorial.TutorialManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adapted from BadOptimizations' {@code MixinTutorial} - imthosea, MIT, see
 * NOTICE.md. Ported onto 1.21.11's real class/method names (confirmed via
 * javap - Yarn calls this {@code TutorialManager}, not {@code Tutorial}).
 * {@code TutorialManager.tick()} runs every client tick unconditionally,
 * but the tutorial system (the onboarding hints - "Press W to move" etc.)
 * only matters in Minecraft's Demo mode; for every normal install it's
 * pure wasted work every single tick. Skipping it outside demo mode is
 * output-identical for the 99.9% case this project's users are in.
 */
@Mixin(TutorialManager.class)
public abstract class TutorialSkipMixin {

    @Shadow @Final private MinecraftClient client;

    @Inject(method = "tick()V", at = @At("HEAD"), cancellable = true)
    private void solstice$skipOutsideDemo(CallbackInfo ci) {
        if (!this.client.isDemo()) {
            ci.cancel();
        }
    }
}
