package com.example.solstice.mixin.modmenu;

import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes {@code GameMenuScreen}'s private {@code exitButton} field so
 * {@link GameMenuScreenMixin} can read its real, already-laid-out position
 * (set by vanilla's own {@code GridWidget}) instead of recomputing grid math
 * itself, and move it down to make room for the new Mods/Solstice row.
 */
@Mixin(GameMenuScreen.class)
public interface GameMenuScreenExitButtonAccessor {
    @Accessor("exitButton")
    ButtonWidget solstice$getExitButton();
}
