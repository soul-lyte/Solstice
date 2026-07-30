package com.example.solstice.viewdistance.ext;

import net.minecraft.world.chunk.light.LightingProvider;

/**
 * Ported from Johni0702/bobby (LGPL-3.0-only, see NOTICE.md), implemented by
 * {@code LightingProviderMixin}. A shadow "enabled columns" set that fully
 * bypasses vanilla's own light-engine bookkeeping rather than trying to
 * toggle its real internal state - see that Mixin's own Javadoc for why.
 */
public interface LightingProviderExt {
    void solstice$enabledColumn(long pos);
    void solstice$disableColumn(long pos);

    static LightingProviderExt get(LightingProvider provider) {
        return (provider instanceof LightingProviderExt) ? (LightingProviderExt) provider : null;
    }
}
