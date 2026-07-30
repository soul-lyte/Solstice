package com.example.solstice.mixin.viewdistance;

import com.example.solstice.viewdistance.FakeChunkManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The core of {@link com.example.solstice.viewdistance.ViewDistanceModule}:
 * captures a chunk into {@link FakeChunkManager} just before it really
 * unloads, and resurfaces it through {@code getChunk} when vanilla's own
 * lookup comes back empty. Deliberately does NOT cancel {@code unload} -
 * vanilla's own {@code ClientChunkMap} bookkeeping must run normally, since
 * its backing array reuses the same slot for a different position as the
 * player moves regardless of whether unload ran (see {@code FakeChunkManager}'s
 * own Javadoc for why the first attempt at this got that wrong).
 */
@Mixin(ClientChunkManager.class)
public abstract class ClientChunkManagerMixin {

    @Inject(method = "unload", at = @At("HEAD"))
    private void solstice$onUnload(ChunkPos pos, CallbackInfo ci) {
        ClientChunkManager self = (ClientChunkManager) (Object) this;
        WorldChunk chunk = self.getChunk(pos.x, pos.z, ChunkStatus.FULL, false);
        if (chunk == null || chunk instanceof EmptyChunk) return;

        FakeChunkManager.getInstance().onChunkUnloading(pos, chunk, MinecraftClient.getInstance());
    }

    @Inject(method = "getChunk", at = @At("RETURN"), cancellable = true)
    private void solstice$onGetChunk(int x, int z, ChunkStatus status, boolean create,
                                      CallbackInfoReturnable<WorldChunk> cir) {
        // Vanilla returns EmptyChunk for an in-radius-but-currently-unloaded position
        // (create=true), but plain null for a position outside ClientChunkMap's own
        // radius entirely (create=false, confirmed via ClientChunkManager.getChunk's
        // isInRadius() gate) - which is exactly the case for anything in the extended
        // ring once BuiltChunkStorageMixin lets the renderer actually query that far.
        // Both must be treated as "nothing real here, try our retained copy."
        WorldChunk existing = cir.getReturnValue();
        if (existing != null && !(existing instanceof EmptyChunk)) return;

        WorldChunk retained = FakeChunkManager.getInstance().getRetained(x, z);
        if (retained != null) {
            cir.setReturnValue(retained);
        }
    }
}
