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
 * A card for one Textures-tab category (Tools, GUI, Utilities, Fonts) shown
 * in {@link com.example.solstice.ui.SolsticeScreen} in place of module cards
 * while that tab is active - clicking opens the category's
 * {@link com.example.solstice.ui.TextureCategoryScreen}. Deliberately much
 * simpler than {@link ModuleCardWidget}: no toggle, no enabled state, just a
 * name, a description, and a click target.
 */
public class TextureCategoryCardWidget extends SolsticeClickableWidget {

    public static final int HEIGHT = 52;
    private static final int PADDING = 8;

    private final String description;
    private final TextRenderer textRenderer;
    private final Runnable onOpen;

    public TextureCategoryCardWidget(int x, int y, int width, String name, String description,
                                      TextRenderer textRenderer, Runnable onOpen) {
        super(x, y, width, HEIGHT, Text.of(name));
        this.description = description;
        this.textRenderer = textRenderer;
        this.onOpen = onOpen;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        SolsticeTheme.drawCard(context, getX(), getY(), getWidth(), HEIGHT, isHovered());

        context.drawText(textRenderer, getMessage(), getX() + PADDING, getY() + 16, ColorPalette.TEXT_PRIMARY, false);

        String desc = description;
        int maxDescW = getWidth() - PADDING * 2;
        if (textRenderer.getWidth(desc) > maxDescW) {
            desc = textRenderer.trimToWidth(desc, maxDescW - textRenderer.getWidth("…")) + "…";
        }
        context.drawText(textRenderer, desc, getX() + PADDING, getY() + 30, ColorPalette.TEXT_SECONDARY, false);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            SolsticeSounds.playClick();
            onOpen.run();
            return true;
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
