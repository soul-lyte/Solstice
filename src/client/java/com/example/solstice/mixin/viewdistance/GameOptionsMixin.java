package com.example.solstice.mixin.viewdistance;

import com.example.solstice.viewdistance.ViewDistanceModule;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ported from Johni0702/bobby (LGPL-3.0-only, see NOTICE.md) - the single
 * patch point that makes the whole rest of this feature work without ever
 * needing to separately fix fog, the camera's far-clip plane, or {@code
 * BuiltChunkStorage}'s array sizing (all three, plus the render-storage
 * inflation itself, were each individually patched in an earlier version of
 * this module and still weren't sufficient together - see git history).
 * {@code getClampedViewDistance()} is the one method every one of those
 * consumers reads its distance from; when enabled, this makes it return the
 * client's own raw render-distance option instead of clamping it down to
 * whatever the server's own view distance is - so raising your Render
 * Distance slider (already uncapped past 32 by {@code
 * ViewDistanceUncapMixin}) directly becomes the "how far should retained
 * chunks extend" control, with fog/far-plane/storage all automatically
 * agreeing with zero extra Mixins.
 *
 * <p>Does not also widen the slider's own max value - {@code
 * ViewDistanceUncapMixin} already does that unconditionally, and Bobby's own
 * equivalent piece would just get immediately overwritten by it if both ran,
 * so only this half of Bobby's original Mixin is needed here.</p>
 */
@Mixin(GameOptions.class)
public abstract class GameOptionsMixin {
    @Shadow @Final private SimpleOption<Integer> viewDistance;

    @Inject(method = "getClampedViewDistance", at = @At("HEAD"), cancellable = true)
    private void solstice$forceClientDistanceWhenEnabled(CallbackInfoReturnable<Integer> ci) {
        if (ViewDistanceModule.getInstance().isEnabled()) {
            ci.setReturnValue(this.viewDistance.getValue());
        }
    }
}
