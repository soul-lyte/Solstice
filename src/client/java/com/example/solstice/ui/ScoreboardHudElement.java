package com.example.solstice.ui;

import com.example.solstice.core.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.Team;

/**
 * Editor-only stand-in for the scoreboard sidebar - the real position/scale
 * live vanilla actually draws with comes from {@code
 * mixin.hud.InGameHudScoreboardMixin} reading this element's id ({@code
 * "scoreboard"}) directly out of {@link com.example.solstice.core.hud.HudLayoutManager}
 * during {@code InGameHud}'s own render pass, not from this class's own
 * {@link #render} - that Mixin redirects vanilla's real draw calls in
 * place, it doesn't call back into this element at all.
 *
 * <p>{@link #hasLiveScoreboard()} independently re-resolves the real sidebar
 * objective every call (same algorithm vanilla's own {@code
 * InGameHud.renderScoreboardSidebar(DrawContext, RenderTickCounter)} uses
 * to pick between a team-colored slot and the plain sidebar slot, confirmed
 * via decompile) rather than trusting a cache that only updates when the
 * Mixin's own render call actually fires - that render call is skipped
 * entirely both when there's genuinely no objective (solo world, or a
 * server that just doesn't use one) AND, previously, whenever the element
 * was toggled off in the editor, which meant re-opening the editor to turn
 * a hidden scoreboard back on showed stale/default bounds instead of where
 * it would really be. The Mixin no longer skips its own bounds computation
 * while hidden (see its own Javadoc), so the cache this class reads is
 * always fresh for the current frame whenever a real objective exists,
 * independent of the visibility toggle.</p>
 *
 * <p>When a real objective exists, {@link #render} deliberately draws
 * nothing - the real scoreboard is already rendering behind {@code
 * HudEditorScreen} every frame (vanilla's own HUD render pass isn't gated
 * on whether a Screen is open, confirmed via decompile), at the exact
 * position/size this element reports through {@link #getDefaultX}/{@link
 * #getDefaultY}/{@link #getWidth()}/{@link #getHeight()} - drawing a second
 * copy here would just duplicate it. The placeholder (title + a few
 * example name/score rows, styled like vanilla's real sidebar) only draws
 * when there's genuinely nothing real to show instead, so positioning it
 * still works before ever joining a world with an active objective.</p>
 */
public final class ScoreboardHudElement implements HudElement {

    private static final ScoreboardHudElement INSTANCE = new ScoreboardHudElement();

    private static final String TITLE = "Example Scoreboard";
    private static final String[] EXAMPLE_NAMES  = {"Steve", "Alex", "Notch", "Herobrine", "Player5"};
    private static final int[]    EXAMPLE_SCORES = {42, 37, 25, 13, 5};

    private static final int HEADER_H = 10;
    private static final int LINE_H = 9;
    private static final int PLACEHOLDER_WIDTH = 120;

    private static int realNaturalX, realNaturalY, realNaturalW, realNaturalH;

    private ScoreboardHudElement() {}

    public static ScoreboardHudElement getInstance() { return INSTANCE; }

    /** Called by {@code InGameHudScoreboardMixin} every time it runs, whether or not it's actually drawing. */
    public static void recordRealNaturalBounds(int naturalX, int naturalY, int naturalW, int naturalH) {
        realNaturalX = naturalX;
        realNaturalY = naturalY;
        realNaturalW = naturalW;
        realNaturalH = naturalH;
    }

    /**
     * Whether a real sidebar-displayed scoreboard objective exists right
     * now - independently re-resolved every call (not cached), matching
     * vanilla's own team-slot-then-plain-sidebar-slot resolution order.
     */
    public static boolean hasLiveScoreboard() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return false;

        Scoreboard scoreboard = client.world.getScoreboard();
        Team team = scoreboard.getScoreHolderTeam(client.player.getNameForScoreboard());
        if (team != null) {
            ScoreboardDisplaySlot teamSlot = ScoreboardDisplaySlot.fromFormatting(team.getColor());
            if (teamSlot != null && scoreboard.getObjectiveForSlot(teamSlot) != null) {
                return true;
            }
        }
        return scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR) != null;
    }

    @Override public String getId()          { return "scoreboard"; }
    @Override public String getDisplayName() { return "Scoreboard"; }
    @Override public boolean hasLiveNaturalAnchor() { return true; }

    @Override
    public int getWidth() {
        return hasLiveScoreboard() ? realNaturalW : PLACEHOLDER_WIDTH;
    }

    @Override
    public int getHeight() {
        return hasLiveScoreboard() ? realNaturalH : HEADER_H + EXAMPLE_NAMES.length * LINE_H + 4;
    }

    @Override
    public int getDefaultX(int screenWidth, int screenHeight) {
        if (hasLiveScoreboard()) return realNaturalX;
        // Vanilla anchors the real sidebar to the right edge of the screen.
        return screenWidth - PLACEHOLDER_WIDTH - 3;
    }

    @Override
    public int getDefaultY(int screenWidth, int screenHeight) {
        if (hasLiveScoreboard()) return realNaturalY;
        // Vanilla anchors the panel's bottom edge to screenHeight/2 + rowCount*3, growing upward
        // by rowCount*9 for the entry rows plus a 10px header - confirmed via decompiling
        // InGameHud.renderScoreboardSidebar - so the real top-of-header y is
        // screenHeight/2 - rowCount*6. Matches the example content's own row count so this
        // preview box lines up with what render() actually draws.
        return screenHeight / 2 - EXAMPLE_NAMES.length * 6;
    }

    @Override
    public void render(DrawContext context, int x, int y) {
        if (hasLiveScoreboard()) {
            // Already rendering behind this screen via InGameHudScoreboardMixin - see this
            // class's own Javadoc for why drawing anything here would just duplicate it.
            return;
        }

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
