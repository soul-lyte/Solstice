package com.example.solstice.viewdistance;

import com.example.solstice.mixin.viewdistance.BiomeAccessAccessor;
import com.example.solstice.mixin.viewdistance.ClientWorldAccessor;
import com.example.solstice.viewdistance.ext.ChunkLightProviderExt;
import com.example.solstice.viewdistance.ext.ClientChunkManagerExt;
import com.example.solstice.viewdistance.ext.ClientPlayNetworkHandlerExt;
import com.example.solstice.viewdistance.ext.LightingProviderExt;
import io.netty.util.concurrent.DefaultThreadFactory;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Ported from Johni0702/bobby (LGPL-3.0-only, see NOTICE.md), core class -
 * every explored chunk that falls outside the server's real view distance is
 * kept here as a "fake" (cached) {@link WorldChunk} instead of being
 * discarded, and reloaded (from memory if still nearby, otherwise from disk)
 * as the player's view moves back over it. {@link VisibleChunksTracker}
 * drives the diff between "what's currently in view" and "what was in view
 * last update" to know what to load/unload.
 *
 * <p>Scoped down from Bobby's original: no dynamic multi-world merge
 * detection ({@code Worlds}/fingerprinting - a real subsystem for servers
 * that silently reset worlds under the same address, not relevant to this
 * project's singleplayer-focused use case) and no fallback-level migration
 * folder. One fixed on-disk cache folder per world/seed, exactly like
 * Bobby's own non-multi-world fallback path.</p>
 */
public class FakeChunkManager {
    private static final MinecraftClient client = MinecraftClient.getInstance();

    private final ClientWorld world;
    private final ClientChunkManager clientChunkManager;
    private final ClientChunkManagerExt clientChunkManagerExt;
    private final FakeChunkStorage storage;
    private int ticksSinceLastSave;

    private final Long2ObjectMap<WorldChunk> fakeChunks = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());
    private final VisibleChunksTracker chunkTracker = new VisibleChunksTracker();
    private final Long2LongMap toBeUnloaded = new Long2LongOpenHashMap();
    // Contains chunks in order to be unloaded. We keep the chunk and time so we can cross-reference it with
    // [toBeUnloaded] to see if the entry has since been removed / the time reset. This way we do not need
    // to remove entries from the middle of the queue.
    private final Deque<Pair<Long, Long>> unloadQueue = new ArrayDeque<>();

    // The api for loading chunks unfortunately does not handle cancellation, so we utilize a separate thread pool to
    // ensure only a small number of tasks are active at any one time, and all others can still be cancelled before
    // they are submitted.
    // The size of the pool must be sufficiently large such that there is always at least one query operation
    // running, as otherwise the storage io worker will start writing chunks which slows everything down to a crawl.
    private static final ExecutorService loadExecutor = Executors.newFixedThreadPool(8, new DefaultThreadFactory("solstice-viewdistance-loading", true));
    private final Long2ObjectMap<LoadingJob> loadingJobs = new Long2ObjectLinkedOpenHashMap<>();

    // Executor for serialization and saving. Single-threaded so we do not have to worry about races between multiple saves for the same chunk.
    private static final ExecutorService saveExecutor = Executors.newSingleThreadExecutor(new DefaultThreadFactory("solstice-viewdistance-saving", true));

    public FakeChunkManager(ClientWorld world, ClientChunkManager clientChunkManager) {
        this.world = world;
        this.clientChunkManager = clientChunkManager;
        this.clientChunkManagerExt = (ClientChunkManagerExt) clientChunkManager;

        String serverName = getCurrentWorldOrServerName(((ClientWorldAccessor) world).solstice$getNetworkHandler());
        if (serverName.isEmpty()) {
            serverName = "<empty>";
        }
        long seedHash = ((BiomeAccessAccessor) world.getBiomeAccess()).solstice$getSeed();
        RegistryKey<World> worldKey = world.getRegistryKey();
        Identifier worldId = worldKey.getValue();
        Path storagePath = client.runDirectory
                .toPath()
                .resolve(".solstice-chunkcache");
        storagePath = FileSystemUtils.resolveSafeDirectoryName(storagePath, serverName);
        storagePath = storagePath.resolve(seedHash + "");
        storagePath = FileSystemUtils.resolveChild(storagePath, worldId.getNamespace());
        storagePath = FileSystemUtils.resolveChild(storagePath, worldId.getPath());

        storage = FakeChunkStorage.getFor(storagePath, true);
    }

    public WorldChunk getChunk(int x, int z) {
        return fakeChunks.get(ChunkPos.toLong(x, z));
    }

    public FakeChunkStorage getStorage() {
        return storage;
    }

    public void update(boolean blocking, BooleanSupplier shouldKeepTicking) {
        update(blocking, shouldKeepTicking, client.options.getViewDistance().getValue());
    }

    private void update(boolean blocking, BooleanSupplier shouldKeepTicking, int newViewDistance) {
        // Once a minute, force chunks to disk
        if (++ticksSinceLastSave > 20 * 60) {
            storage.completeAll(true);
            ticksSinceLastSave = 0;
        }

        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        int unloadDelaySecs = ViewDistanceModule.getInstance().getUnloadDelaySecs();
        long time = Util.getMeasuringTimeMs();

        List<LoadingJob> newJobs = new ArrayList<>();
        ChunkPos playerChunkPos = player.getChunkPos();
        int newCenterX = playerChunkPos.x;
        int newCenterZ = playerChunkPos.z;
        chunkTracker.update(newCenterX, newCenterZ, newViewDistance, chunkPos -> {
            // Chunk is now outside view distance, can be unloaded / cancelled
            cancelLoad(chunkPos);
            toBeUnloaded.put(chunkPos, time);
            unloadQueue.add(Pair.of(chunkPos, time));
        }, chunkPos -> {
            // Chunk is now inside view distance, load it
            int x = ChunkPos.getPackedX(chunkPos);
            int z = ChunkPos.getPackedZ(chunkPos);

            // We want this chunk, so don't unload it if it's still here
            toBeUnloaded.remove(chunkPos);
            // Not removing it from [unloadQueue], we check [toBeUnloaded] when we poll it.

            // If there already is a chunk loaded, there's nothing to do
            if (clientChunkManager.getChunk(x, z, ChunkStatus.FULL, false) != null) {
                return;
            }

            // All good, load it
            int distanceX = Math.abs(x - newCenterX);
            int distanceZ = Math.abs(z - newCenterZ);
            int distanceSquared = distanceX * distanceX + distanceZ * distanceZ;
            newJobs.add(new LoadingJob(x, z, distanceSquared));
        });

        if (!newJobs.isEmpty()) {
            newJobs.sort(LoadingJob.BY_DISTANCE);
            newJobs.forEach(job -> {
                loadingJobs.put(ChunkPos.toLong(job.x, job.z), job);
                loadExecutor.execute(job);
            });
        }

        // Anything remaining in the set is no longer needed and can now be unloaded
        long unloadTime = time - unloadDelaySecs * 1000L;
        int countSinceLastThrottleCheck = 0;
        while (true) {
            Pair<Long, Long> next = unloadQueue.pollFirst();
            if (next == null) {
                break;
            }
            long chunkPos = next.getLeft();
            long queuedTime = next.getRight();

            if (queuedTime > unloadTime) {
                // Unload is still being delayed, put the entry back into the queue
                // and be done for this update.
                unloadQueue.addFirst(next);
                break;
            }

            long actualQueuedTime = toBeUnloaded.remove(chunkPos);
            if (actualQueuedTime != queuedTime) {
                // The chunk has either been un-queued or re-queued.
                if (actualQueuedTime != 0) {
                    // If it was re-queued, put it back in the map.
                    toBeUnloaded.put(chunkPos, actualQueuedTime);
                }
                // Either way, skip it for now and go to the next entry.
                continue;
            }

            // This chunk is due for unloading
            unload(ChunkPos.getPackedX(chunkPos), ChunkPos.getPackedZ(chunkPos), false);

            if (countSinceLastThrottleCheck++ > 10) {
                countSinceLastThrottleCheck = 0;
                if (!shouldKeepTicking.getAsBoolean()) {
                    break;
                }
            }
        }

        ObjectIterator<LoadingJob> loadingJobsIter = this.loadingJobs.values().iterator();
        jobs:
        while (loadingJobsIter.hasNext()) {
            LoadingJob loadingJob = loadingJobsIter.next();

            //noinspection OptionalAssignedToNull
            while (loadingJob.result == null) {
                // Still loading, should we wait for it?
                if (blocking) {
                    try {
                        //noinspection BusyWait
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    continue jobs;
                }
            }

            // Done loading
            loadingJobsIter.remove();

            Profiler profiler = Profilers.get();
            profiler.push("loadFakeChunk");
            loadingJob.complete();
            profiler.pop();

            if (!shouldKeepTicking.getAsBoolean()) {
                break;
            }
        }
    }

    public void loadMissingChunksFromCache() {
        // We do this by temporarily reducing the client view distance to 0. That will unload all chunks and then try
        // to re-load them (by canceling the unload when they were already loaded, or from the cache when they are
        // missing).
        update(false, () -> false, 0);
        update(false, () -> false);
    }

    public boolean shouldBeLoaded(int x, int z) {
        return chunkTracker.isInViewDistance(x, z);
    }

    private CompletableFuture<Optional<NbtCompound>> loadTag(int x, int z) {
        return storage.loadTag(new ChunkPos(x, z));
    }

    public void load(int x, int z, WorldChunk chunk) {
        fakeChunks.put(ChunkPos.toLong(x, z), chunk);

        loadEmptySectionsOfFakeChunk(x, z, chunk);
        world.resetChunkColor(new ChunkPos(x, z));

        for (int i = world.getBottomSectionCoord(); i < world.getTopSectionCoord(); i++) {
            world.scheduleBlockRenders(x, i, z);
        }

        clientChunkManagerExt.solstice$onFakeChunkAdded(x, z);
    }

    public void loadEmptySectionsOfFakeChunk(int x, int z, WorldChunk chunk) {
        LongOpenHashSet emptySections = clientChunkManager.getActiveSections();
        ChunkSection[] chunkSections = chunk.getSectionArray();
        for (int i = 0; i < chunkSections.length; i++) {
            ChunkSection chunkSection = chunkSections[i];
            if (chunkSection.isEmpty()) {
                emptySections.add(ChunkSectionPos.asLong(x, chunk.sectionIndexToCoord(i), z));
            }
        }
    }

    public boolean unload(int x, int z, boolean willBeReplaced) {
        long chunkPos = ChunkPos.toLong(x, z);
        cancelLoad(chunkPos);
        WorldChunk chunk = fakeChunks.remove(chunkPos);
        if (chunk != null) {
            chunk.clear();

            LightingProvider lightingProvider = clientChunkManager.getLightingProvider();
            LightingProviderExt lightingProviderExt = LightingProviderExt.get(lightingProvider);
            ChunkLightProviderExt blockLightProvider = ChunkLightProviderExt.get(lightingProvider.get(LightType.BLOCK));
            ChunkLightProviderExt skyLightProvider = ChunkLightProviderExt.get(lightingProvider.get(LightType.SKY));

            lightingProviderExt.solstice$disableColumn(ChunkSectionPos.withZeroY(x, z));

            // See the comment above the solstice$queueUnloadFakeLightDataTask implementation
            Runnable unloadLightData = () -> {
                for (int i = 0; i < chunk.getSectionArray().length; i++) {
                    int y = world.sectionIndexToCoord(i);
                    if (blockLightProvider != null) {
                        blockLightProvider.solstice$removeSectionData(ChunkSectionPos.asLong(x, y, z));
                    }
                    if (skyLightProvider != null) {
                        skyLightProvider.solstice$removeSectionData(ChunkSectionPos.asLong(x, y, z));
                    }
                }
            };
            if (willBeReplaced) {
                ClientPlayNetworkHandler networkHandler = ((ClientWorldAccessor) world).solstice$getNetworkHandler();
                ClientPlayNetworkHandlerExt.get(networkHandler).solstice$queueUnloadFakeLightDataTask(() -> {
                    if (fakeChunks.containsKey(chunkPos)) {
                        // Real chunk has been unloaded in the meantime and is now a fake chunk again, that fake
                        // chunk will have loaded its own fake light, so we shouldn't unload it.
                        return;
                    }
                    unloadLightData.run();
                });
            } else {
                unloadLightData.run();

                LongOpenHashSet emptySections = clientChunkManager.getActiveSections();
                ChunkSection[] chunkSections = chunk.getSectionArray();
                for (int i = 0; i < chunkSections.length; i++) {
                    emptySections.remove(ChunkSectionPos.asLong(x, chunk.sectionIndexToCoord(i), z));
                }
            }

            clientChunkManagerExt.solstice$onFakeChunkRemoved(x, z, willBeReplaced);

            return true;
        }
        return false;
    }

    private void cancelLoad(long chunkPos) {
        LoadingJob loadingJob = loadingJobs.remove(chunkPos);
        if (loadingJob != null) {
            loadingJob.cancelled = true;
        }
    }

    public Supplier<WorldChunk> save(WorldChunk chunk) {
        Pair<WorldChunk, Supplier<WorldChunk>> copy = ChunkSerializer.shallowCopy(chunk);
        LightingProvider lightingProvider = chunk.getWorld().getLightingProvider();
        saveExecutor.execute(() -> {
            NbtCompound nbt = ChunkSerializer.serialize(copy.getLeft(), lightingProvider);
            nbt.putLong("age", System.currentTimeMillis()); // fallback in case meta gets corrupted
            storage.save(chunk.getPos(), nbt);
        });
        return copy.getRight();
    }

    private static String getCurrentWorldOrServerName(ClientPlayNetworkHandler networkHandler) {
        IntegratedServer integratedServer = client.getServer();
        if (integratedServer != null) {
            return integratedServer.getSaveProperties().getLevelName();
        }

        ServerInfo serverInfo = networkHandler.getServerInfo();
        if (serverInfo != null) {
            if (serverInfo.isRealm()) {
                return "realms";
            }
            return serverInfo.address.replace(':', '_');
        }

        return "unknown";
    }

    public String getDebugString() {
        return "F: " + fakeChunks.size() + " L: " + loadingJobs.size() + " U: " + toBeUnloaded.size();
    }

    public Collection<WorldChunk> getFakeChunks() {
        return fakeChunks.values();
    }

    public void close() {
        try {
            storage.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private class LoadingJob implements Runnable {
        private final int x;
        private final int z;
        private final int distanceSquared;
        private volatile boolean cancelled;
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // null while loading, empty() if no chunk was found
        private volatile Optional<Supplier<WorldChunk>> result;

        public LoadingJob(int x, int z, int distanceSquared) {
            this.x = x;
            this.z = z;
            this.distanceSquared = distanceSquared;
        }

        @Override
        public void run() {
            if (cancelled) {
                return;
            }
            Optional<NbtCompound> value;
            try {
                value = loadTag(x, z).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                value = Optional.empty();
            }
            if (cancelled) {
                return;
            }
            result = value.map(it -> ChunkSerializer.deserialize(new ChunkPos(x, z), it, world).getRight());
        }

        public void complete() {
            result.ifPresent(it -> load(x, z, it.get()));
        }

        public static final Comparator<LoadingJob> BY_DISTANCE = Comparator.comparing(it -> it.distanceSquared);
    }
}
