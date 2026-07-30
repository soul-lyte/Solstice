package com.example.solstice.ui.widget;

import com.example.solstice.ui.SolsticeSounds;
import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * A labeled row that lets the user rebind a keybinding directly from the
 * settings screen - click, then press the new key. For {@code KeySetting}s.
 */
public class SolsticeKeybindRow extends SolsticeClickableWidget {

    private static final int PADDING = 6;

    private final TextRenderer textRenderer;
    private final String label;
    private final Supplier<String> displayNameGetter;
    private final IntConsumer keyCodeSetter;
    private boolean listening = false;

    public SolsticeKeybindRow(int x, int y, int width, int height, TextRenderer textRenderer,
                               String label, Supplier<String> displayNameGetter, IntConsumer keyCodeSetter) {
        super(x, y, width, height, Text.of(label));
        this.textRenderer = textRenderer;
        this.label = label;
        this.displayNameGetter = displayNameGetter;
        this.keyCodeSetter = keyCodeSetter;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        SolsticeTheme.fillRect(context, getX(), getY(), getWidth(), getHeight(),
                listening || isHovered() ? ColorPalette.BG_HOVER : ColorPalette.BG_CARD);
        SolsticeTheme.drawBorder(context, getX(), getY(), getWidth(), getHeight(), ColorPalette.BORDER_DEFAULT);

        context.drawText(textRenderer, label,
                getX() + PADDING, getY() + (getHeight() - 8) / 2,
                ColorPalette.TEXT_PRIMARY, false);

        String value = listening ? "Press a key..." : displayNameGetter.get();
        int tw = textRenderer.getWidth(value);
        context.drawText(textRenderer, value,
                getX() + getWidth() - PADDING - tw, getY() + (getHeight() - 8) / 2,
                listening ? ColorPalette.TEXT_PRIMARY : ColorPalette.TEXT_SECONDARY, false);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            SolsticeSounds.playClick();
            listening = true;
            setFocused(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (listening) {
            keyCodeSetter.accept(input.key());
            listening = false;
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
