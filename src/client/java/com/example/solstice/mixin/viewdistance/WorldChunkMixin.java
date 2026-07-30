package com.example.solstice.mixin.viewdistance;

import com.example.solstice.viewdistance.ext.WorldChunkExt;
import net.minecraft.network.packet.s2c.play.LightData;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Ported from Johni0702/bobby (LGPL-3.0-only, see NOTICE.md). Captures the
 * exact light data a chunk arrived with in its initial load packet, so if it
 * later gets replaced by a retained/fake chunk, serialization can use the
 * light as-received rather than whatever the live light engine has since
 * drifted to.
 */
@Mixin(WorldChunk.class)
public class WorldChunkMixin implements WorldChunkExt {
    @Unique
    private LightData solstice$initialLightData;

    @Override
    public void solstice$setInitialLightData(@Nullable LightData data) {
        this.solstice$initialLightData = data;
    }

    @Override
    public @Nullable LightData solstice$getInitialLightData() {
        return solstice$initialLightData;
    }
}
