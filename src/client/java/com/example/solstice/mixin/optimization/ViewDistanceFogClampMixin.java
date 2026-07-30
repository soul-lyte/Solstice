package com.example.solstice.mixin.optimization;

import net.minecraft.client.render.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Adapted from C2ME's {@code c2me-client-uncapvd} {@code MixinFogRenderer} -
 * RelativityMC, MIT, see NOTICE.md. {@link ViewDistanceUncapMixin} lets the
 * render-distance slider go well past vanilla's normal cap; without this,
 * the extra distance would also feed straight into fog color blending
 * (confirmed via decompile/javap - {@code FogRenderer.applyFog} passes the
 * live view distance into the private {@code getFogColor} as its 4th
 * argument), which vanilla never tests past its own 32-chunk ceiling.
 * Clamping just this one input keeps fog color blending exactly as vanilla
 * behaves today regardless of how far the slider is actually set - a purely
 * defensive clamp, not a visual change for anyone at or under 32.
 *
 * <p>Full method descriptor required on {@code applyFog} - {@code
 * FogRenderer} has two unrelated overloads of that name (confirmed via
 * javap), plain name matching would be ambiguous.</p>
 */
@Mixin(FogRenderer.class)
public abstract class ViewDistanceFogClampMixin {

    @ModifyArg(method = "applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/fog/FogRenderer;getFogColor(Lnet/minecraft/client/render/Camera;FLnet/minecraft/client/world/ClientWorld;IF)Lorg/joml/Vector4f;"),
            index = 3)
    private int solstice$clampFogViewDistance(int viewDistance) {
        return Math.clamp(viewDistance, 2, 32);
    }
}
