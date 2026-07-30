package com.example.solstice.ui;

import com.example.solstice.qol.viewmodel.ViewModelModule;
import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import com.example.solstice.ui.widget.SolsticeButton;
import com.example.solstice.ui.widget.SolsticeSliderWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Settings for the first-person held item's position and size. Unlike most
 * Solstice screens, this one is anchored to a corner and never dims the rest
 * of the screen - the whole point is to see your actual first-person item
 * update live as you drag the sliders, since the values apply directly to
 * {@code HeldItemRendererMixin} on every frame.
 */
public class ViewModelEditScreen extends Screen {

    private static final int PANEL_W = 220;
    private static final int PADDING = 10;
    private static final int ROW_H = 18;
    private static final int ROW_GAP = 5;

    private final Screen parent;
    private int panelX, panelY, panelH;

    public ViewModelEditScreen(Screen parent) {
        super(Text.literal("View Model"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int innerW = PANEL_W - PADDING * 2;
        panelH = 26 + 5 * (ROW_H + ROW_GAP) + 26;
        panelX = 10;
        panelY = 10;

        int x = panelX + PADDING;
        int y = panelY + 22;

        addDrawableChild(new SolsticeSliderWidget(x, y, innerW, ROW_H, textRenderer, "Scale", 0.5, 1.5,
                ViewModelModule.scale,
                v -> String.format("%.2fx", v),
                ViewModelModule::setScale));
        y += ROW_H + ROW_GAP;

        addDrawableChild(new SolsticeSliderWidget(x, y, innerW, ROW_H, textRenderer, "Offset X", -1.0, 1.0,
                ViewModelModule.offsetX,
                v -> String.format("%.2f", v),
                ViewModelModule::setOffsetX));
        y += ROW_H + ROW_GAP;

        addDrawableChild(new SolsticeSliderWidget(x, y, innerW, ROW_H, textRenderer, "Offset Y", -1.0, 1.0,
                ViewModelModule.offsetY,
                v -> String.format("%.2f", v),
                ViewModelModule::setOffsetY));
        y += ROW_H + ROW_GAP;

        addDrawableChild(new SolsticeSliderWidget(x, y, innerW, ROW_H, textRenderer, "Offset Z", -1.0, 1.0,
                ViewModelModule.offsetZ,
                v -> String.format("%.2f", v),
                ViewModelModule::setOffsetZ));
        y += ROW_H + ROW_GAP;

        int halfW = (innerW - 4) / 2;
        addDrawableChild(new SolsticeButton(x, y, halfW, ROW_H, textRenderer, "Reset", () -> {
            ViewModelModule.reset();
            clearAndInit();
        }));
        addDrawableChild(new SolsticeButton(x + halfW + 4, y, halfW, ROW_H, textRenderer, "Back", this::close));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Deliberately no full-screen dim here - the real first-person view stays
        // visible behind this corner panel so slider changes preview live.
        SolsticeTheme.drawPanel(context, panelX, panelY, PANEL_W, panelH);
        context.drawText(textRenderer, "View Model", panelX + PADDING, panelY + 8, ColorPalette.TEXT_PRIMARY, false);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        assert client != null;
        client.setScreen(parent);
    }

    // No shouldPause() override - see SolsticeScreen's identical note.
}
