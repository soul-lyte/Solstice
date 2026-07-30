package com.example.solstice.viewdistance;

import com.mojang.serialization.Codec;
import com.example.solstice.viewdistance.ext.ChunkLightProviderExt;
import com.example.solstice.viewdistance.ext.LightingProviderExt;
import com.example.solstice.viewdistance.ext.WorldChunkExt;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLongArray;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.packet.s2c.play.LightData;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PaletteProvider;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.ReadableContainer;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Ported from Johni0702/bobby (LGPL-3.0-only, see NOTICE.md) - chunk NBT
 * (de)serialization, including capturing/restoring the "shadow" light data
 * consumed by {@link com.example.solstice.mixin.viewdistance.LightingProviderMixin}
 * / {@link com.example.solstice.mixin.viewdistance.ChunkLightProviderMixin}.
 * The NBT shape written here is real, vanilla-compatible chunk NBT (same
 * region-file format vanilla itself uses), not a custom format.
 *
 * <p>Scoped down from Bobby's original: no chunk fingerprinting (that only
 * ever fed the dropped multi-world-merge feature).</p>
 */
public class ChunkSerializer {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final ChunkNibbleArray COMPLETELY_DARK = new ChunkNibbleArray();
    private static final ChunkNibbleArray COMPLETELY_LIT = new ChunkNibbleArray();

    static {
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    COMPLETELY_LIT.set(x, y, z, 15);
                }
            }
        }
    }

    private static final Codec<PalettedContainer<BlockState>> BLOCK_CODEC = PalettedContainer.createPalettedContainerCodec(
            BlockState.CODEC,
            PaletteProvider.forBlockStates(Block.STATE_IDS),
            Blocks.AIR.getDefaultState()
    );

    public static NbtCompound serialize(WorldChunk chunk, LightingProvider lightingProvider) {
        DynamicRegistryManager registryManager = chunk.getWorld().getRegistryManager();
        Registry<Biome> biomeRegistry = registryManager.getOrThrow(RegistryKeys.BIOME);
        Codec<ReadableContainer<RegistryEntry<Biome>>> biomeCodec = PalettedContainer.createReadableContainerCodec(
                biomeRegistry.getEntryCodec(),
                PaletteProvider.forBiomes(biomeRegistry.getIndexedEntries()),
                biomeRegistry.getEntry(BiomeKeys.PLAINS.getValue()).orElseThrow()
        );

        ChunkPos chunkPos = chunk.getPos();
        NbtCompound level = new NbtCompound();
        level.putInt("DataVersion", SharedConstants.getGameVersion().dataVersion().id());
        level.putInt("xPos", chunkPos.x);
        level.putInt("yPos", chunk.getBottomSectionCoord());
        level.putInt("zPos", chunkPos.z);
        level.putBoolean("isLightOn", true);
        level.putString("Status", "full");

        ChunkSection[] chunkSections = chunk.getSectionArray();
        NbtList sectionsTag = new NbtList();

        for (int y = lightingProvider.getBottomY(); y < lightingProvider.getTopY(); y++) {
            boolean empty = true;

            NbtCompound sectionTag = new NbtCompound();
            sectionTag.putByte("Y", (byte) y);

            int i = chunk.sectionCoordToIndex(y);
            ChunkSection chunkSection = i >= 0 && i < chunkSections.length ? chunkSections[i] : null;
            if (chunkSection != null) {
                sectionTag.put("block_states", BLOCK_CODEC.encodeStart(NbtOps.INSTANCE, chunkSection.getBlockStateContainer()).getOrThrow());
                sectionTag.put("biomes", biomeCodec.encodeStart(NbtOps.INSTANCE, chunkSection.getBiomeContainer()).getOrThrow());
                empty = false;
            }

            ChunkNibbleArray blockLight = chunk instanceof FakeChunk fakeChunk
                    ? fakeChunk.blockLight[i + 1]
                    : lightingProvider.get(LightType.BLOCK).getLightSection(ChunkSectionPos.from(chunkPos, y));
            if (blockLight != null && !blockLight.isUninitialized()) {
                sectionTag.putByteArray("BlockLight", blockLight.asByteArray());
                empty = false;
            }

            ChunkNibbleArray skyLight = chunk instanceof FakeChunk fakeChunk
                    ? fakeChunk.skyLight[i + 1]
                    : lightingProvider.get(LightType.SKY).getLightSection(ChunkSectionPos.from(chunkPos, y));
            if (skyLight != null && !skyLight.isUninitialized()) {
                sectionTag.putByteArray("SkyLight", skyLight.asByteArray());
                empty = false;
            }

            if (!empty) {
                sectionsTag.add(sectionTag);
            }
        }

        level.put("sections", sectionsTag);

        NbtList blockEntitiesTag;
        if (chunk instanceof FakeChunk fakeChunk) {
            blockEntitiesTag = fakeChunk.serializedBlockEntities;
        } else {
            blockEntitiesTag = new NbtList();
            for (BlockPos pos : chunk.getBlockEntityPositions()) {
                NbtCompound blockEntityTag = chunk.getPackedBlockEntityNbt(pos, registryManager);
                if (blockEntityTag != null) {
                    blockEntitiesTag.add(blockEntityTag);
                }
            }
        }
        level.put("block_entities", blockEntitiesTag);

        NbtCompound heightmapsTag = new NbtCompound();
        for (Map.Entry<Heightmap.Type, Heightmap> entry : chunk.getHeightmaps()) {
            if (chunk.getStatus().getHeightmapTypes().contains(entry.getKey())) {
                heightmapsTag.put(entry.getKey().getId(), new NbtLongArray(entry.getValue().asLongArray()));
            }
        }
        level.put("Heightmaps", heightmapsTag);

        return level;
    }

    // Note: This method is called asynchronously, so any methods called must either be verified to be thread safe (and
    //       must be unlikely to loose that thread safety in the presence of third party mods) or must be delayed
    //       by moving them into the returned supplier which is executed on the main thread.
    //       For performance reasons though: The more stuff we can do async, the better.
    public static Pair<WorldChunk, Supplier<WorldChunk>> deserialize(ChunkPos pos, NbtCompound level, World world) {
        ChunkPos chunkPos = new ChunkPos(level.getInt("xPos", 0), level.getInt("zPos", 0));
        if (!Objects.equals(pos, chunkPos)) {
            LOGGER.error("Chunk file at {} is in the wrong location; relocating. (Expected {}, got {})", pos, pos, chunkPos);
        }

        Registry<Biome> biomeRegistry = world.getRegistryManager().getOrThrow(RegistryKeys.BIOME);
        Codec<PalettedContainer<RegistryEntry<Biome>>> biomeCodec = PalettedContainer.createPalettedContainerCodec(
                biomeRegistry.getEntryCodec(),
                PaletteProvider.forBiomes(biomeRegistry.getIndexedEntries()),
                biomeRegistry.getEntry(BiomeKeys.PLAINS.getValue()).orElseThrow()
        );

        NbtList sectionsTag = level.getListOrEmpty("sections");
        ChunkSection[] chunkSections = new ChunkSection[world.countVerticalSections()];
        ChunkNibbleArray[] blockLight = new ChunkNibbleArray[chunkSections.length + 2];
        ChunkNibbleArray[] skyLight = new ChunkNibbleArray[chunkSections.length + 2];

        Arrays.fill(blockLight, COMPLETELY_DARK);

        for (int i = 0; i < sectionsTag.size(); i++) {
            Optional<NbtCompound> maybeSectionTag = sectionsTag.getCompound(i);
            if (maybeSectionTag.isEmpty()) continue;
            NbtCompound sectionTag = maybeSectionTag.get();
            int y = sectionTag.getByte("Y", (byte) 0);
            int yIndex = world.sectionCoordToIndex(y);

            if (yIndex < -1 || yIndex > chunkSections.length) {
                // See Bobby's own comment history for why this out-of-bounds case is real and not
                // just defensive paranoia - a past chunk-coordinate-scaling bug plus world-height
                // conversion can both legitimately produce it.
                continue;
            }

            if (yIndex >= 0 && yIndex < chunkSections.length) {
                PalettedContainer<BlockState> blocks = sectionTag
                        .getCompound("block_states")
                        .map(tag -> BLOCK_CODEC.parse(NbtOps.INSTANCE, tag)
                                .promotePartial((errorMessage) -> logRecoverableError(chunkPos, y, errorMessage))
                                .getOrThrow())
                        .orElseGet(() -> new PalettedContainer<>(Blocks.AIR.getDefaultState(), PaletteProvider.forBlockStates(Block.STATE_IDS)));

                PalettedContainer<RegistryEntry<Biome>> biomes = sectionTag
                        .getCompound("biomes")
                        .map(tag -> biomeCodec.parse(NbtOps.INSTANCE, tag)
                                .promotePartial((errorMessage) -> logRecoverableError(chunkPos, y, errorMessage))
                                .getOrThrow())
                        .orElseGet(() -> new PalettedContainer<>(biomeRegistry.getEntry(BiomeKeys.PLAINS.getValue()).orElseThrow(), PaletteProvider.forBiomes(biomeRegistry.getIndexedEntries())));

                ChunkSection chunkSection = new ChunkSection(blocks, biomes);
                chunkSection.calculateCounts();
                if (!chunkSection.isEmpty()) {
                    chunkSections[yIndex] = chunkSection;
                }
            }

            blockLight[yIndex + 1] = sectionTag.getByteArray("BlockLight")
                    .map(ChunkNibbleArray::new)
                    .orElse(null);

            skyLight[yIndex + 1] = sectionTag.getByteArray("SkyLight")
                    .map(ChunkNibbleArray::new)
                    .orElse(null);
        }

        // Not all light sections are stored. For block light we simply fall back to a completely dark section.
        // For sky light we need to compute the section based on those above it. We are going top to bottom section.

        // The nearest section data read from storage
        ChunkNibbleArray fullSectionAbove = null;
        // The nearest section data computed from the one above (based on its bottom-most layer).
        // May be re-used for multiple sections once computed.
        ChunkNibbleArray inferredSection = COMPLETELY_LIT;
        for (int y = skyLight.length - 1; y >= 0; y--) {
            ChunkNibbleArray section = skyLight[y];

            // If we found a section, invalidate our inferred section cache and store it for later
            if (section != null) {
                inferredSection = null;
                fullSectionAbove = section;
                continue;
            }

            // If we are missing a section, infer it from the previous full section (the result of that can be re-used)
            if (inferredSection == null) {
                assert fullSectionAbove != null; // we only clear the cache when we set this
                inferredSection = floodSkylightFromAbove(fullSectionAbove);
            }
            skyLight[y] = inferredSection;
        }

        FakeChunk chunk = new FakeChunk(world, pos, chunkSections);

        NbtCompound heightmapsTag = level.getCompoundOrEmpty("Heightmaps");
        EnumSet<Heightmap.Type> missingHeightmapTypes = EnumSet.noneOf(Heightmap.Type.class);

        for (Heightmap.Type type : chunk.getStatus().getHeightmapTypes()) {
            String key = type.getId();
            Optional<long[]> maybeTag = heightmapsTag.getLongArray(key);
            if (maybeTag.isPresent()) {
                chunk.setHeightmap(type, maybeTag.get());
            } else {
                missingHeightmapTypes.add(type);
            }
        }

        Heightmap.populateHeightmaps(chunk, missingHeightmapTypes);

        if (!ViewDistanceModule.getInstance().isSkipBlockEntities()) {
            level.getList("block_entities")
                    .stream()
                    .flatMap(NbtList::streamCompounds)
                    .forEach(chunk::addPendingBlockEntityNbt);
        }

        return Pair.of(chunk, loadChunk(chunk, blockLight, skyLight));
    }

    private static Supplier<WorldChunk> loadChunk(
            FakeChunk chunk,
            ChunkNibbleArray[] blockLight,
            ChunkNibbleArray[] skyLight
    ) {
        return () -> {
            ChunkPos pos = chunk.getPos();
            World world = chunk.getWorld();
            ChunkSection[] chunkSections = chunk.getSectionArray();

            boolean hasSkyLight = world.getDimension().hasSkyLight();
            ChunkManager chunkManager = world.getChunkManager();
            LightingProvider lightingProvider = chunkManager.getLightingProvider();
            LightingProviderExt lightingProviderExt = LightingProviderExt.get(lightingProvider);
            ChunkLightProviderExt blockLightProvider = ChunkLightProviderExt.get(lightingProvider.get(LightType.BLOCK));
            ChunkLightProviderExt skyLightProvider = ChunkLightProviderExt.get(lightingProvider.get(LightType.SKY));

            lightingProviderExt.solstice$enabledColumn(ChunkSectionPos.withZeroY(pos.x, pos.z));

            for (int i = -1; i < chunkSections.length + 1; i++) {
                int y = world.sectionIndexToCoord(i);
                if (blockLightProvider != null) {
                    blockLightProvider.solstice$addSectionData(ChunkSectionPos.from(pos, y).asLong(), blockLight[i + 1]);
                }
                if (skyLightProvider != null && hasSkyLight) {
                    skyLightProvider.solstice$addSectionData(ChunkSectionPos.from(pos, y).asLong(), skyLight[i + 1]);
                }
            }

            chunk.setTainted(ViewDistanceModule.getInstance().isTaintFakeChunks());

            // MC lazily loads block entities when they are first accessed.
            // It does so in a thread-unsafe way though, so if they are first accessed from e.g. a render thread, this
            // will cause threading issues. To work around this, force all block entities to be initialized
            // immediately, before any other code gets access to the chunk.
            for (BlockPos blockPos : chunk.getBlockEntityPositions()) {
                chunk.getBlockEntity(blockPos);
            }

            return chunk;
        };
    }

    // This method is called before the original chunk is unloaded and needs to return a supplier
    // that can be called after the chunk has been unloaded to load a fake chunk in its place.
    // It also returns a fake chunk immediately that isn't loaded into the game (yet) but can safely
    // be serialized on another thread.
    public static Pair<WorldChunk, Supplier<WorldChunk>> shallowCopy(WorldChunk original) {
        World world = original.getWorld();
        ChunkPos chunkPos = original.getPos();

        ChunkSection[] chunkSections = original.getSectionArray();

        ChunkNibbleArray[] blockLight = new ChunkNibbleArray[chunkSections.length + 2];
        ChunkNibbleArray[] skyLight = new ChunkNibbleArray[chunkSections.length + 2];
        LightingProvider lightingProvider = world.getChunkManager().getLightingProvider();
        LightData initialLightData = WorldChunkExt.get(original).solstice$getInitialLightData();
        if (initialLightData != null) {
            Iterator<byte[]> blockNibbles = initialLightData.getBlockNibbles().iterator();
            Iterator<byte[]> skyNibbles = initialLightData.getSkyNibbles().iterator();
            for (int y = lightingProvider.getBottomY(), i = 0; y < lightingProvider.getTopY(); y++, i++) {
                boolean hasBlockData = initialLightData.getInitedBlock().get(i);
                boolean isBlockZero = initialLightData.getUninitedBlock().get(i);
                if (hasBlockData || isBlockZero) {
                    blockLight[i] = hasBlockData ? new ChunkNibbleArray(blockNibbles.next().clone()) : new ChunkNibbleArray();
                }
                boolean hasSkyData = initialLightData.getInitedSky().get(i);
                boolean isSkyZero = initialLightData.getUninitedSky().get(i);
                if (hasSkyData || isSkyZero) {
                    skyLight[i] = hasSkyData ? new ChunkNibbleArray(skyNibbles.next().clone()) : new ChunkNibbleArray();
                }
            }
        } else {
            for (int y = lightingProvider.getBottomY(), i = 0; y < lightingProvider.getTopY(); y++, i++) {
                blockLight[i] = lightingProvider.get(LightType.BLOCK).getLightSection(ChunkSectionPos.from(chunkPos, y));
                skyLight[i] = lightingProvider.get(LightType.SKY).getLightSection(ChunkSectionPos.from(chunkPos, y));
            }
        }

        FakeChunk fake = new FakeChunk(world, chunkPos, chunkSections);
        fake.blockLight = blockLight;
        fake.skyLight = skyLight;

        for (Map.Entry<Heightmap.Type, Heightmap> entry : original.getHeightmaps()) {
            fake.setHeightmap(entry.getKey(), entry.getValue());
        }

        boolean skipBlockEntities = ViewDistanceModule.getInstance().isSkipBlockEntities();
        NbtList blockEntitiesTag = new NbtList();
        for (BlockPos pos : original.getBlockEntityPositions()) {
            NbtCompound blockEntityTag = original.getPackedBlockEntityNbt(pos, world.getRegistryManager());
            if (blockEntityTag != null) {
                blockEntitiesTag.add(blockEntityTag);
                if (!skipBlockEntities) {
                    fake.addPendingBlockEntityNbt(blockEntityTag);
                }
            }
        }
        fake.serializedBlockEntities = blockEntitiesTag;

        return Pair.of(fake, loadChunk(fake, blockLight, skyLight));
    }

    private static ChunkNibbleArray floodSkylightFromAbove(ChunkNibbleArray above) {
        if (above.isUninitialized()) {
            return new ChunkNibbleArray();
        } else {
            byte[] aboveBytes = above.asByteArray();
            byte[] belowBytes = new byte[2048];

            // Copy the bottom-most slice from above, 16 time over
            for (int i = 0; i < 16; i++) {
                System.arraycopy(aboveBytes, 0, belowBytes, i * 128, 128);
            }

            return new ChunkNibbleArray(belowBytes);
        }
    }

    private static void logRecoverableError(ChunkPos chunkPos, int y, String message) {
        LOGGER.error("Recoverable errors when loading section [" + chunkPos.x + ", " + y + ", " + chunkPos.z + "]: " + message);
    }
}
