package com.example.solstice.ui;

import com.example.solstice.SolsticeMod;
import com.example.solstice.core.module.Module;
import com.example.solstice.core.module.ModuleCategory;
import com.example.solstice.core.module.ModuleRegistry;
import com.example.solstice.profiles.Profile;
import com.example.solstice.profiles.ProfileCategory;
import com.example.solstice.profiles.ProfileManager;
import com.example.solstice.profiles.Renameable;
import com.example.solstice.textures.ResourcePackFileChooser;
import com.example.solstice.textures.TexturePackManager;
import com.example.solstice.textures.TexturePreset;
import com.example.solstice.textures.TexturePresetManager;
import com.example.solstice.textures.TextureSlots;
import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import com.example.solstice.ui.widget.AddPackCardWidget;
import com.example.solstice.ui.widget.CategoryTabWidget;
import com.example.solstice.ui.widget.DraftProfileCardWidget;
import com.example.solstice.ui.widget.ModuleCardWidget;
import com.example.solstice.ui.widget.ProfileCardWidget;
import com.example.solstice.ui.widget.SavedComboCardWidget;
import com.example.solstice.ui.widget.SolsticeButton;
import com.example.solstice.ui.widget.TextureCategoryCardWidget;
import com.example.solstice.ui.widget.TexturePresetCardWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Solstice settings screen.
 *
 * <p>Layout:
 * <pre>
 * ┌────────────────────────────────────────────────────────────────┐
 * │  ☀ SOLSTICE  v1.0.0                                             │
 * ├──────────────────────┬───────────────────────────────────────┤
 * │      Optimization     │           Quality of Life              │
 * ├──────────────────────┴───────────────────────────────────────┤
 * │ [ModuleCard] [ModuleCard] [ModuleCard]                         │
 * │ [ModuleCard] [ModuleCard]                                      │
 * │                                                                │
 * │                 [Done]              [Edit HUD Layout]         │
 * └────────────────────────────────────────────────────────────────┘
 * </pre>
 */
public class SolsticeScreen extends Screen {

    private static final int HEADER_H    = 32;
    private static final int TAB_H       = 24;
    private static final int TAB_GAP     = 4;
    private static final int FOOTER_H    = 22;
    private static final int CARD_COLS   = 4;
    private static final int CARD_GAP    = 6;
    private static final int SIDE_PAD    = 12;
    private static final int SEARCH_H    = 16;
    private static final int GLOBAL_SEARCH_W = 150;

    private final Screen parent;
    private ModuleCategory activeCategory = ModuleCategory.PROFILES;

    private final List<CategoryTabWidget> tabs = new ArrayList<>();
    private final List<ClickableWidget>   cards = new ArrayList<>();

    /**
     * Global search (header, right side) - matches modules across every tab,
     * regardless of which one is active. Non-blank text short-circuits
     * {@link #rebuildCards()} into a flat cross-category results grid.
     */
    private TextFieldWidget globalSearchField;
    /**
     * Per-tab search - only shown/active on tabs that actually list
     * {@link Module}s (Quality of Life, Advanced); hidden (not destroyed -
     * recreating it on every keystroke would drop focus/cursor position,
     * since its own changed-listener is what triggers a rebuild) on the
     * Profiles/Textures tabs, which don't.
     */
    private TextFieldWidget tabSearchField;

    /** One entry per Profiles-tab row, drawn as a small label above that row's cards. */
    private record ProfileRowLabel(String text, int x, int y) {}
    private final List<ProfileRowLabel> profileRowLabels = new ArrayList<>();

    /**
     * Y of the delimiter line above the footer buttons - also the hard clip
     * boundary for the Quality of Life/Advanced module grid, which scrolls a
     * whole row at a time (like {@code VisualsEditScreen}) rather than ever
     * drawing a card below this line. See {@link #render}'s small chevron hint
     * for how "there's more to scroll to" is signaled instead.
     */
    private int footerButtonY;
    private int moduleContentViewportTop;
    private int moduleVisibleRows;
    private int moduleMaxScrollRow;
    private int moduleScrollRow;

    /**
     * Each module card's fully-settled ("natural") Y, parallel to {@link #cards} whenever
     * the module grid is active (empty for every other tab). {@link #render} temporarily
     * offsets each card's real Y from this by {@link #scrollAnimOffsetPx}, which decays to
     * 0 over a short window after every scroll tick - previously the whole row snapped to
     * its new position in one frame, which read as an instant screen-switch rather than a
     * scroll. Offsetting the real widget Y (not a matrix translate) was chosen deliberately
     * over wrapping the render call in a transform/scissor - see the reverted attempt at
     * exactly that for the "partial row" feature, which broke the category tabs outright.
     */
    private final List<Integer> cardNaturalY = new ArrayList<>();
    private float scrollAnimOffsetPx;
    private long lastScrollAnimNanos;
    private static final float SCROLL_ANIM_DECAY_PER_SECOND = 16f;

    /** Pixel-based scroll for the Textures tab - see {@link #rebuildTexturesCards}. */
    private int texturesViewportTop;
    private int texturesScrollOffset;
    private int texturesMaxScroll;

    public SolsticeScreen(Screen parent) {
        super(Text.literal("Solstice"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        tabs.clear();
        cards.clear();

        int totalW = width - SIDE_PAD * 2;
        ModuleCategory[] cats = ModuleCategory.values();
        int tabW = (totalW - (cats.length - 1) * TAB_GAP) / cats.length;

        // Computed before rebuildCards() - the module grid clips against this.
        footerButtonY = height - FOOTER_H + 2;

        // ── Category tabs ──────────────────────────────────────────────────
        for (int i = 0; i < cats.length; i++) {
            ModuleCategory cat = cats[i];
            CategoryTabWidget tab = new CategoryTabWidget(
                    SIDE_PAD + i * (tabW + TAB_GAP),
                    HEADER_H,
                    tabW,
                    cat,
                    cat == activeCategory,
                    textRenderer,
                    this::selectCategory
            );
            tabs.add(tab);
            addDrawableChild(tab);
        }

        // ── Search fields (created once - rebuildCards() only touches `cards`,
        // never these, so typing never drops focus/cursor position) ────────
        globalSearchField = new TextFieldWidget(textRenderer,
                width - SIDE_PAD - GLOBAL_SEARCH_W, (HEADER_H - SEARCH_H) / 2,
                GLOBAL_SEARCH_W, SEARCH_H, Text.of("Search all modules"));
        globalSearchField.setMaxLength(48);
        globalSearchField.setPlaceholder(Text.literal("Search all modules...").formatted(net.minecraft.util.Formatting.DARK_GRAY));
        globalSearchField.setChangedListener(s -> rebuildCards());
        addDrawableChild(globalSearchField);

        tabSearchField = new TextFieldWidget(textRenderer,
                SIDE_PAD, HEADER_H + TAB_H + CARD_GAP, totalW, SEARCH_H, Text.of("Search this tab"));
        tabSearchField.setMaxLength(48);
        tabSearchField.setPlaceholder(Text.literal("Search this tab...").formatted(net.minecraft.util.Formatting.DARK_GRAY));
        tabSearchField.setChangedListener(s -> rebuildCards());
        addDrawableChild(tabSearchField);

        // ── Module cards ───────────────────────────────────────────────────
        rebuildCards();

        // ── Close button ───────────────────────────────────────────────────
        // No footer bar behind these anymore (removed - it visually collided with
        // the buttons' own translucent backgrounds, see render()) - just a bottom
        // margin over the screen's normal full-bleed dim.
        addDrawableChild(new SolsticeButton(width / 2 - 60, footerButtonY, 120, 20,
                textRenderer, "Done", this::close));

        // ── HUD editor entry point ────────────────────────────────────────
        addDrawableChild(new SolsticeButton(width - SIDE_PAD - 110, footerButtonY, 110, 20,
                textRenderer, "Edit HUD Layout", () -> {
                    assert client != null;
                    client.setScreen(new HudEditorScreen(this));
                }));

        // Mods entry point deliberately removed from here - see SolsticeModsScreen's
        // Javadoc, it's now reached from real buttons on vanilla's Title/Pause menus
        // (mixin.modmenu package), not from inside Solstice's own GUI.
    }

    private void selectCategory(ModuleCategory cat) {
        activeCategory = cat;
        moduleScrollRow = 0;
        texturesScrollOffset = 0;
        tabs.forEach(t -> t.setSelected(t.getCategory() == cat));
        rebuildCards();
    }

    private void rebuildCards() {
        // Remove old cards (search fields are never in this list - see their
        // own field Javadocs for why they must survive every rebuild)
        cards.forEach(this::remove);
        cards.clear();
        cardNaturalY.clear();
        profileRowLabels.clear();

        int contentTop = HEADER_H + TAB_H + CARD_GAP;
        int totalW     = width - SIDE_PAD * 2;
        int cardW      = (totalW - (CARD_COLS - 1) * CARD_GAP) / CARD_COLS;

        String globalQuery = globalSearchField != null ? globalSearchField.getText() : "";
        if (!globalQuery.isBlank()) {
            tabSearchField.visible = false;
            tabSearchField.active  = false;
            rebuildGlobalSearchResults(globalQuery, contentTop, cardW);
            return;
        }

        if (activeCategory == ModuleCategory.TEXTURES) {
            tabSearchField.visible = false;
            tabSearchField.active  = false;
            rebuildTexturesCards(contentTop, cardW);
            return;
        }
        if (activeCategory == ModuleCategory.PROFILES) {
            tabSearchField.visible = false;
            tabSearchField.active  = false;
            rebuildProfileCards(contentTop, cardW);
            return;
        }

        tabSearchField.visible = true;
        tabSearchField.active  = true;
        int moduleContentTop = contentTop + SEARCH_H + CARD_GAP;
        moduleContentViewportTop = moduleContentTop;

        List<Module> modules = ModuleRegistry.getInstance().getByCategory(activeCategory).stream()
                .filter(m -> matchesQuery(m, tabSearchField.getText()))
                .collect(Collectors.toList());

        // Scrolls a whole row at a time rather than ever drawing a card past
        // footerButtonY (the delimiter line above Done/Edit HUD Layout) - the
        // same no-scissor-needed technique VisualsEditScreen already uses,
        // just applied to a multi-column grid instead of one row per widget.
        // A scissor-cropped partial overhang row was tried here as a "more
        // below" hint and reverted - it visually broke the category tabs at
        // the top of the screen (rendered invisible while still clickable),
        // for reasons not fully root-caused. See render()'s "more below"
        // indicator instead for the same hint without touching scissor state.
        int rowStep = ModuleCardWidget.HEIGHT + CARD_GAP;
        int viewportH = Math.max(ModuleCardWidget.HEIGHT, footerButtonY - moduleContentTop - CARD_GAP);
        moduleVisibleRows = Math.max(1, (viewportH + CARD_GAP) / rowStep);
        int totalRows = (int) Math.ceil(modules.size() / (double) CARD_COLS);
        moduleMaxScrollRow = Math.max(0, totalRows - moduleVisibleRows);
        moduleScrollRow = Math.max(0, Math.min(moduleScrollRow, moduleMaxScrollRow));

        for (int i = 0; i < modules.size(); i++) {
            int col = i % CARD_COLS;
            int row = i / CARD_COLS;
            int visualRow = row - moduleScrollRow;
            if (visualRow < 0 || visualRow >= moduleVisibleRows) {
                continue;
            }

            int cx = SIDE_PAD + col * (cardW + CARD_GAP);
            int cy = moduleContentTop + visualRow * rowStep;

            ModuleCardWidget card = new ModuleCardWidget(cx, cy, cardW,
                    modules.get(i), textRenderer,
                    m -> {
                        assert client != null;
                        client.setScreen(new ModuleDetailScreen(this, m));
                    });
            cards.add(card);
            cardNaturalY.add(cy);
            addDrawableChild(card);
        }
    }

    /**
     * Global search results - a flat grid of every module matching the
     * global query, from every category at once, ignoring which tab is
     * currently selected (the tabs stay visually as-is, just not consulted).
     */
    private void rebuildGlobalSearchResults(String query, int contentTop, int cardW) {
        List<Module> matches = ModuleRegistry.getInstance().getAllModules().stream()
                .filter(m -> matchesQuery(m, query))
                .collect(Collectors.toList());

        for (int i = 0; i < matches.size(); i++) {
            int col = i % CARD_COLS;
            int row = i / CARD_COLS;
            int cx  = SIDE_PAD + col * (cardW + CARD_GAP);
            int cy  = contentTop + row * (ModuleCardWidget.HEIGHT + CARD_GAP);

            ModuleCardWidget card = new ModuleCardWidget(cx, cy, cardW,
                    matches.get(i), textRenderer,
                    m -> {
                        assert client != null;
                        client.setScreen(new ModuleDetailScreen(this, m));
                    });
            cards.add(card);
            addDrawableChild(card);
        }
    }

    /**
     * Case-insensitive substring match against display name, description,
     * and {@link Module#getSearchKeywords()} - a blank query always matches
     * (so an empty search field shows every module, unfiltered).
     */
    private static boolean matchesQuery(Module module, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String q = query.toLowerCase(Locale.ROOT);
        if (module.getDisplayName().toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }
        if (module.getDescription().toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }
        for (String keyword : module.getSearchKeywords()) {
            if (keyword.toLowerCase(Locale.ROOT).contains(q)) {
                return true;
            }
        }
        return false;
    }

    /** One row/label/card whose real screen position depends on the current Textures-tab scroll offset - see {@link #rebuildTexturesCards}. */
    private record TexturesItem(int naturalY, int height, java.util.function.IntConsumer place) {}

    /**
     * The Textures tab doesn't list {@link Module}s at all - it shows two
     * rows: "Presets" (whole real resource packs, mutually exclusive - see
     * {@link TexturePresetManager}) and "Advanced" (one card per texture
     * category - Tools, Utilities, Armor, GUI, Fonts - each opening a
     * {@link TextureCategoryScreen} listing that category's swappable
     * slots, independent of whichever Preset is active).
     *
     * <p>Laid out in two passes: first every row/label/card is computed at
     * its natural (unscrolled) Y into a flat list, which also gives the
     * real total content height; then the current scroll offset (see
     * {@link #mouseScrolled}) is subtracted from each and only the ones
     * that land inside {@code [contentTop, footerButtonY]} actually get
     * built - matching the same "never draw past the line" approach the
     * module grid already uses, just pixel-based instead of row-snapped
     * since Presets/Advanced rows aren't uniform height.</p>
     */
    private void rebuildTexturesCards(int contentTop, int cardW) {
        texturesViewportTop = contentTop;
        List<TexturesItem> items = new ArrayList<>();

        int y = layoutPresetsRow(items, 0, cardW);
        y = layoutAdvancedRow(items, y + 14, cardW);

        int viewportH = Math.max(0, footerButtonY - contentTop);
        texturesMaxScroll = Math.max(0, y - viewportH);
        texturesScrollOffset = Math.max(0, Math.min(texturesScrollOffset, texturesMaxScroll));

        for (TexturesItem item : items) {
            int screenY = contentTop + item.naturalY() - texturesScrollOffset;
            if (screenY + item.height() < contentTop || screenY > footerButtonY) {
                continue;
            }
            item.place().accept(screenY);
        }
    }

    private int layoutPresetsRow(List<TexturesItem> items, int labelY, int cardW) {
        items.add(new TexturesItem(labelY, 12, screenY ->
                profileRowLabels.add(new ProfileRowLabel("Presets", SIDE_PAD, screenY))));
        int rowTop = labelY + 12;

        TexturePresetManager presets = TexturePresetManager.getInstance();
        TexturePreset selected = presets.getSelected();

        List<TexturePreset> allPresets = presets.getAllPresets();
        int totalW = width - SIDE_PAD * 2;
        int maxPerRow = Math.max(1, (totalW + CARD_GAP) / (cardW + CARD_GAP));

        int col = 0;
        int row = 0;
        for (TexturePreset preset : allPresets) {
            int cx = SIDE_PAD + col * (cardW + CARD_GAP);
            int naturalY = rowTop + row * (TexturePresetCardWidget.HEIGHT + CARD_GAP);
            boolean isSelected = preset.id().equals(selected.id());
            items.add(new TexturesItem(naturalY, TexturePresetCardWidget.HEIGHT, screenY -> {
                TexturePresetCardWidget card = new TexturePresetCardWidget(cx, screenY, cardW, preset, textRenderer,
                        isSelected, () -> {
                            presets.select(preset);
                            rebuildCards();
                        });
                cards.add(card);
                addDrawableChild(card);
            }));
            col++;
            if (col >= maxPerRow) { col = 0; row++; }
        }

        int addCol = col, addRow = row;
        int addX = SIDE_PAD + addCol * (cardW + CARD_GAP);
        int addNaturalY = rowTop + addRow * (TexturePresetCardWidget.HEIGHT + CARD_GAP);
        items.add(new TexturesItem(addNaturalY, TexturePresetCardWidget.HEIGHT, screenY -> {
            AddPackCardWidget addCard = new AddPackCardWidget(addX, screenY, cardW, textRenderer, this::openAddPackPicker);
            cards.add(addCard);
            addDrawableChild(addCard);
        }));
        col++;
        if (col >= maxPerRow) { col = 0; row++; }

        for (TexturePresetManager.SavedCombo combo : presets.getSavedCombos()) {
            int cx = SIDE_PAD + col * (cardW + CARD_GAP);
            int naturalY = rowTop + row * (TexturePresetCardWidget.HEIGHT + CARD_GAP);
            items.add(new TexturesItem(naturalY, TexturePresetCardWidget.HEIGHT, screenY -> {
                SavedComboCardWidget card = new SavedComboCardWidget(cx, screenY, cardW, combo, textRenderer, false, () -> {
                    presets.applyCombo(combo);
                    rebuildCards();
                });
                cards.add(card);
                addDrawableChild(card);
            }));
            col++;
            if (col >= maxPerRow) { col = 0; row++; }
        }

        int lineCount = row + (col > 0 ? 1 : 0);
        if (lineCount == 0) lineCount = 1;
        int rowBottom = rowTop + lineCount * TexturePresetCardWidget.HEIGHT + (lineCount - 1) * CARD_GAP;

        int saveBtnY = rowBottom + 6;
        items.add(new TexturesItem(saveBtnY, 16, screenY -> {
            SolsticeButton saveBtn = new SolsticeButton(SIDE_PAD, screenY, 160, 16, textRenderer,
                    "Save Current as Preset", this::saveCurrentPreset);
            cards.add(saveBtn);
            addDrawableChild(saveBtn);
        }));

        return saveBtnY + 16;
    }

    private int layoutAdvancedRow(List<TexturesItem> items, int labelY, int cardW) {
        items.add(new TexturesItem(labelY, 12, screenY ->
                profileRowLabels.add(new ProfileRowLabel("Advanced", SIDE_PAD, screenY))));
        int rowTop = labelY + 12;

        record Category(String name, String description, java.util.List<com.example.solstice.textures.TextureSlot> slots) {}
        List<Category> categories = List.of(
                new Category("Tools", "Swap tool textures (weapons, etc).", TextureSlots.TOOLS),
                new Category("Utilities", "Cobwebs, bow/crossbow reskin, misc items.", TextureSlots.UTILITIES),
                new Category("Armor", "Swap worn armor textures.", TextureSlots.ARMOR),
                new Category("GUI", "Swap the inventory/container GUI skin.", TextureSlots.GUI),
                new Category("Fonts", "Swap the game's font.", TextureSlots.FONTS)
        );

        int maxRow = 0;
        for (int i = 0; i < categories.size(); i++) {
            Category cat = categories.get(i);
            int col = i % CARD_COLS;
            int row = i / CARD_COLS;
            maxRow = Math.max(maxRow, row);
            int cx  = SIDE_PAD + col * (cardW + CARD_GAP);
            int naturalY = rowTop + row * (TextureCategoryCardWidget.HEIGHT + CARD_GAP);

            items.add(new TexturesItem(naturalY, TextureCategoryCardWidget.HEIGHT, screenY -> {
                TextureCategoryCardWidget card = new TextureCategoryCardWidget(cx, screenY, cardW,
                        cat.name(), cat.description(), textRenderer, () -> {
                            assert client != null;
                            client.setScreen(new TextureCategoryScreen(this, cat.name(), cat.slots()));
                        });
                cards.add(card);
                addDrawableChild(card);
            }));
        }

        return rowTop + (maxRow + 1) * (TextureCategoryCardWidget.HEIGHT + CARD_GAP);
    }

    private void openAddPackPicker() {
        ResourcePackFileChooser.pick(path -> {
            TexturePreset added = TexturePresetManager.getInstance().addPackFromDisk(path);
            if (added != null) {
                TexturePresetManager.getInstance().select(added);
            }
            rebuildCards();
        });
    }

    private void saveCurrentPreset() {
        TexturePackManager packs = TexturePackManager.getInstance();
        int toolsIndex = packs.getSelectedIndex(TextureSlots.TOOLS_ALL);
        int utilitiesIndex = packs.getSelectedIndex(TextureSlots.UTILITIES_ALL);
        int armorIndex = packs.getSelectedIndex(TextureSlots.ARMOR_ALL);
        TexturePresetManager.getInstance().saveCombo(toolsIndex, utilitiesIndex, armorIndex);
        rebuildCards();
    }

    /**
     * The Profiles tab doesn't list {@link Module}s either - it shows one row
     * per {@link ProfileCategory} (Visual, Performance), each with its built-in
     * profile, its saved Custom profile if one exists, and - if the live settings
     * no longer match any profile in that row - an extra unsaved "Custom" preview
     * card with a Save button. Clicking a real card applies it immediately, no
     * confirmation (nothing it does is destructive - it only flips module
     * toggles/settings that are freely changeable again after).
     */
    private void rebuildProfileCards(int contentTop, int cardW) {
        int rowCardW = Math.min(cardW, 150);
        int y = contentTop;
        y = addProfileRow(ProfileCategory.VISUAL, y, rowCardW);
        y += 10;
        addProfileRow(ProfileCategory.PERFORMANCE, y, rowCardW);
    }

    /**
     * Lays out one Profiles-tab row, wrapping to additional lines once more
     * cards exist than fit the available width - previously this just kept
     * advancing {@code x} with no bounds check, so the Performance row
     * (now up to 5 cards: Balanced/Lite/Aggressive/Custom/draft-Custom)
     * could run off the right edge of the screen at normal GUI scales.
     * Returns the real bottom y of everything just drawn, so the caller
     * can stack the next row underneath regardless of how many lines this
     * one wrapped to.
     */
    private int addProfileRow(ProfileCategory category, int labelY, int cardW) {
        profileRowLabels.add(new ProfileRowLabel(category.getRowLabel(), SIDE_PAD, labelY));
        int rowTop = labelY + 12;

        ProfileManager profileManager = ProfileManager.getInstance();
        List<Profile> profiles = profileManager.getProfiles(category);
        Profile active = profileManager.getActiveProfile(category);

        int totalW = width - SIDE_PAD * 2;
        int maxPerRow = Math.max(1, (totalW + CARD_GAP) / (cardW + CARD_GAP));

        int col = 0;
        int row = 0;
        for (Profile profile : profiles) {
            boolean selected = profile == active;
            Runnable onRename = profile instanceof Renameable renameable
                    ? () -> openProfileRename(renameable)
                    : null;
            int cx = SIDE_PAD + col * (cardW + CARD_GAP);
            int cy = rowTop + row * (ProfileCardWidget.HEIGHT + CARD_GAP);
            ProfileCardWidget card = new ProfileCardWidget(cx, cy, cardW, profile, textRenderer, selected, onRename, this::rebuildCards);
            cards.add(card);
            addDrawableChild(card);
            col++;
            if (col >= maxPerRow) {
                col = 0;
                row++;
            }
        }

        if (active == null) {
            int cx = SIDE_PAD + col * (cardW + CARD_GAP);
            int cy = rowTop + row * (ProfileCardWidget.HEIGHT + CARD_GAP);
            DraftProfileCardWidget draft = new DraftProfileCardWidget(cx, cy, cardW, textRenderer, () -> {
                ProfileManager.getInstance().saveCustom(category);
                rebuildCards();
            });
            cards.add(draft);
            addDrawableChild(draft);
            col++;
            if (col >= maxPerRow) {
                col = 0;
                row++;
            }
        }

        int lineCount = row + (col > 0 ? 1 : 0);
        if (lineCount == 0) {
            lineCount = 1;
        }
        return rowTop + lineCount * ProfileCardWidget.HEIGHT + (lineCount - 1) * CARD_GAP;
    }

    private void openProfileRename(Renameable target) {
        assert client != null;
        client.setScreen(new ProfileRenameScreen(this, target, this::rebuildCards));
    }

    private boolean isModuleGridActive() {
        return (activeCategory == ModuleCategory.QUALITY_OF_LIFE || activeCategory == ModuleCategory.ADVANCED)
                && globalSearchField != null && globalSearchField.getText().isBlank();
    }

    private static final int TEXTURES_SCROLL_STEP = 18;

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isModuleGridActive() && moduleMaxScrollRow > 0
                && mouseY >= moduleContentViewportTop && mouseY < footerButtonY) {
            int newScrollRow = (int) Math.max(0, Math.min(moduleMaxScrollRow, moduleScrollRow - Math.signum(verticalAmount)));
            if (newScrollRow != moduleScrollRow) {
                int rowStep = ModuleCardWidget.HEIGHT + CARD_GAP;
                scrollAnimOffsetPx += (newScrollRow - moduleScrollRow) * rowStep;
                moduleScrollRow = newScrollRow;
                rebuildCards();
            }
            return true;
        }
        if (activeCategory == ModuleCategory.TEXTURES && texturesMaxScroll > 0
                && mouseY >= texturesViewportTop && mouseY < footerButtonY) {
            int newOffset = (int) Math.max(0, Math.min(texturesMaxScroll,
                    texturesScrollOffset - verticalAmount * TEXTURES_SCROLL_STEP));
            if (newOffset != texturesScrollOffset) {
                texturesScrollOffset = newOffset;
                rebuildCards();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Semi-transparent dark overlay behind the GUI. (Screen#applyBlur was tried here,
        // but Minecraft only allows one blur pass per frame and something else in the
        // frame already claims it, so a flat dim is used instead.)
        context.fill(0, 0, width, height, ColorPalette.OVERLAY_DARK);

        // Header bar
        SolsticeTheme.fillRect(context, 0, 0, width, HEADER_H, ColorPalette.BG_DEEP);
        context.drawText(textRenderer,
                "☀ SOLSTICE  " + SolsticeMod.VERSION,
                SIDE_PAD, (HEADER_H - 8) / 2,
                ColorPalette.ACCENT_NEUTRAL, false);

        // Divider below tabs
        SolsticeTheme.drawDivider(context, 0, HEADER_H + TAB_H, width);

        // Delimiter line above the footer buttons - card content never draws past this
        // (the module grid clips to whole rows above it instead of overflowing).
        SolsticeTheme.drawDivider(context, 0, footerButtonY, width);

        // Darken the footer strip below the divider a bit more than the rest of
        // the screen's own dim, so the Done/Edit HUD Layout buttons read as a
        // distinct bottom bar rather than floating over the same background.
        context.fill(0, footerButtonY, width, height, 0xCC000000);

        if (isModuleGridActive() && moduleMaxScrollRow > 0) {
            int rowStep = ModuleCardWidget.HEIGHT + CARD_GAP;
            int trackX = width - SIDE_PAD + 3;
            int trackH = moduleVisibleRows * rowStep - CARD_GAP;
            int totalRows = moduleMaxScrollRow + moduleVisibleRows;
            SolsticeTheme.fillRect(context, trackX, moduleContentViewportTop, 3, trackH, ColorPalette.BORDER_DEFAULT);
            int thumbH = Math.max(10, trackH * moduleVisibleRows / totalRows);
            int thumbY = moduleContentViewportTop + (trackH - thumbH) * moduleScrollRow / moduleMaxScrollRow;
            SolsticeTheme.fillRect(context, trackX, thumbY, 3, thumbH, ColorPalette.ACCENT_DIM);
        }

        if (activeCategory == ModuleCategory.TEXTURES && texturesMaxScroll > 0) {
            int trackX = width - SIDE_PAD + 3;
            int trackH = footerButtonY - texturesViewportTop;
            SolsticeTheme.fillRect(context, trackX, texturesViewportTop, 3, trackH, ColorPalette.BORDER_DEFAULT);
            int totalContentH = trackH + texturesMaxScroll;
            int thumbH = Math.max(10, trackH * trackH / totalContentH);
            int thumbY = texturesViewportTop + (trackH - thumbH) * texturesScrollOffset / texturesMaxScroll;
            SolsticeTheme.fillRect(context, trackX, thumbY, 3, thumbH, ColorPalette.ACCENT_DIM);
        }

        for (ProfileRowLabel label : profileRowLabels) {
            context.drawText(textRenderer, label.text(), label.x(), label.y(), ColorPalette.TEXT_SECONDARY, false);
        }

        // No footer bar - removed (its RAM/Modules status text shared the same x
        // position as the footer buttons and bled through their translucent
        // backgrounds, reading as visual overlap). Buttons now just sit on the
        // screen's own full-bleed OVERLAY_DARK dim filled above.

        // "More below" hint - a small centered chevron just above the footer line
        // when scrolling further would reveal more modules. Plain 2D draw, no
        // scissor/matrix state involved (a scissor-cropped partial row was tried
        // here before and reverted - it broke rendering of the category tabs).
        if (isModuleGridActive() && moduleScrollRow < moduleMaxScrollRow) {
            drawMoreBelowHint(context);
        }

        applyScrollAnimation();
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawMoreBelowHint(DrawContext context) {
        String hint = "v";
        int hintW = textRenderer.getWidth(hint);
        context.drawText(textRenderer, hint, (width - hintW) / 2, footerButtonY - 11, ColorPalette.TEXT_SECONDARY, false);
    }

    /**
     * Decays {@link #scrollAnimOffsetPx} toward 0 by real elapsed time (not tick delta -
     * this runs every frame, not every tick) and writes each module card's Y as its
     * settled {@link #cardNaturalY} plus whatever offset remains, so a scroll tick's
     * jump plays out as a short slide instead of an instant snap.
     */
    private void applyScrollAnimation() {
        long now = System.nanoTime();
        float dt = lastScrollAnimNanos == 0 ? 0f : Math.min(0.1f, (now - lastScrollAnimNanos) / 1_000_000_000f);
        lastScrollAnimNanos = now;

        if (scrollAnimOffsetPx != 0f) {
            scrollAnimOffsetPx *= (float) Math.exp(-SCROLL_ANIM_DECAY_PER_SECOND * dt);
            if (Math.abs(scrollAnimOffsetPx) < 0.5f) {
                scrollAnimOffsetPx = 0f;
            }
        }

        int offset = Math.round(scrollAnimOffsetPx);
        for (int i = 0; i < cards.size() && i < cardNaturalY.size(); i++) {
            cards.get(i).setY(cardNaturalY.get(i) + offset);
        }
    }

    @Override
    public void close() {
        assert client != null;
        client.setScreen(parent);
    }

    // No shouldPause() override - Screen's own default (true) is exactly what's wanted:
    // pause a singleplayer world while this is open, same as vanilla's own menus, instead
    // of letting it keep running in the background behind the GUI.
}
