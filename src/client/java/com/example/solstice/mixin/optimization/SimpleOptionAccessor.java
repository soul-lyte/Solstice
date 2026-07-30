package com.example.solstice.mixin.optimization;

import com.mojang.serialization.Codec;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Adapted from C2ME's {@code c2me-client-uncapvd} {@code ISimpleOption} -
 * RelativityMC, MIT, see NOTICE.md. {@code SimpleOption.callbacks}/{@code
 * codec} are both {@code private final} (confirmed via decompile), so
 * widening an already-built option's valid range in place needs both
 * replaced directly - hence {@code @Mutable} on each accessor.
 *
 * <p>{@code callbacks}'s real declared type, {@code SimpleOption.Callbacks},
 * is package-private in {@code net.minecraft.client.option} (confirmed via
 * {@code javap} - no explicit access modifier on the nested interface).
 * Widened to public via {@code solstice.accesswidener} (C2ME's own build
 * does the same via its own access widener) so it can be named directly
 * here - an earlier {@code Object}-typed version of this file compiled fine
 * but broke Mixin's refmap generation ({@code gradle build} logged "Cannot
 * remap callbacks because it does not exist in any of the targets" - the
 * remapper matches accessor targets by name *and* descriptor, and an
 * {@code Object} descriptor doesn't match the real field's actual type),
 * which would have failed to resolve the field at runtime on a real
 * (non-dev) install. Typing this correctly, backed by the access widener,
 * is what actually fixes that.</p>
 */
@Mixin(SimpleOption.class)
public interface SimpleOptionAccessor {

    @Accessor("callbacks")
    @Mutable
    void solstice$setCallbacks(SimpleOption.Callbacks<Integer> callbacks);

    @Accessor("codec")
    @Mutable
    void solstice$setCodec(Codec<Integer> codec);
}
