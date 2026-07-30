package com.example.solstice.mixin.visuals;

import com.example.solstice.qol.viewmodel.ViewModelModule;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * View Model Customization - a persistent scale/offset applied to the held
 * ITEM only, in first person.
 *
 * <p>Targets {@code HeldItemRenderer}'s private {@code renderItem(LivingEntity,
 * ItemStack, ItemDisplayContext, MatrixStack, OrderedRenderCommandQueue, int)}
 * specifically - confirmed via decompile that this is the exact call site
 * {@code renderFirstPersonItem} uses to draw the actual item model, completely
 * separate from {@code renderArmHoldingItem}/{@code renderArm} (empty hand,
 * map-in-both-hands). Wrapping the narrower method instead of the whole
 * {@code renderFirstPersonItem} (the old approach) means this transform can
 * never touch the player's arm/hand model - it literally isn't in scope here.
 * There's a second, unrelated {@code renderItem(float, MatrixStack,
 * OrderedRenderCommandQueue, ClientPlayerEntity, int)} overload in the same
 * class (the per-frame entry point for both hands) - the full descriptor
 * below is required to target the right one, plain method-name matching
 * would be ambiguous between the two.</p>
 *
 * <p>Deliberately skipped for a held shield ({@code instanceof ShieldItem}):
 * the shield's own position comes from a bundled resource pack (see {@code
 * VisualsModule.SHIELD_PACK_ID}) that's meant to be the final word regardless
 * of these settings, not stacked on top of them.</p>
 */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    @Inject(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
            at = @At("HEAD"))
    private void solstice$beforeRenderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext renderMode,
                                            MatrixStack matrices, OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        matrices.push();

        if (ViewModelModule.getInstance().isEnabled() && !(stack.getItem() instanceof ShieldItem)) {
            matrices.translate((float) ViewModelModule.offsetX,
                    (float) ViewModelModule.offsetY, (float) ViewModelModule.offsetZ);
            float scale = (float) ViewModelModule.scale;
            matrices.scale(scale, scale, scale);
        }
    }

    @Inject(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
            at = @At("TAIL"))
    private void solstice$afterRenderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext renderMode,
                                           MatrixStack matrices, OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        matrices.pop();
    }
}
