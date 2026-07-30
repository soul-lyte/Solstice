package com.example.solstice.ui.theme;

import net.minecraft.client.gui.DrawContext;

/**
 * Shared rendering helpers for the Solstice UI.
 * All draw calls go through here so the look can be updated in one place.
 *
 * <p>Deliberately square, not rounded: without a genuine sub-pixel-smooth
 * rounding primitive (Minecraft's GUI renderer in this version has none -
 * only axis-aligned {@code fill()}), a "rounded" rect built from stepped
 * pixel rows reads as a blocky fake circle rather than smooth. Flat/square
 * is the explicitly preferred fallback over that.</p>
 */
public final class SolsticeTheme {

    private SolsticeTheme() {}

    /** Fills a rectangle with a flat color. */
    public static void fillRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + h, color);
    }

    /** Draws a 1-px border around a rectangle. */
    public static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,         y,         x + w,     y + 1,     color); // top
        ctx.fill(x,         y + h - 1, x + w,     y + h,     color); // bottom
        ctx.fill(x,         y + 1,     x + 1,     y + h - 1, color); // left
        ctx.fill(x + w - 1, y + 1,     x + w,     y + h - 1, color); // right
    }

    /** Draws a panel (background + border). */
    public static void drawPanel(DrawContext ctx, int x, int y, int w, int h) {
        fillRect(ctx, x, y, w, h, ColorPalette.BG_PANEL);
        drawBorder(ctx, x, y, w, h, ColorPalette.BORDER_DEFAULT);
    }

    /** Draws a card (slightly lighter than panel). */
    public static void drawCard(DrawContext ctx, int x, int y, int w, int h, boolean hovered) {
        int bg = hovered ? ColorPalette.BG_HOVER : ColorPalette.BG_CARD;
        fillRect(ctx, x, y, w, h, bg);
        drawBorder(ctx, x, y, w, h, ColorPalette.BORDER_DEFAULT);
    }

    /**
     * Draws a small square toggle switch at (x, y).
     * Width = 26 px, height = 14 px.
     */
    public static void drawToggle(DrawContext ctx, int x, int y, boolean on) {
        int track = on ? ColorPalette.TOGGLE_ON : ColorPalette.TOGGLE_OFF;
        fillRect(ctx, x, y, 26, 14, track);
        int kx = on ? x + 14 : x + 2;
        fillRect(ctx, kx, y + 2, 10, 10, ColorPalette.TOGGLE_KNOB);
    }

    /**
     * Draws a static, non-interactive "always on" badge in place of a toggle.
     * Same footprint as {@link #drawToggle} (26x14) so it drops into the same layout slot.
     */
    public static void drawAlwaysOnBadge(DrawContext ctx, int x, int y,
                                          net.minecraft.client.font.TextRenderer tr) {
        fillRect(ctx, x, y, 26, 14, ColorPalette.ACCENT_DIM);
        String label = "ON";
        int tw = tr.getWidth(label);
        ctx.drawText(tr, label, x + (26 - tw) / 2, y + 3, ColorPalette.TEXT_PRIMARY, false);
    }

    /**
     * Draws a horizontal divider line.
     */
    public static void drawDivider(DrawContext ctx, int x, int y, int w) {
        ctx.fill(x, y, x + w, y + 1, ColorPalette.BORDER_DEFAULT);
    }

    /**
     * Draws a category badge (colored tag).
     */
    public static void drawCategoryBadge(DrawContext ctx, int x, int y,
                                          net.minecraft.client.font.TextRenderer tr,
                                          com.example.solstice.core.module.ModuleCategory cat) {
        String label = cat.getDisplayName().toUpperCase();
        int tw = tr.getWidth(label);
        int badgeW = tw + 8;
        fillRect(ctx, x, y, badgeW, 11, ColorPalette.categoryColor(cat));
        ctx.drawText(tr, label, x + 4, y + 2, ColorPalette.TEXT_PRIMARY, false);
    }
}
