package com.example.solstice.mixin.visuals;

import com.example.solstice.qol.visuals.VisualsModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "No Pumpkin Blur": {@code InGameHud.renderOverlay} is the one shared helper
 * vanilla uses to draw any item-driven camera overlay - the carved pumpkin's
 * {@code cameraOverlay} component texture ({@code misc/pumpkinblur}) as well
 * as the powder-snow "freezing" outline both route through it. Only the
 * pumpkin one is cancelled here, matched by texture path, so freezing still
 * gets its (gameplay-relevant) visual feedback.
 */
@Mixin(InGameHud.class)
public abstract class InGameHudOverlayMixin {

    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
    private void solstice$onRenderOverlay(DrawContext context, Identifier textureId, float opacity, CallbackInfo ci) {
        if (VisualsModule.getInstance().isFeatureActive(VisualsModule.noPumpkinBlur)
                && textureId.getPath().contains("pumpkinblur")) {
            ci.cancel();
        }
    }
}
