package com.example.solstice.mixin.visuals;

import com.example.solstice.qol.visuals.VisualsModule;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.render.fog.WaterFogModifier;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "No Fog > Water" - pushes the fog distance out instead of cancelling
 * {@code shouldApply} (the previous approach, real crash - see below), same
 * technique {@code AtmosphericFogModifierMixin} already uses and for the
 * same underlying reason.
 *
 * <p>Confirmed via decompiling {@code FogRenderer.getFogColor}: it requires
 * finding exactly one applicable modifier with {@code isColorSource() ==
 * true} among {@link net.minecraft.client.render.fog.FogModifier}'s full
 * list, or it throws {@code IllegalStateException("No color source
 * environment found")}. {@code WaterFogModifier.shouldApply} only returns
 * true for {@code CameraSubmersionType.WATER}, and it's the *only* modifier
 * in the list that does for that submersion type - cancelling it outright
 * meant the moment the camera actually entered water, zero modifiers
 * qualified as a color source and the game crashed on the very next frame.
 * Confirmed by actually reproducing it, not just reasoning about it.</p>
 */
@Mixin(WaterFogModifier.class)
public abstract class WaterFogModifierMixin {

    @Inject(method = "applyStartEndModifier", at = @At("TAIL"))
    private void solstice$pushFogOut(FogData fogData, Camera camera, ClientWorld world, float viewDistance,
                                      RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!VisualsModule.getInstance().isFeatureActive(VisualsModule.noFogWater)) return;

        fogData.environmentalStart = viewDistance * 0.5f;
        fogData.environmentalEnd = viewDistance;
        fogData.skyEnd = fogData.environmentalEnd;
        fogData.cloudEnd = fogData.environmentalEnd;
    }
}
