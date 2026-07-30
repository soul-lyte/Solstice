package com.example.solstice.ui.widget;

import com.example.solstice.textures.TexturePresetManager;
import com.example.solstice.ui.SolsticeSounds;
import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;

/**
 * A card for one saved custom Preset+Advanced combo (see {@code
 * TexturePresetManager.SavedCombo}) - applies the whole combo on click,
 * same "no confirmation" spirit as {@code ProfileCardWidget}.
 */
public class SavedComboCardWidget extends SolsticeClickableWidget {

    private static final int PADDING = 6;

    private final TexturePresetManager.SavedCombo combo;
    private final TextRenderer textRenderer;
    private final boolean selected;
    private final Runnable onApplied;

    public SavedComboCardWidget(int x, int y, int width, TexturePresetManager.SavedCombo combo,
                                 TextRenderer textRenderer, boolean selected, Runnable onApplied) {
        super(x, y, width, TexturePresetCardWidget.HEIGHT, Text.of(combo.name()));
        this.combo = combo;
        this.textRenderer = textRenderer;
        this.selected = selected;
        this.onApplied = onApplied;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        SolsticeTheme.drawCard(context, getX(), getY(), getWidth(), getHeight(), isHovered());
        if (selected) {
            SolsticeTheme.drawBorder(context, getX(), getY(), getWidth(), getHeight(), ColorPalette.ACCENT_NEUTRAL);
        }

        context.drawText(textRenderer, getMessage(), getX() + PADDING, getY() + 8,
                selected ? ColorPalette.TEXT_ACCENT : ColorPalette.TEXT_PRIMARY, false);
        context.drawText(textRenderer, "Custom combo", getX() + PADDING, getY() + 20, ColorPalette.TEXT_SECONDARY, false);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            SolsticeSounds.playClick();
            onApplied.run();
            return true;
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
