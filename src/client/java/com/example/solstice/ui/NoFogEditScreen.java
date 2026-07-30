package com.example.solstice.ui;

import com.example.solstice.qol.visuals.VisualsModule;
import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import com.example.solstice.ui.widget.SolsticeButton;
import com.example.solstice.ui.widget.SolsticeToggleRow;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;

/**
 * Per-fog-type toggles, opened by the nested Edit button on Visuals' "No Fog"
 * row. Render-distance ("Atmospheric") fog is handled differently under the
 * hood than the other three - see {@code mixin.visuals.AtmosphericFogModifierMixin}
 * for why it's pushed far out rather than fully removed (avoids a real render
 * bug: unloaded chunks at the edge of your view flashing black for a moment
 * with no fog at all to mask them while they load).
 */
public class NoFogEditScreen extends Screen {

    private static final int PANEL_W = 280;
    private static final int PADDING = 14;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 6;

    private final Screen parent;
    private int panelX, panelY, panelH;

    public NoFogEditScreen(Screen parent) {
        super(Text.literal("No Fog"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int innerW = PANEL_W - PADDING * 2;
        int rowCount = 3;
        panelH = 30 + rowCount * (ROW_H + ROW_GAP) + 30;
        panelX = (width - PANEL_W) / 2;
        panelY = Math.max(20, (height - panelH) / 2);

        int x = panelX + PADDING;
        int y = panelY + 26;

        y = addToggle(x, y, innerW, "Render Distance",
                "Thins out the fog at the edge of your view distance instead of removing it outright - fully removing it causes unloaded chunks to flash black for a moment while they load.",
                () -> VisualsModule.noFogAtmospheric, VisualsModule::setNoFogAtmospheric);

        y = addToggle(x, y, innerW, "Water",
                "Removes the underwater fog/tint when your head is submerged.",
                () -> VisualsModule.noFogWater, VisualsModule::setNoFogWater);

        addToggle(x, y, innerW, "Lava",
                "Removes the fog when your head is inside lava.",
                () -> VisualsModule.noFogLava, VisualsModule::setNoFogLava);

        addDrawableChild(new SolsticeButton(panelX + (PANEL_W - 100) / 2, panelY + panelH - 24, 100, 18,
                textRenderer, "Back", this::close));
    }

    private int addToggle(int x, int y, int w, String label, String description,
                           java.util.function.BooleanSupplier getter, java.util.function.Consumer<Boolean> setter) {
        SolsticeToggleRow row = new SolsticeToggleRow(x, y, w, ROW_H, textRenderer, label, getter, setter);
        row.setTooltip(Tooltip.of(Text.of(description)));
        addDrawableChild(row);
        return y + ROW_H + ROW_GAP;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, ColorPalette.OVERLAY_DARK);
        SolsticeTheme.drawPanel(context, panelX, panelY, PANEL_W, panelH);
        context.drawText(textRenderer, "No Fog", panelX + PADDING, panelY + PADDING, ColorPalette.TEXT_PRIMARY, false);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        assert client != null;
        client.setScreen(parent);
    }

    // No shouldPause() override - see SolsticeScreen's identical note.
}
