package com.example.solstice.ui;

import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import com.example.solstice.ui.widget.ModCardWidget;
import com.example.solstice.ui.widget.SolsticeButton;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The "Mods" screen, opened via real buttons injected into vanilla's own
 * {@link net.minecraft.client.gui.screen.TitleScreen} and
 * {@link net.minecraft.client.gui.screen.GameMenuScreen} (see {@code mixin.modmenu}),
 * the same way the real ModMenu mod surfaces its mod list.
 *
 * <p>A grid of horizontal {@link ModCardWidget} boxes (icon, name, one-line
 * description) rather than a plain scrolling list - clicking a card opens
 * {@link ModDetailScreen} for the full picture, matching the real ModMenu
 * mod's own click-for-details UX. Scrolls a whole row at a time, same
 * mechanism as {@link VisualsEditScreen} - rows toggle {@code visible}/{@code
 * active} together, which also blocks clicks on hidden rows for free.</p>
 *
 * <p>Only shows what the user actually installed: their own mods plus a
 * single "Fabric API" entry - not its ~40 internal submodules, Fabric Loader,
 * Minecraft, Java, or jar-in-jar embedded dependencies nobody chose to
 * install (see {@link #isInstalledMod}). Read-only: no per-mod config-screen
 * buttons - that needs a real compile dependency on ModMenu's own API to
 * discover other mods' config screens, which isn't something this project
 * takes on just for this screen.</p>
 */
public class SolsticeModsScreen extends Screen {

    private static final int TOP = 32;
    private static final int BOTTOM_MARGIN = 32;
    private static final int SIDE_PAD = 16;
    private static final int CARD_GAP = 8;
    private static final int TARGET_CARD_W = 340;
    private static final int ROW_STEP = ModCardWidget.HEIGHT + CARD_GAP;

    private final Screen parent;
    private final List<ClickableWidget> cards = new ArrayList<>();
    private final List<Integer> cardRows = new ArrayList<>();
    private int cols, viewportTop, visibleRowCount, maxScrollRow, scrollRow;

    public SolsticeModsScreen(Screen parent) {
        super(Text.literal("Mods"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        cards.clear();
        cardRows.clear();

        List<ModContainer> mods = FabricLoader.getInstance().getAllMods().stream()
                .filter(SolsticeModsScreen::isInstalledMod)
                .sorted(Comparator.comparing(m -> m.getMetadata().getName(), String.CASE_INSENSITIVE_ORDER))
                .toList();

        int totalW = width - SIDE_PAD * 2;
        cols = Math.max(1, (totalW + CARD_GAP) / (TARGET_CARD_W + CARD_GAP));
        int cardW = (totalW - (cols - 1) * CARD_GAP) / cols;

        viewportTop = TOP;
        int viewportH = height - BOTTOM_MARGIN - TOP;
        visibleRowCount = Math.max(1, viewportH / ROW_STEP);
        int totalRows = (mods.size() + cols - 1) / cols;
        maxScrollRow = Math.max(0, totalRows - visibleRowCount);
        scrollRow = Math.min(scrollRow, maxScrollRow);

        for (int i = 0; i < mods.size(); i++) {
            ModContainer mod = mods.get(i);
            int col = i % cols;
            int row = i / cols;
            int cx = SIDE_PAD + col * (cardW + CARD_GAP);

            ModCardWidget card = new ModCardWidget(cx, 0, cardW, mod, client, textRenderer,
                    () -> { assert client != null; client.setScreen(new ModDetailScreen(this, mod)); });
            cards.add(card);
            cardRows.add(row);
            addDrawableChild(card);
        }
        updateScrollPositions();

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .position(width / 2 - 100, height - 26)
                .size(200, 20)
                .build());
    }

    private void updateScrollPositions() {
        for (int i = 0; i < cards.size(); i++) {
            ClickableWidget widget = cards.get(i);
            int visualRow = cardRows.get(i) - scrollRow;
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
        int viewportH = visibleRowCount * ROW_STEP - CARD_GAP;
        if (maxScrollRow > 0 && mouseY >= viewportTop && mouseY < viewportTop + viewportH) {
            int newScrollRow = (int) Math.max(0, Math.min(maxScrollRow, scrollRow - Math.signum(verticalAmount)));
            if (newScrollRow != scrollRow) {
                scrollRow = newScrollRow;
                updateScrollPositions();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    /**
     * True for real, user-facing mods: the user's own installed mods, plus the
     * single "Fabric API" entry itself. False for Fabric API's own ~40 internal
     * submodules, Fabric Loader, Minecraft, Java, Loom-generated entries, and
     * jar-in-jar embedded mods nested inside another mod's jar (forks/bundled
     * copies a mod ships internally, not something the user chose to install).
     */
    private static boolean isInstalledMod(ModContainer container) {
        ModMetadata meta = container.getMetadata();
        String id = meta.getId();

        if (id.equals("fabric-api")) {
            return true;
        }
        if (id.equals("fabricloader") || id.equals("minecraft") || id.equals("java")) {
            return false;
        }
        if (id.startsWith("fabric") && meta.containsCustomValue("fabric-api:module-lifecycle")) {
            return false;
        }
        if (meta.containsCustomValue("fabric-loom:generated")) {
            return false;
        }
        if ("builtin".equals(meta.getType())) {
            return false;
        }
        return container.getOrigin().getKind() != ModOrigin.Kind.NESTED;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Flat dim instead of vanilla's renderBackground()/applyBlur() - this screen can be
        // opened while a blur pass has already been claimed elsewhere in the same frame
        // (e.g. from the paused game world), and Minecraft only allows one per frame. Same
        // workaround already used by SolsticeScreen/ModuleDetailScreen for this exact reason.
        context.fill(0, 0, width, height, ColorPalette.OVERLAY_DARK);
        context.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, 10, 0xFFFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        assert client != null;
        client.setScreen(parent);
    }
}
