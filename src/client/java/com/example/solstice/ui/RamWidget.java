package com.example.solstice.ui;

import com.example.solstice.core.hud.HudElement;
import com.example.solstice.core.hud.HudLayoutManager;
import com.example.solstice.performance.memory.MemoryModule;
import com.example.solstice.ui.theme.ColorPalette;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * RAM usage HUD widget - independently positionable, hideable, and
 * background-customizable from the HUD editor (see {@code HudEditorScreen}).
 *
 * <p>Split out of the old combined {@code HudOverlay} so FPS and RAM can be
 * toggled and styled separately.</p>
 */
public final class RamWidget implements HudElement {

    private static final RamWidget INSTANCE = new RamWidget();

    private static final int LINE_H = 10;
    private static final int PAD_X = 4;
    private static final int PAD_Y = 2;

    private RamWidget() {}

    public static RamWidget getInstance() { return INSTANCE; }

    @Override public String getId()          { return "ram"; }
    @Override public String getDisplayName() { return "RAM Usage"; }
    @Override public int getWidth()          { return 110; }
    @Override public int getHeight()         { return LINE_H + PAD_Y * 2; }

    /** Called from {@link com.example.solstice.SolsticeClient} via {@code HudRenderCallback.EVENT}. */
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();

        boolean visible = HudLayoutManager.getInstance().isMasterVisible()
                && !client.debugHudEntryList.isF3Enabled()
                && client.currentScreen == null
                && HudLayoutManager.getInstance().isVisible(getId(), true);
        if (!visible) return;

        int defaultX = context.getScaledWindowWidth() - getWidth() - 2;
        int x = HudLayoutManager.getInstance().getX(getId(), defaultX);
        int y = HudLayoutManager.getInstance().getY(getId(), 2 + getHeight() + 2);
        render(context, x, y);
    }

    /**
     * Draws the widget at the given position - used both by real gameplay and the HUD
     * editor's live preview. Box size is a fixed, user-resizable value from
     * {@link HudLayoutManager} rather than measured from the current text, so it
     * doesn't grow/shrink as the displayed number changes digit count.
     */
    @Override
    public void render(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();

        String line = "RAM: " + MemoryModule.getInstance().getHudString();
        int boxW = HudLayoutManager.getInstance().getWidth(getId(), getWidth());
        int boxH = HudLayoutManager.getInstance().getHeight(getId(), getHeight());
        int background = HudLayoutManager.getInstance().getBackground(getId(), 0x880D1117);
        int textColor = HudLayoutManager.getInstance().getTextColor(getId(), ColorPalette.TEXT_SECONDARY);

        context.fill(x, y, x + boxW, y + boxH, background);
        HudLayoutManager.withContentScale(context, x, y, getWidth(), getHeight(), boxW, boxH, () ->
                context.drawText(client.textRenderer, line, x + PAD_X, y + PAD_Y, textColor, false));
    }
}
