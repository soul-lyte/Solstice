package com.example.solstice.mixin.visuals;

import com.example.solstice.qol.visuals.VisualsModule;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.render.fog.LavaFogModifier;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "No Fog > Lava" - pushes the fog distance out instead of cancelling
 * {@code shouldApply}. Same real crash as {@code WaterFogModifierMixin} had
 * (see its Javadoc for the full decompiled explanation) - {@code
 * LavaFogModifier.shouldApply} is the only modifier that applies for {@code
 * CameraSubmersionType.LAVA}, so cancelling it removed the only valid color
 * source the instant the camera entered lava.
 */
@Mixin(LavaFogModifier.class)
public abstract class LavaFogModifierMixin {

    @Inject(method = "applyStartEndModifier", at = @At("TAIL"))
    private void solstice$pushFogOut(FogData fogData, Camera camera, ClientWorld world, float viewDistance,
                                      RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!VisualsModule.getInstance().isFeatureActive(VisualsModule.noFogLava)) return;

        fogData.environmentalStart = viewDistance * 0.5f;
        fogData.environmentalEnd = viewDistance;
        fogData.skyEnd = fogData.environmentalEnd;
        fogData.cloudEnd = fogData.environmentalEnd;
    }
}
