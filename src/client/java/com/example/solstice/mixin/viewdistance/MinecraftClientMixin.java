package com.example.solstice.mixin.viewdistance;

import com.example.solstice.viewdistance.FakeChunkManager;
import com.example.solstice.viewdistance.FakeChunkStorage;
import com.example.solstice.viewdistance.ext.ClientChunkManagerExt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Util;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ported from Johni0702/bobby (LGPL-3.0-only, see NOTICE.md) - drives
 * {@link FakeChunkManager}'s load/unload/save queue once per rendered frame,
 * budgeted to a small fraction of the frame's own time so it never causes a
 * noticeable stutter, instead of the old implementation's unbudgeted
 * once-per-tick call.
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow @Final public GameOptions options;

    @Shadow @Nullable public ClientWorld world;

    @Inject(method = "render", at = @At(value = "CONSTANT", args = "stringValue=tick"))
    private void solstice$update(CallbackInfo ci) {
        if (world == null) {
            return;
        }
        FakeChunkManager chunkManager = ((ClientChunkManagerExt) world.getChunkManager()).solstice$getFakeChunkManager();
        if (chunkManager == null) {
            return;
        }

        Profiler profiler = Profilers.get();
        profiler.push("solsticeViewDistanceUpdate");

        int maxFps = options.getMaxFps().getValue();
        long frameTime = 1_000_000_000 / (maxFps == GameOptions.MAX_FPS_LIMIT ? 120 : maxFps);
        // Arbitrarily choosing 1/4 of frame time as our max budget, that way we're hopefully not noticeable.
        long frameBudget = frameTime / 4;
        long timeLimit = Util.getMeasuringTimeNano() + frameBudget;
        chunkManager.update(false, () -> Util.getMeasuringTimeNano() < timeLimit);

        profiler.pop();
    }

    @Inject(method = "onDisconnected", at = @At("RETURN"))
    private void solstice$close(CallbackInfo ci) {
        FakeChunkStorage.closeAll();
    }
}
