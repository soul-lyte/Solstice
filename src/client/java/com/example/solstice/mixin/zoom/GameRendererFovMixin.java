package com.example.solstice.mixin.zoom;

import com.example.solstice.qol.zoom.ZoomModule;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Divides the vanilla FOV by {@link ZoomModule}'s current smoothed zoom
 * factor. Separate from {@code GameRendererMixin} (same target class, but a
 * different method/concern) to keep each feature's Mixin self-contained.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererFovMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void solstice$onGetFov(Camera camera, float tickProgress, boolean changingFov,
                                    CallbackInfoReturnable<Float> cir) {
        float factor = ZoomModule.getInstance().updateAndGetFactor();
        if (factor > 1.0f) {
            cir.setReturnValue(cir.getReturnValue() / factor);
        }
    }
}
