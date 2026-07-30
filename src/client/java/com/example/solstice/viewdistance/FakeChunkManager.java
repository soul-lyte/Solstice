package com.example.solstice.viewdistance;

import com.example.solstice.SolsticeMod;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Holds onto real {@link WorldChunk} objects the server told the client to
 * unload, for chunks within {@link ViewDistanceModule}'s configured extra
 * ring, instead of letting them be discarded immediately - a Voxy/Bobby-style
 * "keep already-explored chunks visible" behavior with no disk persistence.
 *
 * <p><b>Second attempt at this mechanism</b> (see git history / prior session
 * notes): the first version cancelled vanilla's {@code unload()} outright,
 * assuming that would keep the chunk resident in vanilla's own chunk map.
 * That was wrong - {@code ClientChunkManager$ClientChunkMap}'s backing store
 * is a fixed-size array indexed by chunk position with modulo wraparound
 * (sized to the configured view distance), so a different chunk claiming the
 * same array slot as the player moves silently overwrites it regardless of
 * whether {@code unload()} ran - producing exactly the "random reload/
 * corruption" symptom that was reported. This version keeps retained chunks
 * in Solstice's own map entirely, lets vanilla's real {@code unload()} run
 * normally (so its own bookkeeping stays internally consistent), and only
 * resurfaces a retained chunk through a {@code getChunk()} override.</p>
 */
public final class FakeChunkManager {

    private static final FakeChunkManager INSTANCE = new FakeChunkManager();

    private final Long2ObjectMap<WorldChunk> retained = new Long2ObjectOpenHashMap<>();

    private FakeChunkManager() {}

    public static FakeChunkManager getInstance() { return INSTANCE; }

    /** Called from {@code ClientChunkManagerMixin} just before a chunk's real unload proceeds. */
    public void onChunkUnloading(ChunkPos pos, WorldChunk chunk, MinecraftClient client) {
        ViewDistanceModule module = ViewDistanceModule.getInstance();
        if (!module.isEnabled()) return;

        PlayerEntity player = client.player;
        if (player == null) return;

        if (distanceFromPlayer(pos, player) > extendedDistance(client)) return;
        retained.put(ChunkPos.toLong(pos.x, pos.z), chunk);
        SolsticeMod.LOGGER.info("[Solstice] ViewDistance: captured ({}, {}) on unload, retained={}",
                pos.x, pos.z, retained.size());
    }

    /** Called from {@code ClientChunkManagerMixin} when vanilla's own lookup came back empty. */
    public WorldChunk getRetained(int x, int z) {
        WorldChunk retained = this.retained.get(ChunkPos.toLong(x, z));
        if (retained != null) {
            SolsticeMod.LOGGER.info("[Solstice] ViewDistance: resurfaced ({}, {}) from retention", x, z);
        }
        return retained;
    }

    /** Called each tick (from {@code MinecraftClientMixin}) - stops tracking anything now outside the extended ring. */
    public void tick(MinecraftClient client) {
        if (retained.isEmpty()) return;

        PlayerEntity player = client.player;
        if (player == null) return;

        int extended = extendedDistance(client);
        int playerChunkX = player.getChunkPos().x;
        int playerChunkZ = player.getChunkPos().z;

        retained.long2ObjectEntrySet().removeIf(entry -> {
            long packed = entry.getLongKey();
            int x = ChunkPos.getPackedX(packed);
            int z = ChunkPos.getPackedZ(packed);
            int dist = Math.max(Math.abs(x - playerChunkX), Math.abs(z - playerChunkZ));
            boolean evict = dist > extended;
            if (evict) {
                SolsticeMod.LOGGER.info("[Solstice] ViewDistance: evicted ({}, {}), retained={}",
                        x, z, retained.size() - 1);
            }
            return evict;
        });
    }

    /** Called on world disconnect/change so chunks from one world never leak into another. */
    public void clear() {
        retained.clear();
    }

    private int distanceFromPlayer(ChunkPos pos, PlayerEntity player) {
        ChunkPos playerPos = player.getChunkPos();
        return Math.max(Math.abs(pos.x - playerPos.x), Math.abs(pos.z - playerPos.z));
    }

    private int extendedDistance(MinecraftClient client) {
        // Must match BuiltChunkStorageMixin's math exactly - it inflates the same
        // getClampedViewDistance() value (not the raw, unclamped option) that
        // WorldRenderer.reload() actually sizes the render storage with, so a
        // server-side view-distance clamp can't put this ahead of what the renderer
        // actually has slots for.
        return client.options.getClampedViewDistance() + ViewDistanceModule.extraChunks;
    }
}
