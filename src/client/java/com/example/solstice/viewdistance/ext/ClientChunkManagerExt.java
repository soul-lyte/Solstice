package com.example.solstice.viewdistance.ext;

import com.example.solstice.viewdistance.FakeChunkManager;
import com.example.solstice.viewdistance.VisibleChunksTracker;

/**
 * Ported from Johni0702/bobby (LGPL-3.0-only, see NOTICE.md), implemented by
 * {@code ClientChunkManagerMixin}.
 */
public interface ClientChunkManagerExt {
    FakeChunkManager solstice$getFakeChunkManager();
    VisibleChunksTracker solstice$getRealChunksTracker();
    void solstice$onFakeChunkAdded(int x, int z);
    void solstice$onFakeChunkRemoved(int x, int z, boolean willBeReplaced);
}
