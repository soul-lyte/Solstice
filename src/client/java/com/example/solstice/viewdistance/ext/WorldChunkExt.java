package com.example.solstice.viewdistance.ext;

import net.minecraft.network.packet.s2c.play.LightData;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

/**
 * Ported from Johni0702/bobby (LGPL-3.0-only, see NOTICE.md), implemented by
 * {@code WorldChunkMixin}. Captures a real chunk's initial light packet data
 * so a chunk that gets replaced by a fake one can serialize the exact light
 * it arrived with, rather than whatever the light engine has drifted to.
 */
public interface WorldChunkExt {
    void solstice$setInitialLightData(@Nullable LightData data);
    @Nullable LightData solstice$getInitialLightData();

    static WorldChunkExt get(WorldChunk chunk) {
        return (chunk instanceof WorldChunkExt) ? (WorldChunkExt) chunk : null;
    }
}
