package com.example.solstice.mixin.visuals;

import com.example.solstice.qol.visuals.VisualsModule;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "No Food Particles": skips the crumb particles any {@link LivingEntity}
 * spawns while eating/drinking - applies to every entity on the client, not
 * just the local player, since it's a pure visual declutter.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityFoodParticleMixin {

    @Inject(method = "spawnItemParticles", at = @At("HEAD"), cancellable = true)
    private void solstice$onSpawnItemParticles(ItemStack stack, int count, CallbackInfo ci) {
        if (VisualsModule.getInstance().isFeatureActive(VisualsModule.noFoodParticles)) {
            ci.cancel();
        }
    }
}
