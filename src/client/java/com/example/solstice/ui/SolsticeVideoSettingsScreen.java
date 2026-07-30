package com.example.solstice.ui;

import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import com.example.solstice.ui.widget.SolsticeButton;
import com.example.solstice.ui.widget.SolsticeChoiceRow;
import com.example.solstice.ui.widget.SolsticeSliderWidget;
import com.example.solstice.ui.widget.SolsticeToggleRow;
import com.example.solstice.viewdistance.ViewDistanceModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Solstice's own themed Video Settings screen, replacing vanilla's when
 * opened from the main Options menu (see {@code VideoOptionsRedirectMixin}).
 *
 * <p>A curated core set of the most commonly-tweaked options, styled with
 * Solstice's existing widgets - not a full replacement of every vanilla
 * video option (biome blend, entity shadows, attack indicator, etc. stay on
 * vanilla's own screen, reachable via "More Options..." below). Each option
 * reads/writes vanilla's own {@link GameOptions} {@link SimpleOption}
 * directly - Solstice doesn't shadow or duplicate vanilla's settings
 * storage, it's just a themed front end over the same options vanilla
 * already persists to {@code options.txt}.</p>
 *
 * <p>Inspired by Sodium's video settings screen (verified against its real
 * source): a persistent side panel shows a plain-language description of
 * whatever option the mouse is currently over, instead of vanilla's
 * hover-tooltip convention.</p>
 */
public class SolsticeVideoSettingsScreen extends Screen {

    private static final int HEADER_H = 32;
    private static final int TAB_H = 24;
    private static final int FOOTER_H = 28;
    private static final int SIDE_PANEL_W = 160;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 6;
    private static final int SIDE_PAD = 12;

    /** Set around the one legitimate direct construction of vanilla's real screen, so the redirect Mixin lets it through. */
    public static volatile boolean allowVanillaScreen = false;

    private final Screen parent;
    private int activeTab = 0;

    private final List<OptionRow> rows = new ArrayList<>();
    private String hoveredDescription;

    private record OptionRow(ClickableWidget widget, String description) {}

    public SolsticeVideoSettingsScreen(Screen parent) {
        super(Text.of("Video Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rows.clear();

        addDrawableChild(new SolsticeButton(SIDE_PAD, HEADER_H, 90, TAB_H, textRenderer, "General",
                () -> selectTab(0)));
        addDrawableChild(new SolsticeButton(SIDE_PAD + 94, HEADER_H, 90, TAB_H, textRenderer, "Quality",
                () -> selectTab(1)));

        buildTab();

        addDrawableChild(new SolsticeButton(width / 2 - 130, height - FOOTER_H - 4, 120, 20,
                textRenderer, "Done", this::close));
        addDrawableChild(new SolsticeButton(width / 2 + 10, height - FOOTER_H - 4, 120, 20,
                textRenderer, "More Options...", this::openVanillaScreen));
    }

    private void selectTab(int tab) {
        activeTab = tab;
        rows.forEach(r -> remove(r.widget()));
        rows.clear();
        buildTab();
    }

    private void buildTab() {
        GameOptions options = MinecraftClient.getInstance().options;
        int contentW = width - SIDE_PANEL_W - SIDE_PAD * 3;
        int y = HEADER_H + TAB_H + ROW_GAP;

        if (activeTab == 0) {
            y = addIntRow(options.getViewDistance(), "Render Distance", "How many chunks of terrain load around you. Higher looks further but costs more FPS.", 2, 32, SIDE_PAD, y, contentW);
            y = addIntRow(ViewDistanceModule.extraChunks, "Extra Chunks (Solstice)", "How many extra chunks beyond your render distance Solstice keeps visible once already explored, instead of them popping out immediately.", 0, 16, SIDE_PAD, y, contentW, ViewDistanceModule.getInstance()::setExtraChunks);
            y = addIntRow(options.getSimulationDistance(), "Simulation Distance", "How far from you the game actually simulates things like crops growing or redstone - always at least as small as render distance.", 5, 32, SIDE_PAD, y, contentW);
            y = addDoubleRow(options.getEntityDistanceScaling(), "Entity Distance", "How far away entities (mobs, players, item drops) stay visible, as a percentage.", 0.5, 5.0, SIDE_PAD, y, contentW);
            y = addMaxFpsRow(options.getMaxFps(), SIDE_PAD, y, contentW);
            addBoolRow(options.getEnableVsync(), "VSync", "Locks your framerate to your monitor's refresh rate to prevent screen tearing, at the cost of some input latency.", SIDE_PAD, y, contentW);
        } else {
            y = addChoiceRow(() -> options.getParticles().getValue().ordinal(),
                    v -> options.getParticles().setValue(ParticlesMode.values()[v]),
                    "Particles", "How many particle effects (smoke, splashes, etc.) are shown. Lower can help FPS in busy scenes.",
                    List.of("All", "Decreased", "Minimal"), SIDE_PAD, y, contentW);
            y = addBoolRow(options.getAo(), "Smooth Lighting", "Softens lighting between blocks instead of a hard, blocky shadow edge.", SIDE_PAD, y, contentW);
            y = addChoiceRow(() -> options.getCloudRenderMode().getValue().ordinal(),
                    v -> options.getCloudRenderMode().setValue(CloudRenderMode.values()[v]),
                    "Clouds", "Whether clouds render at all, and how detailed they look.",
                    List.of("Off", "Fast", "Fancy"), SIDE_PAD, y, contentW);
            y = addDoubleRow(options.getGamma(), "Brightness", "How bright dark areas appear. Doesn't affect actual light levels, just how they're displayed.", 0.0, 1.0, SIDE_PAD, y, contentW);
            addIntRow(options.getGuiScale(), "GUI Scale", "How large Solstice's and vanilla's menus render. 0 lets the game pick automatically.", 0, 4, SIDE_PAD, y, contentW);
        }
    }

    private int addIntRow(SimpleOption<Integer> option, String label, String description, int min, int max, int x, int y, int w) {
        return addIntRow(option.getValue(), label, description, min, max, x, y, w, option::setValue);
    }

    private int addIntRow(int current, String label, String description, int min, int max, int x, int y, int w,
                           java.util.function.IntConsumer setter) {
        SolsticeSliderWidget widget = new SolsticeSliderWidget(x, y, w, ROW_H, textRenderer, label, min, max, current,
                v -> String.valueOf((int) Math.round(v)), v -> setter.accept((int) Math.round(v)));
        addRow(widget, description);
        return y + ROW_H + ROW_GAP;
    }

    private int addMaxFpsRow(SimpleOption<Integer> option, int x, int y, int w) {
        // GameOptions.MAX_FPS_LIMIT is vanilla's own real cap (its slider shows "Unlimited" at
        // this exact value) - using the constant directly instead of a guessed number.
        int max = GameOptions.MAX_FPS_LIMIT;
        SolsticeSliderWidget widget = new SolsticeSliderWidget(x, y, w, ROW_H, textRenderer, "Max Framerate", 10, max, option.getValue(),
                v -> (int) Math.round(v) >= max ? "Unlimited" : String.valueOf((int) Math.round(v)),
                v -> option.setValue((int) Math.round(v)));
        addRow(widget, "Caps how many frames per second the game will render. Lower can save power/heat.");
        return y + ROW_H + ROW_GAP;
    }

    private int addDoubleRow(SimpleOption<Double> option, String label, String description, double min, double max, int x, int y, int w) {
        SolsticeSliderWidget widget = new SolsticeSliderWidget(x, y, w, ROW_H, textRenderer, label, min, max, option.getValue(),
                v -> String.format("%.0f%%", v * 100), option::setValue);
        addRow(widget, description);
        return y + ROW_H + ROW_GAP;
    }

    private int addBoolRow(SimpleOption<Boolean> option, String label, String description, int x, int y, int w) {
        SolsticeToggleRow widget = new SolsticeToggleRow(x, y, w, ROW_H, textRenderer, label, option::getValue, option::setValue);
        addRow(widget, description);
        return y + ROW_H + ROW_GAP;
    }

    private int addChoiceRow(java.util.function.IntSupplier currentIndexSupplier, java.util.function.IntConsumer setter,
                              String label, String description, List<String> options, int x, int y, int w) {
        SolsticeChoiceRow widget = new SolsticeChoiceRow(x, y, w, ROW_H, textRenderer, label, options,
                currentIndexSupplier, setter);
        addRow(widget, description);
        return y + ROW_H + ROW_GAP;
    }

    private void addRow(ClickableWidget widget, String description) {
        rows.add(new OptionRow(widget, description));
        addDrawableChild(widget);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, ColorPalette.OVERLAY_DARK);

        SolsticeTheme.fillRect(context, 0, 0, width, HEADER_H, ColorPalette.BG_DEEP);
        context.drawText(textRenderer, "Video Settings", SIDE_PAD, (HEADER_H - 8) / 2, ColorPalette.ACCENT_NEUTRAL, false);
        SolsticeTheme.drawDivider(context, 0, HEADER_H + TAB_H, width);

        int panelX = width - SIDE_PANEL_W - SIDE_PAD;
        SolsticeTheme.drawPanel(context, panelX, HEADER_H + TAB_H + ROW_GAP, SIDE_PANEL_W, height - HEADER_H - TAB_H - FOOTER_H - ROW_GAP * 2);

        hoveredDescription = null;
        for (OptionRow row : rows) {
            if (row.widget().isHovered()) {
                hoveredDescription = row.description();
                break;
            }
        }

        if (hoveredDescription != null) {
            List<OrderedText> lines = textRenderer.wrapLines(Text.of(hoveredDescription), SIDE_PANEL_W - SIDE_PAD * 2);
            int ty = HEADER_H + TAB_H + ROW_GAP + SIDE_PAD;
            for (OrderedText line : lines) {
                context.drawText(textRenderer, line, panelX + SIDE_PAD, ty, ColorPalette.TEXT_SECONDARY, false);
                ty += 10;
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void openVanillaScreen() {
        allowVanillaScreen = true;
        try {
            assert client != null;
            client.setScreen(new VideoOptionsScreen(parent, client, client.options));
        } finally {
            allowVanillaScreen = false;
        }
    }

    @Override
    public void close() {
        assert client != null;
        client.setScreen(parent);
    }

    // No shouldPause() override - see SolsticeScreen's identical note.
}
