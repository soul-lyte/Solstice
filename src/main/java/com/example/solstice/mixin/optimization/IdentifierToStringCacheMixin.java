package com.example.solstice.mixin.optimization;

import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adapted from C2ME's {@code c2me-opts-allocs} {@code MixinIdentifier} -
 * RelativityMC, MIT, see NOTICE.md. {@code Identifier} is immutable (namespace/
 * path are both {@code final}, confirmed via javap - every {@code withX}
 * method returns a new instance rather than mutating), so its {@code
 * toString()} result never changes for a given instance. Identifiers get
 * stringified constantly throughout the game (logging, debug output, some
 * comparison/lookup paths) - caching after the first call is output-identical
 * and pure.
 *
 * <p>Uses HEAD+TAIL inject rather than C2ME's own {@code @Overwrite} - lets
 * vanilla's real concatenation logic run untouched on a cache miss instead of
 * this file needing to replicate it, only observing and caching the result.</p>
 */
@Mixin(Identifier.class)
public abstract class IdentifierToStringCacheMixin {

    @Unique
    private String solstice$cachedToString;

    @Inject(method = "toString", at = @At("HEAD"), cancellable = true)
    private void solstice$returnCached(CallbackInfoReturnable<String> cir) {
        if (this.solstice$cachedToString != null) {
            cir.setReturnValue(this.solstice$cachedToString);
        }
    }

    @Inject(method = "toString", at = @At("RETURN"))
    private void solstice$cacheResult(CallbackInfoReturnable<String> cir) {
        this.solstice$cachedToString = cir.getReturnValue();
    }
}
