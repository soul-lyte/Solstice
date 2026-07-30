package com.example.solstice.mixin.viewdistance;

import com.example.solstice.viewdistance.FakeChunkManager;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drives {@link FakeChunkManager}'s per-tick eviction check. Separate
 * package/class from the existing {@code mixin.memory.MinecraftClientMixin}
 * - Mixin allows multiple distinct classes to target the same class, already
 * the pattern used for {@code GameRenderer} in this project.
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void solstice$onTick(CallbackInfo ci) {
        FakeChunkManager.getInstance().tick((MinecraftClient) (Object) this);
    }
}
