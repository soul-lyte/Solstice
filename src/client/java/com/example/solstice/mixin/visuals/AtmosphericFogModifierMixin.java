package com.example.solstice.mixin.visuals;

import com.example.solstice.qol.visuals.VisualsModule;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.AtmosphericFogModifier;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "No Fog > Render Distance" - deliberately does NOT cancel this modifier
 * outright the way the other three fog types do (see the sibling Mixins in
 * this package). Render-distance fog is also what camouflages the boundary
 * of already-loaded terrain from not-yet-loaded chunks beyond it; fully
 * removing it exposed that boundary as a raw black flash for a moment while
 * new chunks streamed in. Instead this pushes the fog several times further
 * out than vanilla would - it stays there to mask the loading edge, but sits
 * so far past your actual render distance it isn't perceptible day to day.
 */
@Mixin(AtmosphericFogModifier.class)
public abstract class AtmosphericFogModifierMixin {

    private static final float PUSH_OUT_FACTOR = 6.0f;

    @Inject(method = "applyStartEndModifier", at = @At("TAIL"))
    private void solstice$pushFogOut(FogData fogData, Camera camera, ClientWorld world, float viewDistance,
                                      RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!VisualsModule.getInstance().isFeatureActive(VisualsModule.noFogAtmospheric)) return;

        fogData.renderDistanceStart *= PUSH_OUT_FACTOR;
        fogData.renderDistanceEnd *= PUSH_OUT_FACTOR;
        fogData.environmentalStart *= PUSH_OUT_FACTOR;
        fogData.environmentalEnd *= PUSH_OUT_FACTOR;
        fogData.skyEnd *= PUSH_OUT_FACTOR;
        fogData.cloudEnd *= PUSH_OUT_FACTOR;
    }
}
