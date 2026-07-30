package com.example.solstice.ui.widget;

import com.example.solstice.ui.SolsticeSounds;
import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Shown at the end of a Profiles-tab row when the live settings no longer
 * match any saved profile in that category - an ephemeral "Custom" preview
 * of the current, unsaved state. Clicking Save persists it as a real,
 * selectable profile (see {@code CustomVisualProfile}/{@code CustomPerformanceProfile}).
 * Unlike {@link ProfileCardWidget}, clicking the card body does nothing - the
 * state it shows is already live, there's nothing to "apply".
 */
public class DraftProfileCardWidget extends SolsticeClickableWidget {

    private static final int PADDING = 8;
    private static final int SAVE_W = 50;
    private static final int SAVE_H = 16;

    private final TextRenderer textRenderer;
    private final Runnable onSave;

    public DraftProfileCardWidget(int x, int y, int width, TextRenderer textRenderer, Runnable onSave) {
        super(x, y, width, ProfileCardWidget.HEIGHT, Text.of("Custom"));
        this.textRenderer = textRenderer;
        this.onSave = onSave;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        SolsticeTheme.drawCard(context, getX(), getY(), getWidth(), getHeight(), isHovered());
        SolsticeTheme.drawBorder(context, getX(), getY(), getWidth(), getHeight(), ColorPalette.BORDER_ACCENT);

        context.drawText(textRenderer, "Custom (unsaved)", getX() + PADDING, getY() + 6, ColorPalette.TEXT_PRIMARY, false);
        context.drawText(textRenderer, "Doesn't match a saved", getX() + PADDING, getY() + 18, ColorPalette.TEXT_SECONDARY, false);
        context.drawText(textRenderer, "profile.", getX() + PADDING, getY() + 27, ColorPalette.TEXT_SECONDARY, false);

        int bx = getX() + PADDING;
        int by = getY() + getHeight() - SAVE_H - 4;
        boolean saveHovered = mouseX >= bx && mouseX < bx + SAVE_W && mouseY >= by && mouseY < by + SAVE_H;
        SolsticeTheme.fillRect(context, bx, by, SAVE_W, SAVE_H, saveHovered ? ColorPalette.BG_HOVER : ColorPalette.BG_SELECTED);
        SolsticeTheme.drawBorder(context, bx, by, SAVE_W, SAVE_H, ColorPalette.BORDER_DEFAULT);
        String label = "Save";
        int tw = textRenderer.getWidth(label);
        context.drawText(textRenderer, label, bx + (SAVE_W - tw) / 2, by + 4, ColorPalette.TEXT_PRIMARY, false);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (!isMouseOver(click.x(), click.y())) {
            return false;
        }
        int bx = getX() + PADDING;
        int by = getY() + getHeight() - SAVE_H - 4;
        if (click.x() >= bx && click.x() < bx + SAVE_W && click.y() >= by && click.y() < by + SAVE_H) {
            SolsticeSounds.playClick();
            onSave.run();
            return true;
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
