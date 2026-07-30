package com.example.solstice.mixin.modmenu;

import com.example.solstice.ui.SolsticeModsScreen;
import com.example.solstice.ui.SolsticeScreen;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextIconButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds real "Mods" and "Solstice" buttons to vanilla's own title screen,
 * opening {@link SolsticeModsScreen} and {@link SolsticeScreen} respectively -
 * not buttons living inside Solstice's own GUI.
 *
 * <p>Per the explicit request: positioned where "Options"/"Quit Game" (and
 * the language/accessibility icon buttons flanking them - confirmed via
 * decompile, all four share exactly one row, {@code TitleScreen.init()}
 * computes a single {@code l} used for all of them) currently sit, and that
 * whole row is pushed down one row (24px, matching the same spacing constant
 * {@code addNormalWidgets}/{@code addDevelopmentWidgets} already use) to make
 * room for the new row above it.</p>
 *
 * <p>None of those four widgets are stored in a field (all local variables
 * inside {@code init()}), so there's no accessor target - they're located
 * after the fact via {@link Screen#children()}: the two {@code ButtonWidget}s
 * by matching {@link ButtonWidget#getMessage()} against the exact {@code
 * Text.translatable} keys vanilla itself uses ({@code "menu.options"}/{@code
 * "menu.quit"} - reliable since {@code TranslatableTextContent} implements
 * real key+args equality, not string comparison), the two {@code
 * TextIconButtonWidget}s (language + accessibility) just by type, since
 * they're the only two of that type on this screen. If any of the four
 * aren't found (vanilla's layout changed shape), bails out entirely rather
 * than half-applying a broken layout.</p>
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void solstice$addModMenuRow(CallbackInfo ci) {
        TitleScreen self = (TitleScreen) (Object) this;

        ButtonWidget options = solstice$findButton(Text.translatable("menu.options"));
        ButtonWidget quit = solstice$findButton(Text.translatable("menu.quit"));
        List<TextIconButtonWidget> icons = solstice$findIconButtons();
        if (options == null || quit == null || icons.size() != 2) return;

        int rowY = options.getY();
        int spacingY = 24;

        options.setY(rowY + spacingY);
        quit.setY(rowY + spacingY);
        for (TextIconButtonWidget icon : icons) {
            icon.setPosition(icon.getX(), rowY + spacingY);
        }

        assert client != null;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Mods"), b -> client.setScreen(new SolsticeModsScreen(self)))
                .dimensions(options.getX(), rowY, options.getWidth(), options.getHeight())
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Solstice"), b -> client.setScreen(new SolsticeScreen(self)))
                .dimensions(quit.getX(), rowY, quit.getWidth(), quit.getHeight())
                .build());
    }

    private ButtonWidget solstice$findButton(Text message) {
        for (Element element : this.children()) {
            if (element instanceof ButtonWidget button && button.getMessage().equals(message)) {
                return button;
            }
        }
        return null;
    }

    private List<TextIconButtonWidget> solstice$findIconButtons() {
        List<TextIconButtonWidget> result = new ArrayList<>();
        for (Element element : this.children()) {
            if (element instanceof TextIconButtonWidget icon) {
                result.add(icon);
            }
        }
        return result;
    }
}
