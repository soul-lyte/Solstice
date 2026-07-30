package com.example.solstice.mixin.hunger;

import net.minecraft.entity.player.HungerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes {@link HungerManager}'s private {@code exhaustion} field, which
 * vanilla has no public getter (or setter) for. A pure field accessor - no
 * injection, no behavior change, cannot alter gameplay on its own.
 *
 * <p>Lives in the common source set (not client-only): the server-side half
 * of the exhaustion sync ({@code SolsticeMod}'s tick hook, reading the real
 * value from each {@code ServerPlayerEntity}'s hunger manager) needs this
 * too, and common code cannot see client-only classes.</p>
 */
@Mixin(HungerManager.class)
public interface HungerManagerAccessor {

    @Accessor("exhaustion")
    float solstice$getExhaustion();

    @Mutable
    @Accessor("exhaustion")
    void solstice$setExhaustion(float value);
}
