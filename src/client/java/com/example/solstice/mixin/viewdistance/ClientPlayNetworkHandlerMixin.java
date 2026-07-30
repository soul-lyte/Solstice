package com.example.solstice.mixin.viewdistance;

import com.example.solstice.viewdistance.FakeChunk;
import com.example.solstice.viewdistance.ext.ClientPlayNetworkHandlerExt;
import com.example.solstice.viewdistance.ext.WorldChunkExt;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.LightData;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ported from Johni0702/bobby (LGPL-3.0-only, see NOTICE.md).
 *
 * <p>Vanilla doesn't actually process a chunk's light data until the next
 * frame (or up to several frames later, if many chunks are queued) - so if
 * that chunk unloads before then, retention has nothing to capture its light
 * from. This synchronously stashes the packet's own light data on the chunk
 * the moment it arrives ({@link WorldChunkExt}, implemented by {@code
 * WorldChunkMixin}), so it's available regardless of vanilla's own
 * processing delay.</p>
 *
 * <p>The second half handles a genuinely subtle ordering problem: vanilla's
 * chunk mesh builder is not thread-safe with respect to light data - if a
 * rebuild task is already running (common, due to neighbor updates) when a
 * retained chunk's shadow light data gets cleared, the builder may read
 * mid-clear data on its own thread and render a chunk as partially black.
 * Vanilla's own light-load queue can't be relied on to run before or after
 * that clear in a fixed order, so instead of queuing separately, this merges
 * "clear the old shadow light" and "vanilla's real light load" into a single
 * queued runnable, guaranteeing the order.</p>
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin implements ClientPlayNetworkHandlerExt {
    @Shadow private ClientWorld world;

    @Inject(method = "onChunkData", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;loadChunk(IILnet/minecraft/network/packet/s2c/play/ChunkData;)V", shift = At.Shift.AFTER))
    private void solstice$storeInitialLightData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        int chunkX = packet.getChunkX();
        int chunkZ = packet.getChunkZ();
        WorldChunk chunk = this.world.getChunkManager().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
        if (chunk == null || chunk instanceof FakeChunk) {
            return; // failed to load, ignore
        }
        WorldChunkExt.get(chunk).solstice$setInitialLightData(packet.getLightData());
    }

    // Once vanilla has actually loaded the real light data, the manually-kept copy can be dropped.
    @Inject(method = "readLightData", at = @At("HEAD"))
    private void solstice$clearInitialLightData(int chunkX, int chunkZ, LightData data, boolean rebuildChunks, CallbackInfo ci) {
        WorldChunk chunk = this.world.getChunkManager().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
        if (chunk == null || chunk instanceof FakeChunk) {
            return; // already unloaded, nothing to do
        }
        WorldChunkExt.get(chunk).solstice$setInitialLightData(null);
    }

    @Unique
    private Runnable solstice$queuedUnloadFakeLightDataTask;

    @Override
    public void solstice$queueUnloadFakeLightDataTask(Runnable runnable) {
        if (solstice$queuedUnloadFakeLightDataTask != null) {
            // Not consumed by solstice$addUnloadFakeLightDataTask below for some reason - run it now
            // rather than silently leak the light data.
            solstice$queuedUnloadFakeLightDataTask.run();
        }
        solstice$queuedUnloadFakeLightDataTask = runnable;
    }

    @ModifyArg(method = "onChunkData", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;enqueueChunkUpdate(Ljava/lang/Runnable;)V"))
    private Runnable solstice$addUnloadFakeLightDataTask(Runnable vanillaLoadLightDataTask) {
        if (solstice$queuedUnloadFakeLightDataTask != null) {
            Runnable unloadTask = solstice$queuedUnloadFakeLightDataTask;
            solstice$queuedUnloadFakeLightDataTask = null;
            return () -> {
                unloadTask.run();
                vanillaLoadLightDataTask.run();
            };
        }
        return vanillaLoadLightDataTask;
    }
}
