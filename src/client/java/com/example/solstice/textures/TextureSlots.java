package com.example.solstice.textures;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Static registry of every swappable texture slot, grouped by Textures-tab category.
 * Built from what's actually available under {@code References/Textures/} (see
 * {@code tools/copy-texture-packs.ps1} for how the bundled packs were produced).
 *
 * <p>Preview asset paths point at one representative file (relative to
 * {@code assets/minecraft/}) that both the vanilla asset and every bundled
 * pack for that slot are known to contain - {@link TexturePreviewLoader}
 * loads whichever option is currently being browsed directly, without
 * touching the real active resource pack.</p>
 */
public final class TextureSlots {

    private TextureSlots() {}

    /**
     * The single Tools slot - deliberately just one unified choice, not a
     * per-tool-type breakdown (there used to also be a separate Sword-only
     * slot; removed per direct request in favor of one "All Tools" control).
     * "Mini Sword" only reskins the sword, unlike the other three options
     * which retexture the whole tool/weapon set - kept as an option here
     * rather than dropped, since it was already a real, working feature.
     */
    public static final TextureSlot TOOLS_ALL = new TextureSlot("tools_all", "All Tools",
            "Swaps every tool/weapon texture at once, sourced from a real resource pack.",
            List.of(
                    new TextureOption("vanilla", "Vanilla", null),
                    new TextureOption("mini", "Mini Sword", "tools_mini_sword"),
                    new TextureOption("tiny_tools", "Tiny Tools", "tools_tiny_tools"),
                    new TextureOption("vanillaplus", "Vanilla+", "tools_vanillaplus"),
                    new TextureOption("tournament", "Tournament", "tools_tournament"),
                    new TextureOption("kb1_sword", "KB1 Sword", "tools_kb1_sword")
            ),
            "textures/item/diamond_sword.png");

    /**
     * Advanced-row addition: every other item texture a pack provides
     * (golden apples, misc food, ender pearls, etc.) plus the cobweb block
     * texture where present, sourced from a real resource pack.
     */
    public static final TextureSlot UTILITIES_ALL = new TextureSlot("utilities_all", "Utility Items",
            "Swaps misc item textures (food, cobwebs, etc.) at once, sourced from a real resource pack.",
            List.of(
                    new TextureOption("vanilla", "Vanilla", null),
                    new TextureOption("vanillaplus", "Vanilla+", "utilities_vanillaplus"),
                    new TextureOption("tournament", "Tournament", "utilities_tournament")
            ),
            "textures/item/golden_apple.png");

    /** Advanced-row addition: worn armor textures, sourced from a real resource pack. New category - vanilla had none here before. */
    public static final TextureSlot ARMOR_ALL = new TextureSlot("armor_all", "Armor",
            "Swaps worn armor textures, sourced from a real resource pack.",
            List.of(
                    new TextureOption("vanilla", "Vanilla", null),
                    new TextureOption("vanillaplus", "Vanilla+", "armor_vanillaplus"),
                    new TextureOption("tournament", "Tournament", "armor_tournament")
            ),
            // The worn-armor equipment texture is a full UV skin sheet (all
            // pieces laid out in one image), not a clean icon - previewing it
            // directly looked unrecognizable even scaled correctly. Both
            // packs also ship a real chestplate inventory icon, which is a
            // normal single-item texture, so that's what gets previewed -
            // copied alongside the equipment texture into each derived pack
            // for exactly this reason (also a real correctness bonus: the
            // inventory icon is now retextured too, not just the worn look).
            "textures/item/diamond_chestplate.png");

    public static final TextureSlot GUI_SKIN = new TextureSlot("gui_skin", "Inventory GUI",
            "Reskins inventory/container screens - crafting table, furnace, chests, etc.",
            List.of(
                    new TextureOption("vanilla", "Vanilla", null),
                    new TextureOption("dark", "Dark", "gui_dark"),
                    new TextureOption("transparent", "Transparent", "gui_transparent")
            ),
            "textures/gui/sprites/container/slot.png");

    public static final TextureSlot FONT = new TextureSlot("font", "Font",
            "Replaces the game's default font glyphs.",
            List.of(
                    new TextureOption("vanilla", "Vanilla", null),
                    new TextureOption("capitalized", "Capitalized", "font_capitalized"),
                    new TextureOption("common32", "Common 32px", "font_common32"),
                    new TextureOption("smooth48", "Smooth 48px", "font_smooth48")
            ),
            (context, x, y, size) -> {
                Text sample = Text.literal("Aa");
                var tr = MinecraftClient.getInstance().textRenderer;
                int tw = tr.getWidth(sample);
                context.drawText(tr, sample, x + Math.max(0, (size - tw) / 2), y + (size - 8) / 2, 0xFFFFFFFF, true);
            });

    public static final TextureSlot UTIL_COBWEB = new TextureSlot("util_cobweb", "Cobweb",
            "Reskins the cobweb block texture.",
            List.of(
                    new TextureOption("vanilla", "Vanilla", null),
                    new TextureOption("cyan", "Cyan", "util_cobweb_cyan"),
                    new TextureOption("outline", "Outline", "util_cobweb_outline")
            ),
            "textures/block/cobweb.png");

    public static final TextureSlot UTIL_BOW_CROSSBOW = new TextureSlot("util_bow_crossbow", "Bow/Crossbow",
            "Reskins the bow and crossbow pull-frame textures to show draw progress more clearly.",
            List.of(
                    new TextureOption("vanilla", "Vanilla", null),
                    new TextureOption("pull_indicator", "Pull Indicator", "util_bow_crossbow")
            ),
            "textures/item/bow.png", 1);

    public static final List<TextureSlot> TOOLS = List.of(TOOLS_ALL);
    public static final List<TextureSlot> GUI = List.of(GUI_SKIN);
    public static final List<TextureSlot> UTILITIES = List.of(UTIL_COBWEB, UTIL_BOW_CROSSBOW, UTILITIES_ALL);
    public static final List<TextureSlot> FONTS = List.of(FONT);
    public static final List<TextureSlot> ARMOR = List.of(ARMOR_ALL);

    public static final List<TextureSlot> ALL = List.of(TOOLS_ALL, GUI_SKIN, FONT,
            UTIL_COBWEB, UTIL_BOW_CROSSBOW, UTILITIES_ALL, ARMOR_ALL);
}
