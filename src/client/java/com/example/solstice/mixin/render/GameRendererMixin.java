package com.example.solstice.mixin.render;

import com.example.solstice.performance.render.RenderModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into the main render loop:
 * - Sleeps the render thread when the window is minimized.
 * - Feeds the current FPS into {@link RenderModule} for dynamic render-distance.
 *
 * <p>Target signature is {@code render(RenderTickCounter, boolean)} - this
 * replaced the older {@code render(float, long, boolean)} several versions
 * before 1.21.11. Neither injected method reads the tick-counter value; it
 * is present only so the parameter list matches the real method for Mixin
 * to find it.</p>
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow private MinecraftClient client;

    @Inject(method = "render", at = @At("HEAD"))
    private void solstice$onRenderHead(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        if (!RenderModule.getInstance().isEnabled()) return;
        if (client.isWindowFocused()) return;

        try {
            Thread.sleep(RenderModule.minimizedSleepMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void solstice$onRenderTail(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        if (!RenderModule.getInstance().isEnabled()) return;
        RenderModule.getInstance().onFrameRendered(client, client.getCurrentFps());
    }
}
