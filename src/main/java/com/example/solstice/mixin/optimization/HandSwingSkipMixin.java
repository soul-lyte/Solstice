package com.example.solstice.mixin.optimization;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adapted from Lithium's {@code entity/fast_hand_swing} mixin - CaffeineMC,
 * LGPL-3.0-only, see NOTICE.md and licenses/LICENSE-LGPL-3.0.txt.
 *
 * <p>Ported onto 1.21.11's real method/field names (confirmed via decompile,
 * different from Lithium's own Mojmap-based target - {@code updateSwingTime}/
 * {@code swinging}/{@code swingTime} there is {@code tickHandSwing}/{@code
 * handSwinging}/{@code handSwingTicks} here). Every tick, for every living
 * entity, {@code tickHandSwing()} unconditionally calls {@code
 * getHandSwingDuration()} before even checking whether a swing is in
 * progress - wasted for the common case (not swinging, no leftover ticks),
 * where the rest of the method is a no-op anyway (the else branch sets an
 * already-zero field to zero, and the final progress assignment computes
 * 0f/i = 0f, which is what it already was). Skipping the whole method in
 * that case is output-identical, confirmed by reading the real method body.</p>
 */
@Mixin(LivingEntity.class)
public abstract class HandSwingSkipMixin {

    @Shadow public boolean handSwinging;
    @Shadow public int handSwingTicks;

    @Inject(method = "tickHandSwing", at = @At("HEAD"), cancellable = true)
    private void solstice$skipWhenIdle(CallbackInfo ci) {
        if (!this.handSwinging && this.handSwingTicks == 0) {
            ci.cancel();
        }
    }
}
