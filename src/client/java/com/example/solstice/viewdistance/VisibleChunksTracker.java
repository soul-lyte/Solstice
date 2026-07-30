package com.example.solstice.viewdistance;

import net.minecraft.util.math.ChunkPos;

import java.util.function.LongConsumer;

/**
 * Ported from Johni0702/bobby (LGPL-3.0-only, see NOTICE.md) - diffs the
 * square of chunk positions within view distance of a center point between
 * calls, reporting exactly which positions newly left ({@code unload}) or
 * newly entered ({@code load}) as the center or radius changes. Used by
 * {@link FakeChunkManager} to know which positions to start/stop caching
 * without re-deriving the whole square from scratch every tick.
 */
public class VisibleChunksTracker {
    private int centerX, centerZ, viewDistance = -1;

    public void updateCenter(int centerX, int centerZ, LongConsumer unload, LongConsumer load) {
        update(centerX, centerZ, viewDistance, unload, load);
    }

    public void updateViewDistance(int viewDistance, LongConsumer unload, LongConsumer load) {
        update(centerX, centerZ, viewDistance, unload, load);
    }

    public void update(int newCenterX, int newCenterZ, int newViewDistance, LongConsumer unload, LongConsumer load) {
        int oldCenterX = this.centerX;
        int oldCenterZ = this.centerZ;
        int oldViewDistance = this.viewDistance;
        if (oldCenterX != newCenterX || oldCenterZ != newCenterZ || oldViewDistance != newViewDistance) {
            if (unload != null) {
                for (int x = oldCenterX - oldViewDistance; x <= oldCenterX + oldViewDistance; x++) {
                    boolean xOutsideNew = x < newCenterX - newViewDistance || x > newCenterX + newViewDistance;
                    for (int z = oldCenterZ - oldViewDistance; z <= oldCenterZ + oldViewDistance; z++) {
                        boolean zOutsideNew = z < newCenterZ - newViewDistance || z > newCenterZ + newViewDistance;
                        if (xOutsideNew || zOutsideNew) {
                            unload.accept(ChunkPos.toLong(x, z));
                        }
                    }
                }
            }

            if (load != null) {
                for (int x = newCenterX - newViewDistance; x <= newCenterX + newViewDistance; x++) {
                    boolean xOutsideOld = x < oldCenterX - oldViewDistance || x > oldCenterX + oldViewDistance;
                    for (int z = newCenterZ - newViewDistance; z <= newCenterZ + newViewDistance; z++) {
                        boolean zOutsideOld = z < oldCenterZ - oldViewDistance || z > oldCenterZ + oldViewDistance;
                        if (xOutsideOld || zOutsideOld) {
                            load.accept(ChunkPos.toLong(x, z));
                        }
                    }
                }
            }

            this.centerX = newCenterX;
            this.centerZ = newCenterZ;
            this.viewDistance = newViewDistance;
        }
    }

    public boolean isInViewDistance(int x, int z) {
        boolean xInside = x >= centerX - viewDistance && x <= centerX + viewDistance;
        boolean zInside = z >= centerZ - viewDistance && z <= centerZ + viewDistance;
        return xInside && zInside;
    }

    public void forEach(LongConsumer consumer) {
        for (int x = centerX - viewDistance; x <= centerX + viewDistance; x++) {
            for (int z = centerZ - viewDistance; z <= centerZ + viewDistance; z++) {
                consumer.accept(ChunkPos.toLong(x, z));
            }
        }
    }
}
