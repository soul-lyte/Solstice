package com.example.solstice.mixin.visuals;

import com.example.solstice.qol.visuals.VisualsModule;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.particle.TotemParticle;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "Small Totem Pop": shrinks the golden sparkle burst spawned when a totem
 * of undying saves you - the most screen-covering part of the vanilla
 * effect. (Vanilla's separate item-icon flash overlay isn't touched by
 * this - no clean Mixin target for it was found in this Minecraft version.)
 */
@Mixin(TotemParticle.class)
public abstract class TotemParticleMixin {

    private static final float SMALL_TOTEM_SCALE = 0.6f;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void solstice$onInit(ClientWorld world, double x, double y, double z,
                                  double velX, double velY, double velZ,
                                  SpriteProvider spriteProvider, CallbackInfo ci) {
        if (VisualsModule.getInstance().isFeatureActive(VisualsModule.smallTotemPop)) {
            ((TotemParticle) (Object) this).scale(SMALL_TOTEM_SCALE);
        }
    }
}
