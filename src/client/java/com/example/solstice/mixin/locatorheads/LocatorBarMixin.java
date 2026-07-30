package com.example.solstice.mixin.locatorheads;

import com.example.solstice.qol.locatorheads.LocatorHeadsModule;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.bar.LocatorBar;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.resource.waypoint.WaypointStyleAsset;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
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
 *
 * <p>Heads also shrink toward the camera the farther away a player is,
 * matching vanilla's own locator-bar dots (which swap to a smaller sprite
 * variant past the waypoint style's far distance) - see {@link
 * #solstice$distanceScale}, also ported from the reference mod.</p>
 */
@Mixin(LocatorBar.class)
public abstract class LocatorBarMixin {

    private static final String TARGET = "method_70870";
    private static final String DRAW_ICON = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIIII)V";

    /** Smallest a head ever shrinks to at/past the style's far distance, in pixels - matches the reference mod's own floor. */
    private static final float MIN_HEAD_SIZE = 5f;

    @Unique
    private TrackedWaypoint solstice$capturedWaypoint;
    @Unique
    private Entity solstice$capturedEntity;

    @Inject(method = TARGET, at = @At(value = "INVOKE", target = DRAW_ICON, shift = At.Shift.BEFORE))
    private void solstice$captureWaypoint(Entity entity, World world, EntityTickProgress entityTickProgress,
                                          DrawContext context, int centerY, TrackedWaypoint waypoint, CallbackInfo ci) {
        this.solstice$capturedWaypoint = waypoint;
        this.solstice$capturedEntity = entity;
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
        float sizeMultiplier = (float) LocatorHeadsModule.headSizeMultiplier * solstice$distanceScale(w);

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

    /**
     * Ratio (0-1) shrinking the head toward {@link #MIN_HEAD_SIZE} as the player gets
     * farther away, matching vanilla's own locator-bar dots (which swap to a smaller
     * sprite variant past a style's far distance) - ported from Haage001/locator-heads'
     * own distance-lerp technique (LGPL-3.0-only, see NOTICE.md), adapted to use a
     * plain matrix scale instead of that mod's fixed-point sub-pixel workaround (not
     * needed here since the head is already drawn through a float matrix scale).
     */
    @Unique
    private float solstice$distanceScale(int iconSize) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (this.solstice$capturedWaypoint == null || this.solstice$capturedEntity == null) {
            return 1f;
        }
        WaypointStyleAsset style = client.getWaypointStyleAssetManager().get(this.solstice$capturedWaypoint.getConfig().style);
        float distance = MathHelper.sqrt((float) this.solstice$capturedWaypoint.squaredDistanceTo(this.solstice$capturedEntity));
        float range = style.farDistance() - style.nearDistance();
        float progress = range <= 0f ? 1f
                : 1f - MathHelper.clamp((distance - style.nearDistance()) / range, 0f, 1f);
        float minFraction = iconSize > 0 ? MIN_HEAD_SIZE / iconSize : 1f;
        return MathHelper.lerp(progress, minFraction, 1f);
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
