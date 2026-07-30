package com.example.solstice.ui;

import com.example.solstice.core.hud.HudElement;
import com.example.solstice.core.hud.HudLayoutManager;
import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import com.example.solstice.ui.widget.SolsticeButton;
import com.example.solstice.ui.widget.SolsticeColorPickerWidget;
import com.example.solstice.ui.widget.SolsticeSliderWidget;
import com.example.solstice.ui.widget.SolsticeToggleRow;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * Per-widget configuration screen for the HUD editor - text color, background
 * color, and box size, opened by the "Edit" button on a widget's box in
 * {@link HudEditorScreen}. Replaces the old cramped hide/color-cycle buttons
 * with a proper settings screen matching the rest of Solstice's UI.
 *
 * <p>{@link WatermarkHudElement} additionally gets a text field row here for
 * editing its actual displayed text - the only element with content the
 * user types themselves rather than something Solstice computes.</p>
 */
public class HudWidgetConfigScreen extends Screen {

    private static final int PANEL_W = 260;
    private static final int PADDING = 14;
    private static final int ROW_H = 20;
    private static final int COLOR_ROW_H = 70;
    private static final int ROW_GAP = 6;

    private final Screen parent;
    private final HudElement element;
    private int panelX, panelY, panelH;

    public HudWidgetConfigScreen(Screen parent, HudElement element) {
        super(Text.of(element.getDisplayName() + " Settings"));
        this.parent = parent;
        this.element = element;
    }

    @Override
    protected void init() {
        HudLayoutManager layout = HudLayoutManager.getInstance();
        String id = element.getId();
        int innerW = PANEL_W - PADDING * 2;
        boolean isWatermark = element instanceof WatermarkHudElement;

        // visible row + (optional text row) + 2 color pickers (taller than a normal row) + width + height
        panelH = 40 + ROW_H + ROW_GAP + (isWatermark ? ROW_H + ROW_GAP : 0)
                + 2 * (COLOR_ROW_H + ROW_GAP) + 2 * (ROW_H + ROW_GAP) + 30;
        panelX = (width - PANEL_W) / 2;
        panelY = Math.max(20, (height - panelH) / 2);

        int y = panelY + 36;
        int x = panelX + PADDING;

        addDrawableChild(new SolsticeToggleRow(x, y, innerW, ROW_H, textRenderer, "Visible",
                () -> layout.isVisible(id, element.isVisibleByDefault()), v -> layout.setVisible(id, v)));
        y += ROW_H + ROW_GAP;

        if (isWatermark) {
            WatermarkHudElement watermark = (WatermarkHudElement) element;
            TextFieldWidget textField = new TextFieldWidget(textRenderer, x, y, innerW, ROW_H, Text.of("Watermark text"));
            textField.setMaxLength(128);
            textField.setText(watermark.getText());
            textField.setChangedListener(watermark::setText);
            addDrawableChild(textField);
            y += ROW_H + ROW_GAP;
        }

        y = addColorPicker(x, y, innerW,
                () -> layout.getTextColor(id, ColorPalette.TEXT_PRIMARY), c -> layout.setTextColor(id, c), "Text Color");
        y = addColorPicker(x, y, innerW,
                () -> layout.getBackground(id, 0x880D1117), c -> layout.setBackground(id, c), "Background Color");

        int width = layout.getWidth(id, element.getWidth());
        int height = layout.getHeight(id, element.getHeight());
        addDrawableChild(new SolsticeSliderWidget(x, y, innerW, ROW_H, textRenderer, "Width", 40, 300, width,
                v -> String.valueOf((int) Math.round(v)),
                v -> layout.setSize(id, (int) Math.round(v), layout.getHeight(id, element.getHeight()))));
        y += ROW_H + ROW_GAP;
        addDrawableChild(new SolsticeSliderWidget(x, y, innerW, ROW_H, textRenderer, "Height", 10, 100, height,
                v -> String.valueOf((int) Math.round(v)),
                v -> layout.setSize(id, layout.getWidth(id, element.getWidth()), (int) Math.round(v))));
        y += ROW_H + ROW_GAP;

        addDrawableChild(new SolsticeButton(panelX + (PANEL_W - 100) / 2, panelY + panelH - 24, 100, 18,
                textRenderer, "Back", this::close));
    }

    /**
     * A real color picker bound to one ARGB int color - the picker only ever touches
     * the low 24 (RGB) bits, so the existing alpha channel is preserved on every write.
     */
    private int addColorPicker(int x, int y, int w, java.util.function.IntSupplier getter,
                                java.util.function.IntConsumer setter, String label) {
        addDrawableChild(new SolsticeColorPickerWidget(x, y, w, COLOR_ROW_H, textRenderer, label,
                () -> getter.getAsInt() & 0xFFFFFF,
                rgb -> setter.accept((getter.getAsInt() & 0xFF000000) | (rgb & 0xFFFFFF))));
        return y + COLOR_ROW_H + ROW_GAP;
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
