package com.example.solstice.mixin.render;

import com.example.solstice.performance.render.EntityCullingModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Real hook for {@link EntityCullingModule} - {@code EntityRenderManager.shouldRender}
 * is the per-frame, per-entity decision point that gates both render-state
 * extraction and draw submission (confirmed via decompile of 1.21.11's real
 * {@code EntityRenderManager}, the renamed/reworked successor to the old
 * {@code EntityRenderDispatcher} this project's previous attempt targeted a
 * stale signature on). Only tightens vanilla's own frustum-based decision -
 * never turns a vanilla "false" into "true".
 */
@Mixin(EntityRenderManager.class)
public abstract class EntityRenderManagerMixin {

    @Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true)
    private void solstice$cullOccludedEntities(Entity entity, Frustum frustum, double x, double y, double z,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        if (!EntityCullingModule.getInstance().shouldRender(entity, MinecraftClient.getInstance())) {
            cir.setReturnValue(false);
        }
    }
}
