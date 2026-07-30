package com.example.solstice.mixin.shulker;

import com.example.solstice.qol.shulker.ShulkerBoxTooltipModule;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Replaces vanilla's own plain-text "Item xN" per-slot listing with a
 * single "Contains N Stacks" summary, plus the orange hover hint (see
 * {@link ShulkerBoxTooltipModule#getHoverHintText()}) - always, whenever
 * the module is enabled, not just while a preview key is held, since the
 * hint itself needs to be visible before any key is pressed to tell the
 * user what to press.
 */
@Mixin(ContainerComponent.class)
public abstract class ContainerComponentAppendTooltipMixin {

    @Shadow
    public abstract Stream<ItemStack> streamNonEmpty();

    @Inject(method = "appendTooltip", at = @At("HEAD"), cancellable = true, require = 1)
    private void solstice$replaceWithSummary(Item.TooltipContext context, Consumer<Text> textConsumer,
                                              TooltipType type, ComponentsAccess components, CallbackInfo ci) {
        ShulkerBoxTooltipModule module = ShulkerBoxTooltipModule.getInstance();
        if (!module.isEnabled()) return;

        long stackCount = streamNonEmpty().count();
        textConsumer.accept(Text.literal(stackCount == 1 ? "Contains 1 Stack" : "Contains " + stackCount + " Stacks"));
        textConsumer.accept(module.getHoverHintText());
        ci.cancel();
    }
}
