package com.example.solstice.mixin.shulker;

import com.example.solstice.qol.shulker.ShulkerBoxTooltipModule;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
 *
 * <p>Empty containers are a special case, per explicit spec: an empty
 * shulker box shows a light grey "Empty" (not the white "Contains 0
 * Stacks" summary non-empty ones get) and never shows the preview hint
 * (nothing to preview); every other empty container (chest, hopper,
 * barrel, etc.) says nothing at all - no summary, no hint. A non-empty
 * container of any kind behaves identically to a non-empty shulker box.
 * "Is this a shulker box" comes from {@link
 * ShulkerBoxTooltipModule#isCurrentStackShulkerBox()}, since {@code
 * ContainerComponent} itself has no reference back to the item it belongs
 * to - see {@code ItemStackAppendComponentTooltipMixin}.</p>
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
        if (stackCount == 0) {
            if (module.isCurrentStackShulkerBox()) {
                textConsumer.accept(Text.literal("Empty").formatted(Formatting.GRAY));
            }
            // Every other empty container (chest, hopper, barrel, ...) says nothing at all.
            ci.cancel();
            return;
        }

        textConsumer.accept(Text.literal(stackCount == 1 ? "Contains 1 Stack" : "Contains " + stackCount + " Stacks"));
        textConsumer.accept(module.getHoverHintText());
        ci.cancel();
    }
}
