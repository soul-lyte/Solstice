package com.example.solstice.ui;

import com.example.solstice.textures.TexturePackManager;
import com.example.solstice.textures.TextureSlot;
import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import com.example.solstice.ui.widget.SolsticeButton;
import com.example.solstice.ui.widget.TextureSwapCardWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows every {@link TextureSlot} in one Textures-tab category (Tools, GUI,
 * Utilities, Fonts) as a vertical list of {@link TextureSwapCardWidget}s.
 * Reused for all four categories rather than one screen class each - they're
 * identical in structure, only the slot list and title differ.
 *
 * <p>Browsing with each card's arrows only changes local preview state -
 * "Done" is what actually applies every changed slot and reloads resources
 * exactly once (not once per slot); "Back" discards without applying anything.</p>
 */
public class TextureCategoryScreen extends Screen {

    private static final int PANEL_W = 320;
    private static final int PADDING = 14;
    private static final int ROW_GAP = 6;

    private final Screen parent;
    private final List<TextureSlot> slots;
    private final List<TextureSwapCardWidget> cards = new ArrayList<>();
    private int panelX, panelY, panelH;

    public TextureCategoryScreen(Screen parent, String categoryName, List<TextureSlot> slots) {
        super(Text.literal(categoryName));
        this.parent = parent;
        this.slots = slots;
    }

    @Override
    protected void init() {
        cards.clear();
        int innerW = PANEL_W - PADDING * 2;
        panelH = 30 + slots.size() * (TextureSwapCardWidget.HEIGHT + ROW_GAP) + 30;
        panelX = (width - PANEL_W) / 2;
        panelY = Math.max(20, (height - panelH) / 2);

        int x = panelX + PADDING;
        int y = panelY + 26;

        for (TextureSlot slot : slots) {
            TextureSwapCardWidget card = new TextureSwapCardWidget(x, y, innerW, slot, textRenderer);
            cards.add(card);
            addDrawableChild(card);
            y += TextureSwapCardWidget.HEIGHT + ROW_GAP;
        }

        int buttonY = panelY + panelH - 24;
        addDrawableChild(new SolsticeButton(panelX + PANEL_W / 2 - 104, buttonY, 100, 18,
                textRenderer, "Back", this::close));
        addDrawableChild(new SolsticeButton(panelX + PANEL_W / 2 + 4, buttonY, 100, 18,
                textRenderer, "Done", this::applyAndClose));
    }

    private void applyAndClose() {
        TexturePackManager manager = TexturePackManager.getInstance();
        boolean anyChanged = false;
        for (TextureSwapCardWidget card : cards) {
            if (card.hasPendingChange()) {
                manager.applyIndex(card.getSlot(), card.getPendingIndex());
                anyChanged = true;
            }
        }
        if (anyChanged) {
            manager.reload();
        }
        close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, ColorPalette.OVERLAY_DARK);
        SolsticeTheme.drawPanel(context, panelX, panelY, PANEL_W, panelH);
        context.drawText(textRenderer, getTitle(), panelX + PADDING, panelY + PADDING, ColorPalette.TEXT_PRIMARY, false);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        assert client != null;
        client.setScreen(parent);
    }

    // No shouldPause() override - see SolsticeScreen's identical note.
}
