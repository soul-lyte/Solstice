package com.example.solstice.ui;

import com.example.solstice.qol.visuals.VisualsModule;
import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import com.example.solstice.ui.widget.SolsticeButton;
import com.example.solstice.ui.widget.SolsticeToggleRow;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Sub-GUI opened by the "Edit" button on the Visuals module card. Lists each
 * bundled tweak as its own toggle. View Model Customization moved out into
 * its own standalone module/card (see {@link com.example.solstice.qol.viewmodel.ViewModelModule}) -
 * not listed here anymore.
 *
 * <p>Rows scroll a whole row at a time (no partial rows, no scissor clipping
 * needed) - each row is fully shown or fully hidden via {@code ClickableWidget.visible}/
 * {@code active}, which also blocks clicks on hidden rows for free. The panel height is
 * clamped to the screen so the Back button always stays reachable, even at high
 * GUI scale where 8 rows would otherwise overflow past the bottom of the screen.</p>
 */
public class VisualsEditScreen extends Screen {

    private static final int PANEL_W = 300;
    private static final int PADDING = 14;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 6;
    private static final int ROW_STEP = ROW_H + ROW_GAP;
    private static final int NESTED_EDIT_W = 40;
    private static final int HEADER_H = 26;
    private static final int FOOTER_H = 30;
    private static final int ROW_COUNT = 8;

    private final Screen parent;
    private final List<ClickableWidget> scrollWidgets = new ArrayList<>();
    private final List<Integer> scrollWidgetRows = new ArrayList<>();
    private int panelX, panelY, panelH;
    private int viewportTop, visibleRowCount, maxScrollRow, scrollRow;

    public VisualsEditScreen(Screen parent) {
        super(Text.literal("Visuals"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        scrollWidgets.clear();
        scrollWidgetRows.clear();

        int innerW = PANEL_W - PADDING * 2;
        int contentH = ROW_COUNT * ROW_STEP - ROW_GAP;
        int desiredPanelH = HEADER_H + contentH + FOOTER_H;
        int maxPanelH = height - 40;
        panelH = Math.min(desiredPanelH, maxPanelH);
        panelX = (width - PANEL_W) / 2;
        panelY = Math.max(20, (height - panelH) / 2);

        viewportTop = panelY + HEADER_H;
        int viewportH = panelH - HEADER_H - FOOTER_H;
        visibleRowCount = Math.max(1, viewportH / ROW_STEP);
        maxScrollRow = Math.max(0, ROW_COUNT - visibleRowCount);
        scrollRow = Math.min(scrollRow, maxScrollRow);

        int x = panelX + PADDING;

        int noFogToggleW = innerW - NESTED_EDIT_W - 6;
        SolsticeToggleRow noFogRow = new SolsticeToggleRow(x, 0, noFogToggleW, ROW_H, textRenderer, "No Fog",
                () -> VisualsModule.noFogAtmospheric, VisualsModule::setNoFogAtmospheric);
        noFogRow.setTooltip(Tooltip.of(Text.of("Thins out render-distance fog. Edit for separate water/lava fog toggles.")));
        addScrollWidget(noFogRow, 0);
        addScrollWidget(new SolsticeButton(x + noFogToggleW + 6, 0, NESTED_EDIT_W, ROW_H, textRenderer, "Edit",
                () -> { assert client != null; client.setScreen(new NoFogEditScreen(this)); }), 0);

        addScrollToggle(1, x, innerW, "Low Fire",
                "Shrinks the full-screen fire overlay when you're on fire, instead of it covering most of the view.",
                () -> VisualsModule.lowFire, VisualsModule::setLowFire);

        addScrollToggle(2, x, innerW, "Shield (Side/Low)",
                "Moves the raised shield out to the side and down while blocking, instead of it filling the center of your screen.",
                () -> VisualsModule.shieldSideLow, VisualsModule::setShieldSideLow);

        addScrollToggle(3, x, innerW, "Small Totem Pop",
                "Shrinks the golden sparkle burst when a totem of undying saves you.",
                () -> VisualsModule.smallTotemPop, VisualsModule::setSmallTotemPop);

        addScrollToggle(4, x, innerW, "Bow/Crossbow Pull Indicator",
                "Shows a small charge-percentage bar under the crosshair while drawing a bow or crossbow.",
                () -> VisualsModule.bowCrossbowIndicator, VisualsModule::setBowCrossbowIndicator);

        addScrollToggle(5, x, innerW, "No Food Particles",
                "Removes the crumb particles spawned while eating or drinking.",
                () -> VisualsModule.noFoodParticles, VisualsModule::setNoFoodParticles);

        addScrollToggle(6, x, innerW, "No Pumpkin Blur",
                "Removes the vision overlay from wearing a carved pumpkin as a helmet.",
                () -> VisualsModule.noPumpkinBlur, VisualsModule::setNoPumpkinBlur);

        addScrollToggle(7, x, innerW, "No Fishing Bobber",
                "Hides the fishing rod's bobber and line while fishing.",
                () -> VisualsModule.noFishingBobber, VisualsModule::setNoFishingBobber);

        updateScrollPositions();

        addDrawableChild(new SolsticeButton(panelX + (PANEL_W - 100) / 2, panelY + panelH - 24, 100, 18,
                textRenderer, "Back", this::close));
    }

    private void addScrollToggle(int rowIndex, int x, int w, String label, String description,
                                  java.util.function.BooleanSupplier getter, java.util.function.Consumer<Boolean> setter) {
        SolsticeToggleRow row = new SolsticeToggleRow(x, 0, w, ROW_H, textRenderer, label, getter, setter);
        row.setTooltip(Tooltip.of(Text.of(description)));
        addScrollWidget(row, rowIndex);
    }

    private void addScrollWidget(ClickableWidget widget, int rowIndex) {
        addDrawableChild(widget);
        scrollWidgets.add(widget);
        scrollWidgetRows.add(rowIndex);
    }

    private void updateScrollPositions() {
        for (int i = 0; i < scrollWidgets.size(); i++) {
            ClickableWidget widget = scrollWidgets.get(i);
            int visualRow = scrollWidgetRows.get(i) - scrollRow;
            boolean shown = visualRow >= 0 && visualRow < visibleRowCount;
            widget.visible = shown;
            widget.active = shown;
            if (shown) {
                widget.setY(viewportTop + visualRow * ROW_STEP);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int viewportH = visibleRowCount * ROW_STEP - ROW_GAP;
        if (maxScrollRow > 0 && mouseX >= panelX && mouseX < panelX + PANEL_W
                && mouseY >= viewportTop && mouseY < viewportTop + viewportH) {
            int newScrollRow = (int) Math.max(0, Math.min(maxScrollRow, scrollRow - Math.signum(verticalAmount)));
            if (newScrollRow != scrollRow) {
                scrollRow = newScrollRow;
                updateScrollPositions();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, ColorPalette.OVERLAY_DARK);
        SolsticeTheme.drawPanel(context, panelX, panelY, PANEL_W, panelH);
        context.drawText(textRenderer, "Visuals", panelX + PADDING, panelY + PADDING, ColorPalette.TEXT_PRIMARY, false);

        if (maxScrollRow > 0) {
            int trackX = panelX + PANEL_W - PADDING + 3;
            int trackH = visibleRowCount * ROW_STEP - ROW_GAP;
            SolsticeTheme.fillRect(context, trackX, viewportTop, 3, trackH, ColorPalette.BORDER_DEFAULT);
            int thumbH = Math.max(10, trackH * visibleRowCount / ROW_COUNT);
            int thumbY = viewportTop + (trackH - thumbH) * scrollRow / maxScrollRow;
            SolsticeTheme.fillRect(context, trackX, thumbY, 3, thumbH, ColorPalette.ACCENT_DIM);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        assert client != null;
        client.setScreen(parent);
    }

    // No shouldPause() override - see SolsticeScreen's identical note.
}
