package com.example.solstice.ui.widget;

import com.example.solstice.ui.SolsticeSounds;
import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;

/**
 * The "Add your own" card at the end of the Presets row - clicking opens a
 * native file/folder picker (see {@code ResourcePackFileChooser}) instead
 * of applying anything itself.
 */
public class AddPackCardWidget extends SolsticeClickableWidget {

    private static final int PADDING = 8;

    private final TextRenderer textRenderer;
    private final Runnable onClick;

    public AddPackCardWidget(int x, int y, int width, TextRenderer textRenderer, Runnable onClick) {
        super(x, y, width, TexturePresetCardWidget.HEIGHT, Text.of("Add your own"));
        this.textRenderer = textRenderer;
        this.onClick = onClick;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        SolsticeTheme.drawCard(context, getX(), getY(), getWidth(), getHeight(), isHovered());
        SolsticeTheme.drawBorder(context, getX(), getY(), getWidth(), getHeight(), ColorPalette.BORDER_ACCENT);

        String plus = "+";
        int plusW = textRenderer.getWidth(plus);
        context.drawText(textRenderer, plus, getX() + (getWidth() - plusW) / 2, getY() + 10, ColorPalette.TEXT_ACCENT, false);

        String label = getMessage().getString();
        int labelW = textRenderer.getWidth(label);
        context.drawText(textRenderer, label, getX() + (getWidth() - labelW) / 2, getY() + getHeight() - 20,
                ColorPalette.TEXT_SECONDARY, false);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            SolsticeSounds.playClick();
            onClick.run();
            return true;
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
