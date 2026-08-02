package com.example.solstice.mixin.shulker;

import com.example.solstice.qol.shulker.ShulkerBoxTooltipModule;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures the real {@link ItemStack} into {@link ShulkerBoxTooltipModule}
 * right before vanilla dispatches into whichever component's {@code
 * appendTooltip} - see {@code ContainerComponentAppendTooltipMixin} and the
 * module's own Javadoc on {@code currentTooltipStack} for why this capture
 * is needed at all: {@code ContainerComponent.appendTooltip} has no way to
 * ask "which item am I on" by itself.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackAppendComponentTooltipMixin {

    @Inject(method = "appendComponentTooltip", at = @At("HEAD"), require = 1)
    private void solstice$captureCurrentStack(CallbackInfo ci) {
        ShulkerBoxTooltipModule.getInstance().setCurrentTooltipStack((ItemStack) (Object) this);
    }
}
