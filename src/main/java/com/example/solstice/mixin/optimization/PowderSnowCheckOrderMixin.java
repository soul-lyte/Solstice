package com.example.solstice.mixin.optimization;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Adapted from the same idea as Lithium's {@code entity/fast_powder_snow_check}
 * mixin - CaffeineMC, LGPL-3.0-only, see NOTICE.md and licenses/LICENSE-LGPL-3.0.txt.
 * Not a literal port - Lithium's own version targets a different method shape
 * ({@code tryAddFrost}, a multi-redirect chain) that doesn't exist in 1.21.11;
 * confirmed via decompile that the real equivalent here is {@code
 * LivingEntity.addPowderSnowSlowIfNeeded()}, called unconditionally every
 * server tick for every living entity, which checks {@code
 * getLandingBlockState().isAir()} (a real world/chunk block-state lookup)
 * BEFORE the cheap {@code getFrozenTicks() > 0} check. Since neither call has
 * side effects, evaluating the cheap check first and only doing the real
 * lookup when it's true is output-identical - just skips a chunk lookup for
 * every entity that was never in powder snow, which is nearly all of them at
 * any given moment. {@code extends Entity} (mirroring the real target's
 * superclass) is required here, not just style, since {@code
 * getLandingBlockState()}/{@code getFrozenTicks()} are {@code protected}
 * members declared on {@code Entity}, not {@code LivingEntity}.
 */
@Mixin(LivingEntity.class)
public abstract class PowderSnowCheckOrderMixin extends Entity {

    public PowderSnowCheckOrderMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Redirect(method = "addPowderSnowSlowIfNeeded", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;getLandingBlockState()Lnet/minecraft/block/BlockState;"))
    private BlockState solstice$skipBlockStateLookupIfNeverFrozen(LivingEntity instance) {
        if (this.getFrozenTicks() <= 0) {
            return Blocks.AIR.getDefaultState();
        }
        return this.getLandingBlockState();
    }
}
