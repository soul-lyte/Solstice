package com.example.solstice.mixin.viewdistance;

import com.example.solstice.viewdistance.ViewDistanceModule;
import net.minecraft.client.render.BuiltChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * The real root cause of the "chunks randomly reload" report: {@link BuiltChunkStorage}
 * (the renderer's own {@code chunks[]} mesh-section array, separate from
 * {@code ClientChunkManager}'s block-data storage) is always constructed with exactly
 * {@code GameOptions.getClampedViewDistance()} - confirmed by decompiling
 * {@code WorldRenderer.reload()}, which passes that value straight into
 * {@code BuiltChunkStorage}'s constructor with no margin whatsoever. Its array is
 * {@code (viewDistance*2+1)^2} entries, indexed with wraparound the same way
 * {@code ClientChunkManager$ClientChunkMap} is (see {@code FakeChunkManager}'s own
 * Javadoc for that history) - meaning a retained chunk beyond vanilla's real view
 * distance never had a render slot to draw into at all, regardless of what
 * {@code ClientChunkManagerMixin}'s {@code getChunk} substitution returned. Data
 * retention alone could never have worked without this - it's not a tuning bug, the
 * render storage genuinely wasn't sized for the extended ring.
 *
 * <p>Fix: when {@link ViewDistanceModule} is enabled, inflate the one {@code int}
 * {@link BuiltChunkStorage#setViewDistance} receives by {@code extraChunks} before it's
 * used to size {@code sizeX}/{@code sizeZ} and stored for the wraparound index math in
 * {@code updateCameraPosition} - both derive from this same value, so inflating it here
 * keeps the array size and the indexing consistent automatically. This is the only call
 * site (from the constructor, itself only called by {@code WorldRenderer.reload()}), so
 * {@link ViewDistanceModule} explicitly triggers a {@code reload()} on enable/disable/
 * extra-chunks-change to apply this without requiring a world rejoin.</p>
 */
@Mixin(BuiltChunkStorage.class)
public abstract class BuiltChunkStorageMixin {

    @ModifyVariable(method = "setViewDistance", at = @At("HEAD"), argsOnly = true)
    private int solstice$extendViewDistance(int viewDistance) {
        ViewDistanceModule module = ViewDistanceModule.getInstance();
        if (!module.isEnabled()) return viewDistance;
        return viewDistance + ViewDistanceModule.extraChunks;
    }
}
