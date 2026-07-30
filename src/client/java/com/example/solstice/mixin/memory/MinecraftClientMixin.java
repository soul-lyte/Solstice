package com.example.solstice.mixin.memory;

import com.example.solstice.performance.memory.MemoryModule;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into the client tick to drive {@link MemoryModule#onTick()}.
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void solstice$onTick(CallbackInfo ci) {
        MemoryModule.getInstance().onTick();
    }
}
