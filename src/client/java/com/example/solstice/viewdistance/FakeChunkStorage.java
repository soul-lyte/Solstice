package com.example.solstice.viewdistance;

import net.minecraft.client.MinecraftClient;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.StorageKey;
import net.minecraft.world.storage.VersionedChunkStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Ported from Johni0702/bobby (LGPL-3.0-only, see NOTICE.md) - real,
 * vanilla-compatible region-file NBT chunk storage (thin wrapper around
 * vanilla's own {@link VersionedChunkStorage}, the same class real world
 * saves use), one instance per world's on-disk chunk cache folder.
 *
 * <p>Scoped down from Bobby's original: no {@code LastAccessFile}-tracked
 * "delete regions unused for N days" cleanup and no {@code /bobby upgrade}
 * command support - both are edge-case management tooling for a long-lived
 * multi-server cache, matching this project's established
 * scope-down-the-tooling precedent used elsewhere for C2ME/VMP/notickvd.</p>
 */
public class FakeChunkStorage extends VersionedChunkStorage {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<Path, FakeChunkStorage> active = new HashMap<>();

    public static FakeChunkStorage getFor(Path directory, boolean writeable) {
        synchronized (active) {
            return active.computeIfAbsent(directory, f -> new FakeChunkStorage(directory, writeable));
        }
    }

    public static void closeAll() {
        synchronized (active) {
            for (FakeChunkStorage storage : active.values()) {
                try {
                    storage.close();
                } catch (IOException e) {
                    LOGGER.error("Failed to close storage", e);
                }
            }
            active.clear();
        }
    }

    private FakeChunkStorage(Path directory, boolean writeable) {
        super(
                new StorageKey("dummy", World.OVERWORLD, "solstice-viewdistance"),
                directory,
                MinecraftClient.getInstance().getDataFixer(),
                false,
                DataFixTypes.CHUNK
        );

        if (writeable) {
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                LOGGER.error("Failed to create chunk cache directory:", e);
            }
        }
    }

    public void save(ChunkPos pos, NbtCompound chunk) {
        setNbt(pos, chunk);
    }

    public CompletableFuture<Optional<NbtCompound>> loadTag(ChunkPos pos) {
        return getNbt(pos);
    }
}
