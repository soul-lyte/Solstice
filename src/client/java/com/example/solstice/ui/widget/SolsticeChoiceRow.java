package com.example.solstice.ui.widget;

import com.example.solstice.ui.SolsticeSounds;
import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * A labeled row that cycles through a fixed list of named options on click -
 * for {@code ChoiceSetting}s (e.g. a style/mode picker) on the module
 * settings screen.
 */
public class SolsticeChoiceRow extends SolsticeClickableWidget {

    private static final int PADDING = 6;

    private final TextRenderer textRenderer;
    private final String label;
    private final List<String> options;
    private final IntSupplier indexGetter;
    private final IntConsumer indexSetter;

    public SolsticeChoiceRow(int x, int y, int width, int height, TextRenderer textRenderer,
                              String label, List<String> options, IntSupplier indexGetter, IntConsumer indexSetter) {
        super(x, y, width, height, Text.of(label));
        this.textRenderer = textRenderer;
        this.label = label;
        this.options = options;
        this.indexGetter = indexGetter;
        this.indexSetter = indexSetter;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        SolsticeTheme.fillRect(context, getX(), getY(), getWidth(), getHeight(),
                isHovered() ? ColorPalette.BG_HOVER : ColorPalette.BG_CARD);
        SolsticeTheme.drawBorder(context, getX(), getY(), getWidth(), getHeight(), ColorPalette.BORDER_DEFAULT);

        context.drawText(textRenderer, label,
                getX() + PADDING, getY() + (getHeight() - 8) / 2,
                ColorPalette.TEXT_PRIMARY, false);

        String current = "‹ " + options.get(Math.floorMod(indexGetter.getAsInt(), options.size())) + " ›";
        int tw = textRenderer.getWidth(current);
        context.drawText(textRenderer, current,
                getX() + getWidth() - PADDING - tw, getY() + (getHeight() - 8) / 2,
                ColorPalette.TEXT_SECONDARY, false);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            SolsticeSounds.playClick();
            int next = Math.floorMod(indexGetter.getAsInt() + 1, options.size());
            indexSetter.accept(next);
            return true;
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
