package com.example.solstice.ui;

import com.example.solstice.core.hud.HudElement;
import com.example.solstice.core.hud.HudLayoutManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * FPS / 1% low HUD widget - independently positionable, hideable, and
 * background-customizable from the HUD editor (see {@code HudEditorScreen}).
 *
 * <p>Split out of the old combined {@code HudOverlay} so FPS and RAM can be
 * toggled and styled separately.</p>
 */
public final class FpsWidget implements HudElement {

    private static final FpsWidget INSTANCE = new FpsWidget();

    private static final int LINE_H = 10;
    private static final int PAD_X = 4;
    private static final int PAD_Y = 2;

    // Rolling 1% low tracker - stores last 100 frame times
    private final long[] frameTimes = new long[100];
    private int frameIndex = 0;
    private long lastFrameNs = System.nanoTime();

    private FpsWidget() {}

    public static FpsWidget getInstance() { return INSTANCE; }

    @Override public String getId()          { return "fps"; }
    @Override public String getDisplayName() { return "FPS"; }
    @Override public int getWidth()          { return 110; }
    @Override public int getHeight()         { return LINE_H + PAD_Y * 2; }

    /** Called from {@link com.example.solstice.SolsticeClient} via {@code HudRenderCallback.EVENT}. */
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Keep the frame-time clock fresh even while hidden, so the instant the
        // widget becomes visible again we don't compute one giant bogus "frame
        // time" spanning the entire hidden period.
        long nowNs = System.nanoTime();
        boolean visible = HudLayoutManager.getInstance().isMasterVisible()
                && !client.debugHudEntryList.isF3Enabled()
                && client.currentScreen == null
                && HudLayoutManager.getInstance().isVisible(getId(), true);
        if (!visible) {
            lastFrameNs = nowNs;
            return;
        }

        frameTimes[frameIndex % frameTimes.length] = nowNs - lastFrameNs;
        lastFrameNs = nowNs;
        frameIndex++;

        int defaultX = context.getScaledWindowWidth() - getWidth() - 2;
        int x = HudLayoutManager.getInstance().getX(getId(), defaultX);
        int y = HudLayoutManager.getInstance().getY(getId(), 2);
        render(context, x, y);
    }

    /**
     * Draws the widget at the given position - used both by real gameplay and the HUD
     * editor's live preview. Box size is a fixed, user-resizable value from
     * {@link HudLayoutManager} rather than measured from the current text, so it
     * doesn't grow/shrink as the displayed numbers change digit count.
     */
    @Override
    public void render(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();

        String line = "FPS: " + client.getCurrentFps() + "  1% Low: " + compute1PercentLow();
        int boxW = HudLayoutManager.getInstance().getWidth(getId(), getWidth());
        int boxH = HudLayoutManager.getInstance().getHeight(getId(), getHeight());
        int background = HudLayoutManager.getInstance().getBackground(getId(), 0x880D1117);
        int textColor = HudLayoutManager.getInstance().getTextColor(getId(), com.example.solstice.ui.theme.ColorPalette.TEXT_PRIMARY);

        context.fill(x, y, x + boxW, y + boxH, background);
        HudLayoutManager.withContentScale(context, x, y, getWidth(), getHeight(), boxW, boxH, () ->
                context.drawText(client.textRenderer, line, x + PAD_X, y + PAD_Y, textColor, false));
    }

    /** Computes the approximate 1% low FPS from the rolling frame-time buffer. */
    private int compute1PercentLow() {
        int count = Math.min(frameIndex, frameTimes.length);
        if (count == 0) return 0;

        long[] copy = new long[count];
        for (int i = 0; i < count; i++) {
            copy[i] = frameTimes[i];
        }
        java.util.Arrays.sort(copy);

        int idx = (int) Math.ceil(count * 0.99) - 1;
        long worstNs = copy[Math.min(idx, count - 1)];
        return worstNs > 0 ? (int) (1_000_000_000L / worstNs) : 0;
    }
}
