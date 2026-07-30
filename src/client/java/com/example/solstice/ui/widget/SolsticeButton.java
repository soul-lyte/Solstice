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
 * A flat, rounded, dark-themed button - replaces vanilla {@code ButtonWidget}
 * (which renders Minecraft's classic grey bevel texture) wherever Solstice's
 * own screens need a simple press button, so the GUI never looks "Minecraft-like."
 */
public class SolsticeButton extends SolsticeClickableWidget {

    private final TextRenderer textRenderer;
    private final Runnable onPress;

    public SolsticeButton(int x, int y, int width, int height, TextRenderer textRenderer,
                           String label, Runnable onPress) {
        super(x, y, width, height, Text.of(label));
        this.textRenderer = textRenderer;
        this.onPress = onPress;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        SolsticeTheme.fillRect(context, getX(), getY(), getWidth(), getHeight(),
                isHovered() ? ColorPalette.BG_HOVER : ColorPalette.BG_CARD);
        SolsticeTheme.drawBorder(context, getX(), getY(), getWidth(), getHeight(),
                isHovered() ? ColorPalette.BORDER_ACCENT : ColorPalette.BORDER_DEFAULT);

        int tw = textRenderer.getWidth(getMessage());
        context.drawText(textRenderer, getMessage(),
                getX() + (getWidth() - tw) / 2, getY() + (getHeight() - 8) / 2,
                ColorPalette.TEXT_PRIMARY, false);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            SolsticeSounds.playClick();
            onPress.run();
            return true;
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
