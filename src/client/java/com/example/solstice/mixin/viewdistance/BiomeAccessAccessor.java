package com.example.solstice.mixin.viewdistance;

import net.minecraft.world.biome.source.BiomeAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes {@code BiomeAccess}'s private {@code seed} field, needed by
 * {@link com.example.solstice.viewdistance.FakeChunkManager} to key each
 * cached world's chunk folder to its own seed (so a same-named world that
 * regenerates under a new seed never loads another world's stale chunks).
 * Ported from Johni0702/bobby's equivalent accessor (LGPL-3.0-only, see
 * NOTICE.md).
 */
@Mixin(BiomeAccess.class)
public interface BiomeAccessAccessor {
    @Accessor("seed")
    long solstice$getSeed();
}
