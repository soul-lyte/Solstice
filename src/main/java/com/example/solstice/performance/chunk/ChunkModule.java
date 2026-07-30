package com.example.solstice.performance.chunk;

import com.example.solstice.core.module.AbstractModule;
import com.example.solstice.core.module.ModuleCategory;

/**
 * ChunkModule - chunk build and load prioritization logic.
 *
 * <p><b>Not currently active.</b> {@link #getBuildPriority} is implemented
 * but is not called from anywhere: neither of its two candidate Mixin hooks
 * ({@code ChunkBuilderMixin}, {@code ClientChunkManagerMixin}) actually
 * wired this logic into a real chunk-builder call site - they were
 * placeholders even before Minecraft 1.21.9's world-rendering rework made
 * their target classes' exact internals uncertain for this version, so both
 * were removed rather than left as broken or fake-functional Mixins. This
 * module is deliberately NOT registered in {@code SolsticeMod} - see the
 * comment there.</p>
 *
 * <p>Intended strategy once properly wired:
 * <ul>
 *   <li>Prioritize chunk rebuild tasks near the player</li>
 *   <li>Throttle distant chunk builds when the rebuild queue is large</li>
 * </ul>
 */
public final class ChunkModule extends AbstractModule {

    private static final ChunkModule INSTANCE = new ChunkModule();

    /** Maximum queued rebuild tasks before throttling far chunks. */
    public static int rebuildQueueThrottle = 32;

    /** Chunk sections within this radius (in chunks) of the player are high-priority. */
    public static int highPriorityRadius = 4;

    /** Whether to skip mesh generation for all-air chunk sections. */
    public static boolean skipEmptySections = true;

    private ChunkModule() {}

    public static ChunkModule getInstance() { return INSTANCE; }

    @Override public String getId()          { return "chunk"; }
    @Override public String getDisplayName() { return "Chunk Optimizations"; }
    @Override public String getDescription() { return "Prioritizes nearby chunk builds, throttles distant rebuilds, and skips empty section meshing."; }
    @Override public ModuleCategory getCategory() { return ModuleCategory.ADVANCED; }

    @Override
    protected void init() {
        rebuildQueueThrottle = com.example.solstice.core.config.ConfigManager.getInstance()
                .getInt("chunk.rebuild_queue_throttle", 32);
        highPriorityRadius = com.example.solstice.core.config.ConfigManager.getInstance()
                .getInt("chunk.high_priority_radius", 4);
        skipEmptySections = com.example.solstice.core.config.ConfigManager.getInstance()
                .getBoolean("chunk.skip_empty_sections", true);
    }

    /**
     * Called from {@code ChunkBuilderMixin} to determine build priority.
     * Higher value = higher priority (processed first).
     *
     * @param chunkX  chunk X coordinate of the section being rebuilt
     * @param chunkZ  chunk Z coordinate
     * @param playerX player chunk X
     * @param playerZ player chunk Z
     * @param queueSize current rebuild queue depth
     */
    public int getBuildPriority(int chunkX, int chunkZ, int playerX, int playerZ, int queueSize) {
        if (!isEnabled()) return 0;

        int dx = Math.abs(chunkX - playerX);
        int dz = Math.abs(chunkZ - playerZ);
        int dist = Math.max(dx, dz);

        // Throttle distant chunks when queue is large
        if (queueSize > rebuildQueueThrottle && dist > highPriorityRadius) {
            return -1; // deprioritize
        }

        // Closer = higher priority
        return Integer.MAX_VALUE - dist;
    }
}
