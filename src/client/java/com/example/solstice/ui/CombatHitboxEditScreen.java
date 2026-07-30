package com.example.solstice.ui;

import com.example.solstice.qol.combathitbox.CombatHitboxModule;
import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import com.example.solstice.ui.widget.SolsticeButton;
import com.example.solstice.ui.widget.SolsticeColorPickerWidget;
import com.example.solstice.ui.widget.SolsticeSliderWidget;
import com.example.solstice.ui.widget.SolsticeToggleRow;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Outline width/color for the standalone Combat Hitbox module card, opened
 * via its "Edit" button ({@link EditableModule}) - includes a live 2D
 * preview of both the fixed white default outline and the configurable
 * "target" outline, so the width/color combo is visible without needing to
 * actually be in combat to check it.
 */
public class CombatHitboxEditScreen extends Screen {

    private static final int PANEL_W = 280;
    private static final int PADDING = 14;
    private static final int ROW_H = 20;
    private static final int COLOR_ROW_H = 70;
    private static final int ROW_GAP = 6;
    private static final int PREVIEW_H = 50;
    private static final int PREVIEW_BOX_W = 90;
    private static final int PREVIEW_BOX_H = 34;
    private static final int DEFAULT_COLOR = 0xFFFFFFFF;

    private final Screen parent;
    private int panelX, panelY, panelH;
    private int previewY;

    public CombatHitboxEditScreen(Screen parent) {
        super(Text.literal("Combat Hitbox"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int innerW = PANEL_W - PADDING * 2;
        panelH = 30 + (ROW_H + ROW_GAP) * 2 + (COLOR_ROW_H + ROW_GAP) + PREVIEW_H + 30;
        panelX = (width - PANEL_W) / 2;
        panelY = Math.max(20, (height - panelH) / 2);

        int x = panelX + PADDING;
        int y = panelY + 26;

        addDrawableChild(new SolsticeToggleRow(x, y, innerW, ROW_H, textRenderer, "Players Only",
                () -> CombatHitboxModule.playersOnly, CombatHitboxModule::setPlayersOnly));
        y += ROW_H + ROW_GAP;

        addDrawableChild(new SolsticeSliderWidget(x, y, innerW, ROW_H, textRenderer, "Outline Width",
                0.5, 5.0, CombatHitboxModule.outlineWidth,
                v -> String.format("%.1f", v),
                v -> CombatHitboxModule.setOutlineWidth((float) (double) v)));
        y += ROW_H + ROW_GAP;

        addDrawableChild(new SolsticeColorPickerWidget(x, y, innerW, COLOR_ROW_H, textRenderer, "Target Color",
                () -> CombatHitboxModule.outlineColor & 0xFFFFFF,
                rgb -> CombatHitboxModule.setOutlineColor(0xFF000000 | (rgb & 0xFFFFFF))));
        y += COLOR_ROW_H + ROW_GAP;

        previewY = y;

        addDrawableChild(new SolsticeButton(panelX + (PANEL_W - 100) / 2, panelY + panelH - 24, 100, 18,
                textRenderer, "Back", this::close));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, ColorPalette.OVERLAY_DARK);
        SolsticeTheme.drawPanel(context, panelX, panelY, PANEL_W, panelH);
        context.drawText(textRenderer, "Combat Hitbox", panelX + PADDING, panelY + PADDING, ColorPalette.TEXT_PRIMARY, false);

        int gap = 16;
        int totalW = PREVIEW_BOX_W * 2 + gap;
        int startX = panelX + (PANEL_W - totalW) / 2;
        drawPreviewBox(context, startX, previewY, "Default", DEFAULT_COLOR);
        drawPreviewBox(context, startX + PREVIEW_BOX_W + gap, previewY, "Target", CombatHitboxModule.outlineColor);

        super.render(context, mouseX, mouseY, delta);
    }

    /** Flat 2D stand-in for the real 3D outline - same color and a border thickness scaled from the width setting. */
    private void drawPreviewBox(DrawContext context, int x, int y, String label, int color) {
        int labelW = textRenderer.getWidth(label);
        context.drawText(textRenderer, label, x + (PREVIEW_BOX_W - labelW) / 2, y, ColorPalette.TEXT_SECONDARY, false);
        int boxY = y + 11;

        int thickness = Math.max(1, Math.min(6, Math.round(CombatHitboxModule.outlineWidth)));
        SolsticeTheme.fillRect(context, x, boxY, PREVIEW_BOX_W, PREVIEW_BOX_H, ColorPalette.BG_CARD);
        SolsticeTheme.fillRect(context, x, boxY, PREVIEW_BOX_W, thickness, color); // top
        SolsticeTheme.fillRect(context, x, boxY + PREVIEW_BOX_H - thickness, PREVIEW_BOX_W, thickness, color); // bottom
        SolsticeTheme.fillRect(context, x, boxY, thickness, PREVIEW_BOX_H, color); // left
        SolsticeTheme.fillRect(context, x + PREVIEW_BOX_W - thickness, boxY, thickness, PREVIEW_BOX_H, color); // right
    }

    @Override
    public void close() {
        assert client != null;
        client.setScreen(parent);
    }

    // No shouldPause() override - see SolsticeScreen's identical note.
}
