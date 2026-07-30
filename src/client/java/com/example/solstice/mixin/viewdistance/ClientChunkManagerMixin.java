package com.example.solstice.mixin.viewdistance;

import com.example.solstice.viewdistance.FakeChunk;
import com.example.solstice.viewdistance.FakeChunkManager;
import com.example.solstice.viewdistance.VisibleChunksTracker;
import com.example.solstice.viewdistance.ViewDistanceModule;
import com.example.solstice.viewdistance.ext.ClientChunkManagerExt;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Ported from Johni0702/bobby (LGPL-3.0-only, see NOTICE.md) - replaces
 * Solstice's own earlier from-scratch chunk-retention Mixin entirely. See
 * {@link FakeChunkManager}'s own Javadoc for the full mechanism; this class
 * is purely the vanilla integration points: substituting a retained/fake
 * chunk whenever vanilla's own lookup comes back empty, and saving a real
 * chunk's data (for retention, and for disk persistence) right before
 * vanilla actually discards it.
 */
@Mixin(ClientChunkManager.class)
public abstract class ClientChunkManagerMixin implements ClientChunkManagerExt {
    @Shadow @Final private WorldChunk emptyChunk;

    @Shadow @Nullable public abstract WorldChunk getChunk(int i, int j, ChunkStatus chunkStatus, boolean bl);
    @Shadow private static int getChunkMapRadius(int loadDistance) { throw new AssertionError(); }

    protected FakeChunkManager solstice$chunkManager;

    // Tracks which real chunks are visible (whether or not the were actually received), so we can
    // properly unload (i.e. save and replace with fake) them when the server center pos or view distance changes.
    private final VisibleChunksTracker solstice$realChunksTracker = new VisibleChunksTracker();

    // List of real chunks saved just before they are unloaded, so we can restore fake ones in their place afterwards
    private final List<Pair<Long, Supplier<WorldChunk>>> solstice$chunkReplacements = new ArrayList<>();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void solstice$init(ClientWorld world, int loadDistance, CallbackInfo ci) {
        if (ViewDistanceModule.getInstance().isEnabled()) {
            solstice$chunkManager = new FakeChunkManager(world, (ClientChunkManager) (Object) this);
            solstice$realChunksTracker.update(0, 0, getChunkMapRadius(loadDistance), null, null);
        }
    }

    @Override
    public FakeChunkManager solstice$getFakeChunkManager() {
        return solstice$chunkManager;
    }

    @Override
    public VisibleChunksTracker solstice$getRealChunksTracker() {
        return solstice$realChunksTracker;
    }

    @Inject(method = "getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/WorldChunk;", at = @At("RETURN"), cancellable = true)
    private void solstice$getChunk(int x, int z, ChunkStatus chunkStatus, boolean orEmpty, CallbackInfoReturnable<WorldChunk> ci) {
        // Did we find a live chunk?
        if (ci.getReturnValue() != (orEmpty ? emptyChunk : null)) {
            return;
        }

        if (solstice$chunkManager == null) {
            return;
        }

        // Otherwise, see if we've got one
        WorldChunk chunk = solstice$chunkManager.getChunk(x, z);
        if (chunk != null) {
            ci.setReturnValue(chunk);
        }
    }

    @Inject(method = "loadChunkFromPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientChunkManager$ClientChunkMap;getIndex(II)I"))
    private void solstice$unloadFakeChunk(int x, int z, PacketByteBuf buf, Map<Heightmap.Type, long[]> heightmaps, Consumer<ChunkData.BlockEntityVisitor> consumer, CallbackInfoReturnable<WorldChunk> cir) {
        if (solstice$chunkManager == null) {
            return;
        }

        // Needs to be called unconditionally because even if there is no chunk loaded at the moment,
        // there might already be one queued which needs cancelling, or it'll overwrite the real one later.
        solstice$chunkManager.unload(x, z, true);
    }

    @Unique
    private void solstice$saveRealChunk(long chunkPos) {
        int chunkX = ChunkPos.getPackedX(chunkPos);
        int chunkZ = ChunkPos.getPackedZ(chunkPos);

        WorldChunk chunk = getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
        if (chunk == null || chunk instanceof FakeChunk) {
            return;
        }

        Supplier<WorldChunk> copy = solstice$chunkManager.save(chunk);

        if (solstice$chunkManager.shouldBeLoaded(chunkX, chunkZ)) {
            solstice$chunkReplacements.add(Pair.of(chunkPos, copy));
        }
    }

    @Unique
    private void solstice$substituteFakeChunksForUnloadedRealOnes() {
        for (Pair<Long, Supplier<WorldChunk>> entry : solstice$chunkReplacements) {
            long chunkPos = entry.getKey();
            int chunkX = ChunkPos.getPackedX(chunkPos);
            int chunkZ = ChunkPos.getPackedZ(chunkPos);
            solstice$chunkManager.load(chunkX, chunkZ, entry.getValue().get());
        }
        solstice$chunkReplacements.clear();
    }

    @Inject(method = "unload", at = @At("HEAD"))
    private void solstice$saveChunk(ChunkPos pos, CallbackInfo ci) {
        if (solstice$chunkManager == null) {
            return;
        }

        solstice$saveRealChunk(pos.toLong());
    }

    @Inject(method = "setChunkMapCenter", at = @At("HEAD"))
    private void solstice$saveChunksBeforeMove(int x, int z, CallbackInfo ci) {
        if (solstice$chunkManager == null) {
            return;
        }

        solstice$realChunksTracker.updateCenter(x, z, this::solstice$saveRealChunk, null);
    }

    @Inject(method = "updateLoadDistance", at = @At("HEAD"))
    private void solstice$saveChunksBeforeResize(int loadDistance, CallbackInfo ci) {
        if (solstice$chunkManager == null) {
            return;
        }

        solstice$realChunksTracker.updateViewDistance(getChunkMapRadius(loadDistance), this::solstice$saveRealChunk, null);
    }

    @Inject(method = { "unload", "setChunkMapCenter", "updateLoadDistance" }, at = @At("RETURN"))
    private void solstice$substituteAfterVanillaMoved(CallbackInfo ci) {
        if (solstice$chunkManager == null) {
            return;
        }

        solstice$substituteFakeChunksForUnloadedRealOnes();
    }

    @Inject(method = "updateLoadDistance", at = @At(value = "FIELD", target = "Lnet/minecraft/client/world/ClientChunkManager;chunks:Lnet/minecraft/client/world/ClientChunkManager$ClientChunkMap;", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void solstice$reAddEmptyFakeChunks(CallbackInfo ci) {
        if (solstice$chunkManager == null) {
            return;
        }

        for (WorldChunk chunk : solstice$chunkManager.getFakeChunks()) {
            ChunkPos pos = chunk.getPos();
            solstice$chunkManager.loadEmptySectionsOfFakeChunk(pos.x, pos.z, chunk);
        }
    }

    @Inject(method = "getDebugString", at = @At("RETURN"), cancellable = true)
    private void solstice$debugString(CallbackInfoReturnable<String> cir) {
        if (solstice$chunkManager == null) {
            return;
        }

        cir.setReturnValue(cir.getReturnValue() + " " + solstice$chunkManager.getDebugString());
    }

    @Override
    public void solstice$onFakeChunkAdded(int x, int z) {
        // Vanilla polls for chunks each frame - nothing extra needed here.
    }

    @Override
    public void solstice$onFakeChunkRemoved(int x, int z, boolean willBeReplaced) {
        // Vanilla polls for chunks each frame - nothing extra needed here.
    }
}
