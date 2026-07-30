package com.example.solstice.mixin.viewdistance;

import com.example.solstice.viewdistance.ext.ChunkLightProviderExt;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ported from Johni0702/bobby (LGPL-3.0-only, see NOTICE.md). Companion to
 * {@link LightingProviderMixin} - {@code isLightingEnabled} alone only says
 * "trust this position", it doesn't supply the actual light values. This
 * holds Solstice's own separate per-section light data (captured when a real
 * chunk is retained, or loaded from disk) and returns it directly for any
 * position it knows about, again fully bypassing the real light engine's own
 * storage rather than trying to inject data into it.
 */
@Mixin(value = ChunkLightProvider.class)
public abstract class ChunkLightProviderMixin implements ChunkLightProviderExt {
    private final Long2ObjectMap<ChunkNibbleArray> solstice$sectionData = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());
    private final Long2ObjectMap<ChunkNibbleArray> solstice$originalSectionData = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());

    @Override
    public void solstice$addSectionData(long pos, ChunkNibbleArray data) {
        this.solstice$sectionData.put(pos, data);
        this.solstice$originalSectionData.remove(pos);
    }

    @Override
    public void solstice$removeSectionData(long pos) {
        this.solstice$sectionData.remove(pos);
        this.solstice$originalSectionData.remove(pos);
    }

    /** Temporarily dims/brightens a section's light by {@code delta} for the "Taint Fake Chunks" setting, or restores it when {@code delta == 0}. */
    @Override
    public void solstice$setTainted(long pos, int delta) {
        if (delta != 0) {
            ChunkNibbleArray original = this.solstice$originalSectionData.get(pos);
            if (original == null) {
                original = this.solstice$sectionData.get(pos);
                if (original == null) {
                    return;
                }
                this.solstice$originalSectionData.put(pos, original);
            }

            ChunkNibbleArray updated = new ChunkNibbleArray();

            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        updated.set(x, y, z, Math.min(Math.max(original.get(x, y, z) + delta, 0), 15));
                    }
                }
            }

            this.solstice$sectionData.put(pos, updated);
        } else {
            ChunkNibbleArray original = this.solstice$originalSectionData.remove(pos);
            if (original == null) {
                return;
            }
            solstice$sectionData.put(pos, original);
        }
    }

    @Inject(method = "getLightSection", at = @At("HEAD"), cancellable = true)
    private void solstice$getLightSection(ChunkSectionPos pos, CallbackInfoReturnable<ChunkNibbleArray> ci) {
        ChunkNibbleArray data = this.solstice$sectionData.get(pos.asLong());
        if (data != null) {
            ci.setReturnValue(data);
        }
    }

    @Inject(method = "getLightLevel", at = @At("HEAD"), cancellable = true)
    private void solstice$getLightLevel(BlockPos blockPos, CallbackInfoReturnable<Integer> ci) {
        ChunkNibbleArray data = this.solstice$sectionData.get(ChunkSectionPos.from(blockPos).asLong());
        if (data != null) {
            ci.setReturnValue(data.get(
                    ChunkSectionPos.getLocalCoord(blockPos.getX()),
                    ChunkSectionPos.getLocalCoord(blockPos.getY()),
                    ChunkSectionPos.getLocalCoord(blockPos.getZ())
            ));
        }
    }
}
