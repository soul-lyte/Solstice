package com.example.solstice.mixin.zoom;

import com.example.solstice.qol.zoom.ZoomModule;
import net.minecraft.client.Mouse;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets scrolling adjust the zoom amount while the zoom key is held, instead
 * of also scrolling the hotbar. Fabric API's own hotbar-scroll event
 * (checked directly against this project's actual dependency jar, not just
 * a reference checkout) doesn't cover this Minecraft version, so this hooks
 * vanilla's raw scroll callback directly and only consumes it while zoom is
 * actually active - normal scroll behavior is untouched otherwise.
 */
@Mixin(Mouse.class)
public abstract class MouseScrollMixin {

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void solstice$onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        ZoomModule zoom = ZoomModule.getInstance();
        if (!zoom.isEnabled() || !zoom.isKeyHeld()) return;
        if (MinecraftClient.getInstance().currentScreen != null) return;

        zoom.adjustZoomDivisor(vertical);
        ci.cancel();
    }
}
