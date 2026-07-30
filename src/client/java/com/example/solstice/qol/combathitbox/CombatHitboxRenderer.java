package com.example.solstice.qol.combathitbox;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

import java.util.List;

/**
 * Draws a wireframe outline around nearby living entities via vanilla's own
 * Gizmo debug-draw API ({@code GizmoDrawing}) - the same technique vanilla's
 * built-in F3+B hitbox renderer now uses internally. A port of sootysplash/
 * combat-hitboxes' own outline behavior (Apache-2.0, see NOTICE.md): purely
 * a rendered overlay, never touches {@link Entity#getBoundingBox()} or any
 * real collision/reach/attack logic - the outline width setting only scales
 * the drawn line, not the entity's actual hitbox.
 *
 * <p>Turns {@link CombatHitboxModule#outlineColor} for whichever entity is
 * currently the crosshair target (vanilla's own reach-limited raycast,
 * {@code client.crosshairTarget}) - every other nearby entity gets a fixed,
 * non-configurable neutral white outline instead.</p>
 */
public final class CombatHitboxRenderer {

    private static final double RANGE = 32.0;
    private static final int DEFAULT_COLOR = 0xFFFFFFFF;

    private CombatHitboxRenderer() {}

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(CombatHitboxRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        if (!CombatHitboxModule.getInstance().isEnabled()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return;
        }

        Entity targeted = client.crosshairTarget instanceof EntityHitResult ehr ? ehr.getEntity() : null;
        float tickDelta = client.getRenderTickCounter().getTickProgress(true);

        boolean playersOnly = CombatHitboxModule.playersOnly;
        Box searchBox = player.getBoundingBox().expand(RANGE);
        List<Entity> nearby = client.world.getOtherEntities(player, searchBox,
                e -> e.isAlive() && (playersOnly ? e instanceof PlayerEntity : e instanceof LivingEntity));

        DrawStyle outlineStyle = DrawStyle.stroked(DEFAULT_COLOR, CombatHitboxModule.outlineWidth);
        DrawStyle targetStyle = DrawStyle.stroked(CombatHitboxModule.outlineColor, CombatHitboxModule.outlineWidth);

        for (Entity entity : nearby) {
            GizmoDrawing.box(lerpedBox(entity, tickDelta), entity == targeted ? targetStyle : outlineStyle);
        }
    }

    /** Builds the box at the entity's interpolated render position instead of its raw logical position, to avoid jitter. */
    private static Box lerpedBox(Entity entity, float tickDelta) {
        Vec3d pos = entity.getLerpedPos(tickDelta);
        double halfWidth = entity.getWidth() / 2.0;
        double height = entity.getHeight();
        return new Box(pos.x - halfWidth, pos.y, pos.z - halfWidth, pos.x + halfWidth, pos.y + height, pos.z + halfWidth);
    }
}
