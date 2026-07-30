package com.example.solstice.ui.widget;

import com.example.solstice.core.module.ModuleCategory;
import com.example.solstice.ui.SolsticeSounds;
import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/**
 * A horizontal tab button used at the top of the Solstice settings screen
 * to filter the module list by category.
 */
public class CategoryTabWidget extends SolsticeClickableWidget {

    private static final int HEIGHT = 24;

    private final ModuleCategory category;
    private final TextRenderer textRenderer;
    private boolean selected;
    private final Consumer<ModuleCategory> onSelect;

    public CategoryTabWidget(int x, int y, int width,
                              ModuleCategory category,
                              boolean selected,
                              TextRenderer textRenderer,
                              Consumer<ModuleCategory> onSelect) {
        super(x, y, width, HEIGHT, Text.of(category.getDisplayName()));
        this.category = category;
        this.selected = selected;
        this.textRenderer = textRenderer;
        this.onSelect = onSelect;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int bgColor = selected ? ColorPalette.BG_SELECTED
                    : isHovered() ? ColorPalette.BG_HOVER
                    : ColorPalette.BG_CARD;

        SolsticeTheme.fillRect(context, getX(), getY(), getWidth(), HEIGHT, bgColor);

        if (selected) {
            SolsticeTheme.fillRect(context, getX(), getY() + HEIGHT - 2, getWidth(), 2, ColorPalette.ACCENT_NEUTRAL);
        } else if (isHovered()) {
            SolsticeTheme.fillRect(context, getX(), getY() + HEIGHT - 2, getWidth(), 2, ColorPalette.ACCENT_DIM);
        }

        int textColor = selected ? ColorPalette.TEXT_ACCENT : ColorPalette.TEXT_SECONDARY;
        String label = category.getDisplayName();
        int lx = getX() + (getWidth() - textRenderer.getWidth(label)) / 2;
        int ly = getY() + (HEIGHT - 8) / 2;
        context.drawText(textRenderer, label, lx, ly, textColor, false);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            SolsticeSounds.playClick();
            onSelect.accept(category);
            return true;
        }
        return false;
    }

    public void setSelected(boolean selected) { this.selected = selected; }
    public ModuleCategory getCategory() { return category; }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
