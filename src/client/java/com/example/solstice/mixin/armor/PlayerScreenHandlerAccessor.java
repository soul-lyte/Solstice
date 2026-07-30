package com.example.solstice.mixin.armor;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Exposes {@link PlayerScreenHandler}'s private static armor-slot ordering
 * and empty-slot-icon table - the same real ones vanilla's own inventory
 * screen uses - so Armor HUD draws in the exact vanilla order/icons instead
 * of a guessed one. Both accessor methods are declared {@code static}
 * (confirmed necessary via this project's own earlier gotcha: a
 * non-static accessor on a static target field compiles fine but throws
 * {@code VerifyError} at class-load).
 */
@Mixin(PlayerScreenHandler.class)
public interface PlayerScreenHandlerAccessor {

    @Accessor("EQUIPMENT_SLOT_ORDER")
    static EquipmentSlot[] solstice$getEquipmentSlotOrder() {
        throw new UnsupportedOperationException();
    }

    @Accessor("EMPTY_ARMOR_SLOT_TEXTURES")
    static Map<EquipmentSlot, Identifier> solstice$getEmptyArmorSlotTextures() {
        throw new UnsupportedOperationException();
    }
}
