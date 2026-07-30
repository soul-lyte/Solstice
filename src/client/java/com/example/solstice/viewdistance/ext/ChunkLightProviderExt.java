package com.example.solstice.viewdistance.ext;

import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.light.ChunkLightingView;

/**
 * Ported from Johni0702/bobby (LGPL-3.0-only, see NOTICE.md), implemented by
 * {@code ChunkLightProviderMixin}. A shadow per-section light-data store that
 * fully bypasses vanilla's own light storage, returned directly whenever a
 * position has cached data - see that Mixin's own Javadoc for why.
 */
public interface ChunkLightProviderExt {
    void solstice$addSectionData(long pos, ChunkNibbleArray data);
    void solstice$removeSectionData(long pos);

    void solstice$setTainted(long pos, int delta);

    static ChunkLightProviderExt get(ChunkLightingView view) {
        return (view instanceof ChunkLightProviderExt) ? (ChunkLightProviderExt) view : null;
    }
}
