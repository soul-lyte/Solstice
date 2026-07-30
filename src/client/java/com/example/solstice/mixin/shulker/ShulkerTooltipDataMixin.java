package com.example.solstice.mixin.shulker;

import com.example.solstice.qol.shulker.ShulkerBoxTooltipModule;
import com.example.solstice.qol.shulker.ShulkerTooltipData;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.util.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

/**
 * Attaches {@link ShulkerTooltipData} to any item carrying a real {@code
 * ContainerComponent} (shulker boxes and anything else that has one) so
 * its tooltip can show an icon grid - see {@code TooltipComponentMixin}
 * for where that data actually gets rendered. Only fills in when vanilla's
 * own dispatch came back empty (same pattern as {@code
 * ItemTooltipDataMixin}'s food handling), only while the module is on, and
 * - matching MisterPeModder/ShulkerBoxTooltip's own real behavior - only
 * while one of the two preview keys is actually held. {@code
 * getTooltipData()} is re-queried live every frame while a stack is
 * hovered, so the preview appears/disappears immediately as the key is
 * pressed/released without needing to track hover state separately.
 *
 * <p>Also derives the real slot count and dye-based background color for
 * shulker boxes specifically (see {@link ShulkerTooltipData}'s Javadoc for
 * why 27 is hardcoded rather than trusting the container's own stored list
 * length) - {@code DyeColor.getEntityColor()} is the closest real-vanilla
 * analog available in this Yarn build to the original mod's own {@code
 * getTextureDiffuseColor()} (which doesn't exist here), matching the
 * spirit (a representative RGB swatch per dye) closely enough.</p>
 */
@Mixin(Item.class)
public abstract class ShulkerTooltipDataMixin {

    private static final int SHULKER_BOX_SLOTS = 27;
    private static final int UNDYED_SHULKER_BOX_COLOR = 0xFF976797;
    private static final int DEFAULT_BACKGROUND_COLOR = 0xFFFFFFFF;

    @Inject(method = "getTooltipData", at = @At("RETURN"), cancellable = true)
    private void solstice$addShulkerTooltip(ItemStack stack, CallbackInfoReturnable<Optional<TooltipData>> cir) {
        ShulkerBoxTooltipModule module = ShulkerBoxTooltipModule.getInstance();
        if (!module.isEnabled()) return;
        if (cir.getReturnValue().isPresent()) return;

        boolean compactHeld = module.isPreviewKeyHeld();
        boolean modifierHeld = module.isFullPreviewKeyHeld();
        boolean full = compactHeld && modifierHeld;
        boolean compact = compactHeld && !modifierHeld;
        if (!full && !compact) return;

        if (!stack.contains(DataComponentTypes.CONTAINER)) return;

        ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
        if (container == null) return;

        List<ItemStack> contents = container.stream().toList();
        if (contents.stream().allMatch(ItemStack::isEmpty)) return;

        int totalSlots = contents.size();
        int backgroundColor = DEFAULT_BACKGROUND_COLOR;
        Block block = Block.getBlockFromItem(stack.getItem());
        if (block instanceof ShulkerBoxBlock shulkerBlock) {
            totalSlots = SHULKER_BOX_SLOTS;
            DyeColor dye = shulkerBlock.getColor();
            backgroundColor = dye != null ? (0xFF000000 | dye.getEntityColor()) : UNDYED_SHULKER_BOX_COLOR;
        }

        ShulkerTooltipData data = new ShulkerTooltipData(contents, !full, totalSlots, backgroundColor);
        if (full) {
            module.recordFullPreviewFrame(stack, data);
        }
        cir.setReturnValue(Optional.of(data));
    }
}
