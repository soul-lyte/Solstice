package com.example.solstice.ui;

import com.example.solstice.core.module.Module;
import com.example.solstice.core.module.ModuleSetting;
import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import com.example.solstice.ui.widget.SolsticeButton;
import com.example.solstice.ui.widget.SolsticeChoiceRow;
import com.example.solstice.ui.widget.SolsticeColorPickerWidget;
import com.example.solstice.ui.widget.SolsticeKeybindRow;
import com.example.solstice.ui.widget.SolsticeSliderWidget;
import com.example.solstice.ui.widget.SolsticeToggleRow;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated settings/info screen for a single module, opened by right-clicking
 * its card on {@link SolsticeScreen}. Shows the module's full (non-truncated)
 * description plus any adjustable controls it exposes via {@link Module#getSettings()}.
 *
 * <p>Settings rows scroll one whole setting at a time when there are too
 * many to fit (e.g. Armor HUD's full uku3lig-style customization set) -
 * same "fully shown or fully hidden via visible/active" technique {@code
 * VisualsEditScreen} uses, adapted for variable row heights (a
 * {@link ModuleSetting.ColorSetting} row is taller than the rest) instead
 * of a uniform row step.</p>
 */
public class ModuleDetailScreen extends Screen {

    private static final int PANEL_W = 300;
    private static final int PADDING = 14;
    private static final int ROW_H   = 20;
    private static final int COLOR_ROW_H = 70;
    private static final int ROW_GAP = 6;
    private static final int LINE_H  = 10;

    private static final String ALWAYS_ON_NOTE =
            "Always enabled - this optimization cannot be turned off. More settings may be added later.";

    private static final String EDITABLE_ELSEWHERE_NOTE =
            "This module's settings are configured from the Edit button on its card, not here.";

    private final Screen parent;
    private final Module module;

    private List<OrderedText> descriptionLines = List.of();
    private List<OrderedText> alwaysOnNoteLines = List.of();
    private List<OrderedText> emptySettingsLines = List.of();
    private boolean showSettingsSection;
    private int panelX, panelY, panelH;
    private int statusY, statusH;
    private int settingsStartY;

    private List<ModuleSetting> settings = List.of();
    private final List<ClickableWidget> settingWidgets = new ArrayList<>();
    private int[] settingOffsetY = new int[0];
    private int[] settingHeight = new int[0];
    private int viewportH;
    private int visibleSettingsCount;
    private int scrollIndex = 0;
    private int maxScrollIndex = 0;

    public ModuleDetailScreen(Screen parent, Module module) {
        super(Text.of(module.getDisplayName()));
        this.parent = parent;
        this.module = module;
    }

    @Override
    protected void init() {
        settingWidgets.clear();

        int innerW = PANEL_W - PADDING * 2;
        descriptionLines = textRenderer.wrapLines(Text.of(module.getDescription()), innerW);

        boolean alwaysOn = module.isAlwaysOn();
        alwaysOnNoteLines = alwaysOn
                ? textRenderer.wrapLines(Text.of(ALWAYS_ON_NOTE), innerW - 12)
                : List.of();
        statusH = alwaysOn ? Math.max(ROW_H, alwaysOnNoteLines.size() * LINE_H + 8) : ROW_H;

        settings = module.getSettings();
        boolean editable = module instanceof EditableModule;
        showSettingsSection = !settings.isEmpty() || editable;
        emptySettingsLines = (showSettingsSection && settings.isEmpty())
                ? textRenderer.wrapLines(Text.of(EDITABLE_ELSEWHERE_NOTE), innerW)
                : List.of();

        settingOffsetY = new int[settings.size()];
        settingHeight = new int[settings.size()];
        int cumulative = 0;
        for (int i = 0; i < settings.size(); i++) {
            settingOffsetY[i] = cumulative;
            settingHeight[i] = rowHeight(settings.get(i));
            cumulative += settingHeight[i] + ROW_GAP;
        }
        int settingsContentH = settings.isEmpty()
                ? emptySettingsLines.size() * LINE_H + ROW_GAP
                : cumulative;

        int y = 0;
        y += 20;                                    // title
        y += 12;                                     // category badge row
        y += descriptionLines.size() * LINE_H + 6;    // description
        y += 6;                                       // divider
        statusY = y;
        y += statusH + ROW_GAP;                        // status row

        if (showSettingsSection) {
            y += 10;                                    // "Settings" label
        }
        settingsStartY = y;

        int preFooterH = y + settingsContentH;
        int footerH = 6 + 20; // bottom padding + button
        int desiredPanelH = preFooterH + footerH;
        int maxPanelH = height - 40;
        panelH = Math.min(desiredPanelH, maxPanelH);
        panelX = (width - PANEL_W) / 2;
        panelY = Math.max(20, (height - panelH) / 2);

        viewportH = panelH - settingsStartY - footerH;

        if (!alwaysOn) {
            SolsticeToggleRow enabledRow = new SolsticeToggleRow(panelX + PADDING, panelY + statusY, innerW, ROW_H,
                    textRenderer, "Enabled", module::isEnabled, module::setEnabled);
            enabledRow.setTooltip(Tooltip.of(Text.of("Turns this feature on or off.")));
            addDrawableChild(enabledRow);
        }

        for (ModuleSetting setting : settings) {
            int rx = panelX + PADDING;
            ClickableWidget widget = switch (setting) {
                case ModuleSetting.IntSetting s -> new SolsticeSliderWidget(
                        rx, 0, innerW, ROW_H, textRenderer, s.label(), s.min(), s.max(), s.getter().getAsInt(),
                        v -> String.valueOf((int) Math.round(v)),
                        v -> s.setter().accept((int) Math.round(v)));
                case ModuleSetting.DoubleSetting s -> new SolsticeSliderWidget(
                        rx, 0, innerW, ROW_H, textRenderer, s.label(), s.min(), s.max(), s.getter().getAsDouble(),
                        v -> percentOrPlain(s, v),
                        v -> s.setter().accept(v));
                case ModuleSetting.BooleanSetting s -> new SolsticeToggleRow(
                        rx, 0, innerW, ROW_H, textRenderer, s.label(), s.getter(), b -> s.setter().accept(b));
                case ModuleSetting.ChoiceSetting s -> new SolsticeChoiceRow(
                        rx, 0, innerW, ROW_H, textRenderer, s.label(), s.options(), s.indexGetter(), s.indexSetter());
                case ModuleSetting.KeySetting s -> new SolsticeKeybindRow(
                        rx, 0, innerW, ROW_H, textRenderer, s.label(), s.displayNameGetter(), s.keyCodeSetter());
                case ModuleSetting.ColorSetting s -> new SolsticeColorPickerWidget(
                        rx, 0, innerW, COLOR_ROW_H, textRenderer, s.label(), s.getter(), s.setter());
            };
            widget.setTooltip(Tooltip.of(Text.of(setting.tooltip())));
            addDrawableChild(widget);
            settingWidgets.add(widget);
        }

        computeScrollBounds();
        applyScroll();

        addDrawableChild(new SolsticeButton(panelX + (PANEL_W - 100) / 2, panelY + panelH - 24, 100, 18,
                textRenderer, "Back", this::close));
    }

    /** How many trailing settings fit together in the viewport, and the furthest valid scroll start. */
    private void computeScrollBounds() {
        int n = settings.size();
        if (n == 0) {
            visibleSettingsCount = 0;
            maxScrollIndex = 0;
            return;
        }

        int used = 0;
        int fitCount = 0;
        for (int i = n - 1; i >= 0; i--) {
            int h = settingHeight[i] + ROW_GAP;
            if (used + h > viewportH && fitCount > 0) {
                break;
            }
            used += h;
            fitCount++;
        }
        visibleSettingsCount = Math.max(1, fitCount);
        maxScrollIndex = Math.max(0, n - visibleSettingsCount);
        scrollIndex = Math.max(0, Math.min(scrollIndex, maxScrollIndex));
    }

    private void applyScroll() {
        int baseOffset = scrollIndex < settingOffsetY.length ? settingOffsetY[scrollIndex] : 0;
        for (int i = 0; i < settingWidgets.size(); i++) {
            ClickableWidget widget = settingWidgets.get(i);
            int relativeOffset = settingOffsetY[i] - baseOffset;
            boolean shown = i >= scrollIndex && relativeOffset + settingHeight[i] <= viewportH;
            widget.visible = shown;
            widget.active = shown;
            if (shown) {
                widget.setY(panelY + settingsStartY + relativeOffset);
            }
        }
    }

    private static int rowHeight(ModuleSetting setting) {
        return setting instanceof ModuleSetting.ColorSetting ? COLOR_ROW_H : ROW_H;
    }

    private static String percentOrPlain(ModuleSetting.DoubleSetting s, double v) {
        if (s.max() <= 1.0) {
            return String.format("%.0f%%", v * 100);
        }
        return String.format("%.2f", v);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int viewportTop = panelY + settingsStartY;
        if (maxScrollIndex > 0 && mouseX >= panelX && mouseX < panelX + PANEL_W
                && mouseY >= viewportTop && mouseY < viewportTop + viewportH) {
            int newScrollIndex = (int) Math.max(0, Math.min(maxScrollIndex, scrollIndex - Math.signum(verticalAmount)));
            if (newScrollIndex != scrollIndex) {
                scrollIndex = newScrollIndex;
                applyScroll();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Semi-transparent dark overlay (see SolsticeScreen for why blur isn't used here).
        context.fill(0, 0, width, height, ColorPalette.OVERLAY_DARK);

        SolsticeTheme.drawPanel(context, panelX, panelY, PANEL_W, panelH);

        int tx = panelX + PADDING;
        int ty = panelY + PADDING;

        context.drawText(textRenderer, module.getDisplayName(), tx, ty, ColorPalette.TEXT_PRIMARY, false);
        SolsticeTheme.drawCategoryBadge(context, tx, ty + 12, textRenderer, module.getCategory());

        int descY = ty + 24;
        for (OrderedText line : descriptionLines) {
            context.drawText(textRenderer, line, tx, descY, ColorPalette.TEXT_SECONDARY, false);
            descY += LINE_H;
        }

        int dividerY = descY + 2;
        SolsticeTheme.drawDivider(context, panelX + PADDING, dividerY, PANEL_W - PADDING * 2);

        if (module.isAlwaysOn()) {
            int statusAbsY = panelY + statusY;
            SolsticeTheme.fillRect(context, tx, statusAbsY, PANEL_W - PADDING * 2, statusH, ColorPalette.BG_CARD);
            int lineY = statusAbsY + (statusH - alwaysOnNoteLines.size() * LINE_H) / 2;
            for (OrderedText line : alwaysOnNoteLines) {
                context.drawText(textRenderer, line, tx + 6, lineY, ColorPalette.TEXT_SECONDARY, false);
                lineY += LINE_H;
            }
        }

        if (showSettingsSection) {
            int settingsLabelY = panelY + settingsStartY - 10;
            context.drawText(textRenderer, "Settings", tx, settingsLabelY, ColorPalette.TEXT_ACCENT, false);

            if (settings.isEmpty()) {
                int lineY = panelY + settingsStartY + 2;
                for (OrderedText line : emptySettingsLines) {
                    context.drawText(textRenderer, line, tx, lineY, ColorPalette.TEXT_DISABLED, false);
                    lineY += LINE_H;
                }
            } else if (maxScrollIndex > 0) {
                int trackX = panelX + PANEL_W - PADDING + 3;
                int viewportTop = panelY + settingsStartY;
                SolsticeTheme.fillRect(context, trackX, viewportTop, 3, viewportH, ColorPalette.BORDER_DEFAULT);
                int thumbH = Math.max(10, viewportH * visibleSettingsCount / settings.size());
                int thumbY = viewportTop + (viewportH - thumbH) * scrollIndex / maxScrollIndex;
                SolsticeTheme.fillRect(context, trackX, thumbY, 3, thumbH, ColorPalette.ACCENT_DIM);
            }
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
