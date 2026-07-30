package com.example.solstice.mixin.effects;

import com.example.solstice.qol.effects.StatusEffectVisionModule;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * "Status Effects" (nausea half) - clears the Nausea screen warp. Confirmed
 * via decompile: {@code GameRenderer}'s projection-matrix distortion reads
 * {@code clientPlayerEntity.getEffectFadeFactor(StatusEffects.NAUSEA, tickDelta)}
 * as one of its two inputs (the other, {@code ClientPlayerEntity.nauseaIntensity},
 * is the separate nether-portal transition wobble - deliberately left alone,
 * see {@link StatusEffectVisionModule}'s Javadoc). Only intercepts the query
 * for the local player and only for {@code NAUSEA} specifically - this method
 * is a shared per-status-effect utility used for other effects too.
 */
@Mixin(LivingEntity.class)
public abstract class NauseaFadeFactorMixin {

    @Inject(method = "getEffectFadeFactor", at = @At("HEAD"), cancellable = true)
    private void solstice$onGetEffectFadeFactor(RegistryEntry<StatusEffect> effect, float tickDelta,
                                                 CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof ClientPlayerEntity
                && effect == StatusEffects.NAUSEA
                && StatusEffectVisionModule.getInstance().isEnabled()
                && StatusEffectVisionModule.clearNausea) {
            cir.setReturnValue(0.0f);
        }
    }
}
