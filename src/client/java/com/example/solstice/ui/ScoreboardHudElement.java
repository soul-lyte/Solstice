package com.example.solstice.ui;

import com.example.solstice.core.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Editor-only stand-in for the scoreboard sidebar - the real position/scale
 * live vanilla actually draws with comes from {@code
 * mixin.hud.InGameHudScoreboardMixin} reading this element's id ({@code
 * "scoreboard"}) directly out of {@link com.example.solstice.core.hud.HudLayoutManager}
 * during {@code InGameHud}'s own render pass, not from this class's own
 * {@link #render} - that Mixin redirects vanilla's real draw calls in
 * place, it doesn't call back into this element at all. This exists so the
 * scoreboard has a draggable/resizable box in {@code HudEditorScreen} like
 * every other {@link HudElement}; its own {@code render} draws a realistic
 * example (title + a few name/score rows, styled like vanilla's real
 * sidebar) rather than a plain placeholder, so positioning it doesn't
 * require actually being on a server with a live scoreboard objective.
 */
public final class ScoreboardHudElement implements HudElement {

    private static final ScoreboardHudElement INSTANCE = new ScoreboardHudElement();

    private static final String TITLE = "Example Scoreboard";
    private static final String[] EXAMPLE_NAMES  = {"Steve", "Alex", "Notch", "Herobrine", "Player5"};
    private static final int[]    EXAMPLE_SCORES = {42, 37, 25, 13, 5};

    private static final int HEADER_H = 10;
    private static final int LINE_H = 9;
    private static final int WIDTH = 120;

    private ScoreboardHudElement() {}

    public static ScoreboardHudElement getInstance() { return INSTANCE; }

    @Override public String getId()          { return "scoreboard"; }
    @Override public String getDisplayName() { return "Scoreboard"; }
    @Override public int getWidth()          { return WIDTH; }
    @Override public int getHeight()         { return HEADER_H + EXAMPLE_NAMES.length * LINE_H + 4; }

    @Override
    public int getDefaultX(int screenWidth, int screenHeight) {
        // Vanilla anchors the real sidebar to the right edge of the screen.
        return screenWidth - WIDTH - 3;
    }

    @Override
    public int getDefaultY(int screenWidth, int screenHeight) {
        return screenHeight / 4;
    }

    @Override
    public void render(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        int w = getWidth();
        int h = getHeight();

        context.fill(x, y, x + w, y + HEADER_H, 0xA0000000);
        context.fill(x, y + HEADER_H, x + w, y + h, 0x60000000);

        int titleX = x + (w - client.textRenderer.getWidth(TITLE)) / 2;
        context.drawText(client.textRenderer, TITLE, titleX, y + 1, 0xFFFFFF, false);

        int rowY = y + HEADER_H + 2;
        for (int i = 0; i < EXAMPLE_NAMES.length; i++) {
            String name = EXAMPLE_NAMES[i];
            String score = String.valueOf(EXAMPLE_SCORES[i]);
            context.drawText(client.textRenderer, name, x + 2, rowY, 0xFFFFFF, false);
            int scoreX = x + w - 2 - client.textRenderer.getWidth(score);
            context.drawText(client.textRenderer, score, scoreX, rowY, 0xFF5555, false);
            rowY += LINE_H;
        }
    }
}
