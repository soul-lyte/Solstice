package com.example.solstice.mixin.crosshair;

import com.example.solstice.qol.crosshair.CrosshairModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Leaves vanilla's crosshair completely untouched unless {@link CrosshairModule}
 * is enabled AND the player is currently aiming at an entity within reach -
 * only then is vanilla's draw cancelled and the chosen style drawn instead.
 */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    private static final Identifier VANILLA_CROSSHAIR = Identifier.ofVanilla("hud/crosshair");
    private static final Identifier CUSTOM_CROSSHAIR = Identifier.of("solstice", "textures/crosshair/custom.png");

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void solstice$onRenderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        CrosshairModule module = CrosshairModule.getInstance();
        if (!module.isEnabled()) return;
        if (!(MinecraftClient.getInstance().crosshairTarget instanceof EntityHitResult)) return;

        ci.cancel();

        if (CrosshairModule.styleIndex == 1) {
            // custom.png is 15x15 (an odd size, deliberately - it centers the crosshair
            // exactly on a single pixel rather than a half-pixel boundary), not 16x16 -
            // the draw size/UV bounds below must match its real dimensions or the image
            // samples past its own edge and looks stretched/clipped.
            int cx = context.getScaledWindowWidth() / 2 - 7;
            int cy = context.getScaledWindowHeight() / 2 - 7;
            context.drawTexture(RenderPipelines.GUI_TEXTURED, CUSTOM_CROSSHAIR,
                    cx, cy, 0f, 0f, 15, 15, 15, 15, 0xFFFFFFFF);
        } else {
            int x = context.getScaledWindowWidth() / 2 - 8;
            int y = context.getScaledWindowHeight() / 2 - 8;
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, VANILLA_CROSSHAIR, x, y, 16, 16, module.getTintColor());
        }
    }
}
