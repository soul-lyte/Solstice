package com.example.solstice.mixin.effects;

import com.example.solstice.qol.effects.FullbrightModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Real body of the Fullbright override - see {@link FullbrightModule}'s
 * Javadoc for why this redirects {@code SimpleOption.getValue()} (matched
 * by the option instance's own identity, not an ordinal) rather than
 * touching a local variable inside {@code update(float)}.
 *
 * <p>{@code SimpleOption.getValue()} is called 3 times in that method
 * (hideLightningFlashes, darknessEffectScale, gamma) - this redirect fires
 * for all three, but only substitutes a value when the receiver instance
 * is specifically {@code GameOptions.getGamma()}'s own cached {@code
 * SimpleOption} (constructed once in {@code GameOptions}'s constructor and
 * never replaced, confirmed via the same decompile this project already
 * did for the view-distance-uncap work) - the other two pass through
 * completely untouched.</p>
 */
@Mixin(LightmapTextureManager.class)
public abstract class FullbrightMixin {

    private static final double FULLBRIGHT_GAMMA = 100.0;

    @Redirect(method = "update(F)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"))
    private Object solstice$overrideGamma(SimpleOption<?> instance) {
        Object real = instance.getValue();
        if (instance == MinecraftClient.getInstance().options.getGamma()
                && FullbrightModule.getInstance().isEnabled()) {
            return FULLBRIGHT_GAMMA;
        }
        return real;
    }
}
