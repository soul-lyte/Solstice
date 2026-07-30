package com.example.solstice.mixin.locatorheads;

import com.example.solstice.qol.locatorheads.LocatorHeadsModule;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.bar.LocatorBar;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.waypoint.EntityTickProgress;
import net.minecraft.world.waypoint.TrackedWaypoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;

/**
 * Ported from Haage001/locator-heads (LGPL-3.0-only, see NOTICE.md) -
 * adapted onto 1.21.11's real class shapes, which differ substantially
 * from the reference mod's own Mojmap-based source (it targets a newer MC
 * version via Mojang's official mappings: {@code
 * net.minecraft.client.gui.contextualbar.LocatorBarRenderer}, {@code
 * GuiGraphics}, {@code TrackedWaypoint.id()} - Yarn's 1.21.11 equivalent is
 * {@code net.minecraft.client.gui.hud.bar.LocatorBar}, {@code DrawContext},
 * {@code TrackedWaypoint.getSource()}, confirmed via decompile).
 *
 * <p>The reference mod's own technique - capture the waypoint in one
 * injection, redirect the icon draw call in a second, both scoped to the
 * same per-waypoint render method - carries over directly. Confirmed via
 * {@code javap} that Yarn's real per-waypoint lambda body compiles to a
 * private synthetic method with a fully deterministic parameter list
 * ({@code (Entity, World, EntityTickProgress, DrawContext, int,
 * TrackedWaypoint)}, unmapped as {@code method_70870}) - the waypoint
 * itself arrives as a real, named method parameter here, not a local
 * variable requiring fragile slot-ordinal guessing.</p>
 */
@Mixin(LocatorBar.class)
public abstract class LocatorBarMixin {

    private static final String TARGET = "method_70870";
    private static final String DRAW_ICON = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIIII)V";

    @Unique
    private TrackedWaypoint solstice$capturedWaypoint;

    @Inject(method = TARGET, at = @At(value = "INVOKE", target = DRAW_ICON, shift = At.Shift.BEFORE))
    private void solstice$captureWaypoint(Entity entity, World world, EntityTickProgress entityTickProgress,
                                          DrawContext context, int centerY, TrackedWaypoint waypoint, CallbackInfo ci) {
        this.solstice$capturedWaypoint = waypoint;
    }

    @Redirect(method = TARGET, at = @At(value = "INVOKE", target = DRAW_ICON))
    private void solstice$renderHeadInsteadOfIcon(DrawContext context, RenderPipeline pipeline, Identifier originalIcon,
                                                   int x, int y, int w, int h, int color) {
        PlayerListEntry entry = LocatorHeadsModule.getInstance().isEnabled() ? solstice$resolvePlayerEntry() : null;
        if (entry == null) {
            context.drawGuiTexture(pipeline, originalIcon, x, y, w, h, color);
            return;
        }

        Identifier skin = entry.getSkinTextures().body().texturePath();
        int centerX = x + w / 2;
        int centerY = y + h / 2;
        float sizeMultiplier = (float) LocatorHeadsModule.headSizeMultiplier;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(centerX, centerY);
        context.getMatrices().scale(sizeMultiplier, sizeMultiplier);
        context.getMatrices().translate(-centerX, -centerY);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, skin, x, y, 8f, 8f, w, h, 64, 64, 0xFFFFFFFF);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, skin, x, y, 40f, 8f, w, h, 64, 64, 0xFFFFFFFF);
        context.getMatrices().popMatrix();

        if (LocatorHeadsModule.showNames) {
            MinecraftClient client = MinecraftClient.getInstance();
            String name = entry.getProfile().name();
            int textWidth = client.textRenderer.getWidth(name);
            context.drawText(client.textRenderer, name, centerX - textWidth / 2, y - 10, 0xFFFFFF, true);
        }
    }

    @Unique
    private PlayerListEntry solstice$resolvePlayerEntry() {
        if (this.solstice$capturedWaypoint == null) {
            return null;
        }
        Optional<UUID> uuid = this.solstice$capturedWaypoint.getSource().left();
        if (uuid.isEmpty()) {
            return null;
        }
        ClientPlayNetworkHandler network = MinecraftClient.getInstance().getNetworkHandler();
        if (network == null) {
            return null;
        }
        return network.getPlayerListEntry(uuid.get());
    }
}
