package com.example.solstice.mixin.hunger;

import com.example.solstice.qol.hunger.FoodValueTooltipData;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Attaches {@link FoodValueTooltipData} to food items so their tooltip can
 * render real hunger/saturation icons instead of plain text - see
 * {@code TooltipComponentMixin} for where that data actually gets rendered.
 * Only fills in when vanilla's own dispatch came back empty, so this can
 * never clobber a real vanilla tooltip component (bundle contents, map
 * preview, etc.) on some hypothetical item that's also food.
 */
@Mixin(Item.class)
public abstract class ItemTooltipDataMixin {

    @Inject(method = "getTooltipData", at = @At("RETURN"), cancellable = true)
    private void solstice$addFoodValueTooltip(ItemStack stack, CallbackInfoReturnable<Optional<TooltipData>> cir) {
        if (cir.getReturnValue().isPresent()) return;
        if (!stack.contains(DataComponentTypes.FOOD)) return;

        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        if (food == null) return;

        cir.setReturnValue(Optional.of(new FoodValueTooltipData(food.nutrition(), food.saturation())));
    }
}
