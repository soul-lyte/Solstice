package com.example.solstice.mixin.effects;

import com.example.solstice.qol.effects.StatusEffectVisionModule;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.fog.StatusEffectFogModifier;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * "Status Effects": {@code shouldApply} is implemented once on this shared
 * abstract base and inherited by both {@code BlindnessEffectFogModifier} and
 * {@code DarknessEffectFogModifier}, so mixing in here covers both with one
 * Mixin. Moved from {@code mixin.visuals} into its own package/module - see
 * {@link StatusEffectVisionModule}'s Javadoc for why cancelling this outright
 * is safe (unlike Water/Lava's own fog modifiers).
 */
@Mixin(StatusEffectFogModifier.class)
public abstract class StatusEffectFogModifierMixin {

    @Inject(method = "shouldApply", at = @At("RETURN"), cancellable = true)
    private void solstice$onShouldApply(CameraSubmersionType submersionType, Entity entity,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (StatusEffectVisionModule.getInstance().isEnabled()) {
            cir.setReturnValue(false);
        }
    }
}
