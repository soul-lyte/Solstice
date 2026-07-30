package com.example.solstice.mixin.modmenu;

import com.example.solstice.ui.SolsticeModsScreen;
import com.example.solstice.ui.SolsticeScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds real "Mods" and "Solstice" buttons to vanilla's own pause menu grid,
 * opening {@link SolsticeModsScreen} and {@link SolsticeScreen} respectively -
 * not buttons living inside Solstice's own GUI.
 *
 * <p>Per the explicit request: positioned exactly where the "Save and Quit to
 * Title" / "Disconnect" button (grid's {@code exitButton} field, always the
 * grid's last row, full width) currently sits, split into two half-width
 * buttons the same way the grid's own "Options | Share to LAN" row already
 * splits a row into two - and {@code exitButton} itself is pushed down one
 * row to make room, using the exact same {@code GRID_MARGIN} (4px) rhythm
 * vanilla's own grid uses between rows (confirmed via decompile - {@code
 * ButtonWidget.DEFAULT_HEIGHT} is 20, so a full row-to-row step is 24px).
 * Not inserted into the grid's own internal {@code Adder}/{@code Positioner}
 * system - that's a private local built entirely inside {@code initWidgets()}
 * with no field to hook into mid-build; reading {@code exitButton}'s own
 * final computed bounds after the grid already finished laying itself out is
 * far more robust than trying to reverse-engineer/duplicate that math.</p>
 */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {

    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void solstice$addModMenuRow(CallbackInfo ci) {
        GameMenuScreen self = (GameMenuScreen) (Object) this;
        ButtonWidget exit = ((GameMenuScreenExitButtonAccessor) self).solstice$getExitButton();
        // exitButton is only ever set when showMenu was true (initWidgets() didn't run
        // otherwise) - nothing to anchor off of, so skip rather than guess a position.
        if (exit == null) return;

        int rowY = exit.getY();
        int gap = 4;
        int halfWidth = (exit.getWidth() - gap) / 2;

        exit.setY(rowY + exit.getHeight() + gap);

        assert client != null;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Mods"), b -> client.setScreen(new SolsticeModsScreen(self)))
                .dimensions(exit.getX(), rowY, halfWidth, exit.getHeight())
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Solstice"), b -> client.setScreen(new SolsticeScreen(self)))
                .dimensions(exit.getX() + halfWidth + gap, rowY, halfWidth, exit.getHeight())
                .build());
    }
}
