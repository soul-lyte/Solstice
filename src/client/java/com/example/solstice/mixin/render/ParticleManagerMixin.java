package com.example.solstice.mixin.render;

import com.example.solstice.performance.render.ParticleLimiterModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts particle addition to enforce the cap set by
 * {@link ParticleLimiterModule}.
 *
 * <p>Only depends on the public {@code addParticle(Particle)} method -
 * deliberately does not shadow any private ParticleManager field, since
 * that internal storage layout is not part of the stable API and changed
 * as part of the 1.21.9 world-rendering rework.</p>
 */
@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {

    @Inject(
        method = "addParticle(Lnet/minecraft/client/particle/Particle;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void solstice$limitParticles(Particle particle, CallbackInfo ci) {
        if (!ParticleLimiterModule.getInstance().isEnabled()) return;

        float fps = MinecraftClient.getInstance().getCurrentFps();

        if (!ParticleLimiterModule.getInstance().canSpawnParticle(fps)) {
            ci.cancel();
        } else {
            ParticleLimiterModule.getInstance().recordSpawn();
        }
    }
}
