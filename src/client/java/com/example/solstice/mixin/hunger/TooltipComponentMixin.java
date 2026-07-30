package com.example.solstice.mixin.hunger;

import com.example.solstice.qol.hunger.FoodValueTooltipComponent;
import com.example.solstice.qol.hunger.FoodValueTooltipData;
import com.example.solstice.qol.shulker.ShulkerTooltipComponent;
import com.example.solstice.qol.shulker.ShulkerTooltipData;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.tooltip.TooltipData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code TooltipComponent.of(TooltipData)} dispatches on the real type of its
 * argument via a Java pattern-match switch over vanilla's own known
 * {@link TooltipData} implementations (bundle contents, player profiles,
 * ...) - neither {@link FoodValueTooltipData} nor {@link ShulkerTooltipData}
 * are among them, so without this injection they'd fall through to that
 * switch's default case and throw. Intercepting at HEAD sidesteps the
 * switch entirely for both of our own types.
 */
@Mixin(TooltipComponent.class)
public interface TooltipComponentMixin {

    @Inject(method = "of(Lnet/minecraft/item/tooltip/TooltipData;)Lnet/minecraft/client/gui/tooltip/TooltipComponent;",
            at = @At("HEAD"), cancellable = true)
    private static void solstice$foodValueTooltip(TooltipData data, CallbackInfoReturnable<TooltipComponent> cir) {
        if (data instanceof FoodValueTooltipData food) {
            cir.setReturnValue(new FoodValueTooltipComponent(food));
        } else if (data instanceof ShulkerTooltipData shulker) {
            cir.setReturnValue(new ShulkerTooltipComponent(shulker));
        }
    }
}
