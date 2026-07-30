package com.example.solstice.ui.widget;

import com.example.solstice.ui.SolsticeSounds;
import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * A real saturation/value square + hue strip color picker - replaces the old
 * "guess the RGB numbers on three separate sliders" approach everywhere a
 * module needed a color (combat crosshair tint, HUD widget text/background).
 *
 * <p>Layout: label on top, then a square SV area (drag to set saturation/value)
 * with a thin vertical hue strip to its right (drag to set hue) and a small
 * swatch showing the resulting color. Works on packed {@code 0xRRGGBB} ints -
 * callers that need alpha keep it separately (the picker only ever touches
 * the low 24 bits).</p>
 */
public class SolsticeColorPickerWidget extends SolsticeClickableWidget {

    private static final int LABEL_H = 10;
    private static final int HUE_STRIP_W = 12;
    private static final int HUE_GAP = 4;
    private static final int SWATCH_W = 20;
    private static final int SWATCH_GAP = 4;

    private final TextRenderer textRenderer;
    private final String label;
    private final IntSupplier getter;
    private final IntConsumer setter;

    private boolean draggingSv;
    private boolean draggingHue;

    public SolsticeColorPickerWidget(int x, int y, int width, int height, TextRenderer textRenderer,
                                      String label, IntSupplier getter, IntConsumer setter) {
        super(x, y, width, height, Text.of(label));
        this.textRenderer = textRenderer;
        this.label = label;
        this.getter = getter;
        this.setter = setter;
    }

    private int svX() { return getX(); }
    private int svY() { return getY() + LABEL_H; }
    private int svSize() { return getHeight() - LABEL_H; }

    private int hueX() { return svX() + svSize() + HUE_GAP; }
    private int hueY() { return svY(); }
    private int hueH() { return svSize(); }

    private int swatchX() { return hueX() + HUE_STRIP_W + SWATCH_GAP; }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawText(textRenderer, label, getX(), getY(), ColorPalette.TEXT_PRIMARY, false);

        int rgb = getter.getAsInt();
        float[] hsv = java.awt.Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);

        drawSvSquare(context, hsv[0]);
        drawSvCursor(context, hsv[1], hsv[2]);

        drawHueStrip(context);
        drawHueCursor(context, hsv[0]);

        int swatchSize = svSize();
        SolsticeTheme.fillRect(context, swatchX(), svY(), SWATCH_W, swatchSize, 0xFF000000 | rgb);
        SolsticeTheme.drawBorder(context, swatchX(), svY(), SWATCH_W, swatchSize, ColorPalette.BORDER_DEFAULT);
        String hex = String.format("#%06X", rgb & 0xFFFFFF);
        context.drawText(textRenderer, hex, swatchX(), svY() + swatchSize + 2, ColorPalette.TEXT_SECONDARY, false);
    }

    /** Draws the saturation(x)/value(y) square for a fixed hue - white at (0,0), full color at (1,0), black at y=1. */
    private void drawSvSquare(DrawContext context, float hue) {
        int size = svSize();
        int x0 = svX(), y0 = svY();
        int fullColor = 0xFF000000 | java.awt.Color.HSBtoRGB(hue, 1f, 1f) & 0xFFFFFF;
        // Cheap approximation: fill white->hue horizontally, then overlay a black vertical gradient in steps.
        // A true per-pixel gradient would need per-pixel draws; stepped bands are smooth enough at this size.
        int steps = size;
        for (int i = 0; i < steps; i++) {
            float s = i / (float) steps;
            int stepColor = lerpArgb(0xFFFFFFFF, fullColor, s);
            SolsticeTheme.fillRect(context, x0 + i, y0, 1, size, stepColor);
        }
        for (int j = 0; j < steps; j++) {
            float v = j / (float) steps;
            int alpha = (int) (255 * (1f - v));
            SolsticeTheme.fillRect(context, x0, y0 + j, size, 1, (alpha << 24));
        }
        SolsticeTheme.drawBorder(context, x0, y0, size, size, ColorPalette.BORDER_DEFAULT);
    }

    private void drawSvCursor(DrawContext context, float saturation, float value) {
        int size = svSize();
        int cx = svX() + Math.round(saturation * size);
        int cy = svY() + Math.round((1f - value) * size);
        SolsticeTheme.fillRect(context, cx - 2, cy - 2, 4, 4, 0xFFFFFFFF);
        SolsticeTheme.drawBorder(context, cx - 3, cy - 3, 6, 6, 0xFF000000);
    }

    private void drawHueStrip(DrawContext context) {
        int h = hueH();
        for (int i = 0; i < h; i++) {
            float hue = i / (float) h;
            int color = 0xFF000000 | (java.awt.Color.HSBtoRGB(hue, 1f, 1f) & 0xFFFFFF);
            SolsticeTheme.fillRect(context, hueX(), hueY() + i, HUE_STRIP_W, 1, color);
        }
        SolsticeTheme.drawBorder(context, hueX(), hueY(), HUE_STRIP_W, h, ColorPalette.BORDER_DEFAULT);
    }

    private void drawHueCursor(DrawContext context, float hue) {
        int y = hueY() + Math.round(hue * hueH());
        SolsticeTheme.fillRect(context, hueX() - 2, y - 1, HUE_STRIP_W + 4, 2, 0xFFFFFFFF);
        SolsticeTheme.drawBorder(context, hueX() - 2, y - 2, HUE_STRIP_W + 4, 4, 0xFF000000);
    }

    private static int lerpArgb(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = lerpChannel((from >> 24) & 0xFF, (to >> 24) & 0xFF, t);
        int r = lerpChannel((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
        int g = lerpChannel((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
        int b = lerpChannel(from & 0xFF, to & 0xFF, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerpChannel(int from, int to, float t) {
        return Math.round(from + (to - from) * t);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x();
        int my = (int) click.y();
        if (inRect(mx, my, svX(), svY(), svSize(), svSize())) {
            SolsticeSounds.playClick();
            draggingSv = true;
            applySv(mx, my);
            return true;
        }
        if (inRect(mx, my, hueX(), hueY(), HUE_STRIP_W, hueH())) {
            SolsticeSounds.playClick();
            draggingHue = true;
            applyHue(my);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        int mx = (int) click.x();
        int my = (int) click.y();
        if (draggingSv) {
            applySv(mx, my);
            return true;
        }
        if (draggingHue) {
            applyHue(my);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        boolean was = draggingSv || draggingHue;
        draggingSv = false;
        draggingHue = false;
        return was;
    }

    private void applySv(int mx, int my) {
        int size = svSize();
        float s = clamp01((mx - svX()) / (float) size);
        float v = clamp01(1f - (my - svY()) / (float) size);
        float[] hsv = currentHsv();
        setter.accept(java.awt.Color.HSBtoRGB(hsv[0], s, v) & 0xFFFFFF);
    }

    private void applyHue(int my) {
        float hue = clamp01((my - hueY()) / (float) hueH());
        float[] hsv = currentHsv();
        setter.accept(java.awt.Color.HSBtoRGB(hue, hsv[1], hsv[2]) & 0xFFFFFF);
    }

    private float[] currentHsv() {
        int rgb = getter.getAsInt();
        return java.awt.Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private static boolean inRect(int px, int py, int x, int y, int w, int h) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
