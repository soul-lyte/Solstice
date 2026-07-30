package com.example.solstice.ui;

import com.example.solstice.core.config.ConfigManager;
import com.example.solstice.core.hud.HudElement;
import com.example.solstice.core.hud.HudLayoutManager;
import com.example.solstice.ui.theme.ColorPalette;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Free-text overlay - off by default, lets the user type whatever they want
 * (a tag, a stream label, anything) and have it drawn on screen like any
 * other {@link HudElement}: draggable/resizable from {@code HudEditorScreen},
 * its actual text edited from a field added onto {@code HudWidgetConfigScreen}
 * specifically for this element (see that class's Javadoc).
 */
public final class WatermarkHudElement implements HudElement {

    private static final WatermarkHudElement INSTANCE = new WatermarkHudElement();

    private static final int PAD_X = 4;
    private static final int PAD_Y = 3;
    private static final String TEXT_KEY = "hud_layout.watermark.text";

    private WatermarkHudElement() {}

    public static WatermarkHudElement getInstance() { return INSTANCE; }

    @Override public String getId()          { return "watermark"; }
    @Override public String getDisplayName() { return "Watermark"; }
    @Override public int getWidth()          { return 100; }
    @Override public int getHeight()         { return 10 + PAD_Y * 2; }
    @Override public boolean isVisibleByDefault() { return false; }

    public String getText() {
        return ConfigManager.getInstance().getString(TEXT_KEY, "Solstice");
    }

    public void setText(String text) {
        ConfigManager.getInstance().set(TEXT_KEY, text);
    }

    @Override
    public int getDefaultX(int screenWidth, int screenHeight) {
        return 2;
    }

    @Override
    public int getDefaultY(int screenWidth, int screenHeight) {
        return screenHeight - getHeight() - 2;
    }

    /** Called from {@link com.example.solstice.SolsticeClient} via {@code HudRenderCallback.EVENT}. */
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();

        boolean visible = HudLayoutManager.getInstance().isMasterVisible()
                && client.currentScreen == null
                && HudLayoutManager.getInstance().isVisible(getId(), isVisibleByDefault());
        if (!visible) {
            return;
        }

        int defaultX = getDefaultX(context.getScaledWindowWidth(), context.getScaledWindowHeight());
        int defaultY = getDefaultY(context.getScaledWindowWidth(), context.getScaledWindowHeight());
        int x = HudLayoutManager.getInstance().getX(getId(), defaultX);
        int y = HudLayoutManager.getInstance().getY(getId(), defaultY);
        render(context, x, y);
    }

    @Override
    public void render(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();

        int boxW = HudLayoutManager.getInstance().getWidth(getId(), getWidth());
        int boxH = HudLayoutManager.getInstance().getHeight(getId(), getHeight());
        int background = HudLayoutManager.getInstance().getBackground(getId(), 0x880D1117);
        int textColor = HudLayoutManager.getInstance().getTextColor(getId(), ColorPalette.TEXT_PRIMARY);

        context.fill(x, y, x + boxW, y + boxH, background);
        HudLayoutManager.withContentScale(context, x, y, getWidth(), getHeight(), boxW, boxH, () ->
                context.drawText(client.textRenderer, getText(), x + PAD_X, y + PAD_Y, textColor, false));
    }
}
