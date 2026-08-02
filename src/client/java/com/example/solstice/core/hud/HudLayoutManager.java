package com.example.solstice.core.hud;

import com.example.solstice.core.config.ConfigManager;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Persists where each {@link HudElement} sits on screen, whether it's
 * visible, its box size, and its text/background colors, all keyed by its
 * id. Reuses the existing {@code ConfigManager} int-setting storage - no new
 * persistence mechanism needed.
 *
 * <p>Width/height are stored explicitly (not derived from the widget's own
 * rendered text) specifically so a widget's box stays a fixed size instead
 * of growing/shrinking as its displayed value changes digit count.</p>
 */
public final class HudLayoutManager {

    private static final HudLayoutManager INSTANCE = new HudLayoutManager();

    private final List<HudElement> elements = new ArrayList<>();

    private HudLayoutManager() {}

    public static HudLayoutManager getInstance() { return INSTANCE; }

    /** Registers an element so the HUD editor can list and position it. */
    public void register(HudElement element) {
        elements.add(element);
    }

    public List<HudElement> getElements() {
        return elements;
    }

    public int getX(String id, int defaultX) {
        return ConfigManager.getInstance().getInt("hud_layout." + id + ".x", defaultX);
    }

    public int getY(String id, int defaultY) {
        return ConfigManager.getInstance().getInt("hud_layout." + id + ".y", defaultY);
    }

    public void setPosition(String id, int x, int y) {
        ConfigManager.getInstance().set("hud_layout." + id + ".x", x);
        ConfigManager.getInstance().set("hud_layout." + id + ".y", y);
    }

    public int getWidth(String id, int defaultWidth) {
        return ConfigManager.getInstance().getInt("hud_layout." + id + ".width", defaultWidth);
    }

    public int getHeight(String id, int defaultHeight) {
        return ConfigManager.getInstance().getInt("hud_layout." + id + ".height", defaultHeight);
    }

    public void setSize(String id, int width, int height) {
        ConfigManager.getInstance().set("hud_layout." + id + ".width", width);
        ConfigManager.getInstance().set("hud_layout." + id + ".height", height);
    }

    public void setHeight(String id, int height) {
        ConfigManager.getInstance().set("hud_layout." + id + ".height", height);
    }

    /**
     * Dedicated key namespace for a {@link HudElement#hasLiveNaturalAnchor()}
     * element's offset from its live natural position/width - deliberately
     * NOT the same {@code .x}/{@code .y}/{@code .width} keys {@link #getX}/
     * {@link #getY}/{@link #getWidth} use for plain absolute storage.
     * Reusing those would mean any value a user had already dragged/resized
     * for that element under the old absolute model - a real, full-size
     * screen coordinate - gets silently reinterpreted as a tiny pixel
     * offset the moment it switches to this model, adding a leftover
     * ~1000+ px value on top of the live natural position and pushing the
     * element far off-screen (confirmed as the real cause of "the
     * scoreboard doesn't show at all anymore" after that switch - not a
     * hypothetical). A fresh, distinct key namespace starts every such
     * element at offset 0 (exactly natural) with no migration needed.
     */
    public int getOffsetX(String id, int defaultOffsetX) {
        return ConfigManager.getInstance().getInt("hud_layout." + id + ".offset_x", defaultOffsetX);
    }

    public int getOffsetY(String id, int defaultOffsetY) {
        return ConfigManager.getInstance().getInt("hud_layout." + id + ".offset_y", defaultOffsetY);
    }

    public void setOffsetPosition(String id, int offsetX, int offsetY) {
        ConfigManager.getInstance().set("hud_layout." + id + ".offset_x", offsetX);
        ConfigManager.getInstance().set("hud_layout." + id + ".offset_y", offsetY);
    }

    public int getOffsetWidth(String id, int defaultOffsetWidth) {
        return ConfigManager.getInstance().getInt("hud_layout." + id + ".offset_width", defaultOffsetWidth);
    }

    public void setOffsetWidth(String id, int offsetWidth) {
        ConfigManager.getInstance().set("hud_layout." + id + ".offset_width", offsetWidth);
    }

    public boolean isVisible(String id, boolean defaultVisible) {
        return ConfigManager.getInstance().getBoolean("hud_layout." + id + ".visible", defaultVisible);
    }

    public void setVisible(String id, boolean visible) {
        ConfigManager.getInstance().set("hud_layout." + id + ".visible", visible);
    }

    /**
     * One switch for the whole Solstice HUD overlay layer (FPS, RAM, Boss
     * Bar, Scoreboard, Watermark) - was already half-wired (FpsWidget/
     * RamWidget both checked the "hud.enabled" key) but nothing ever
     * exposed a way to actually set it. Every {@code onHudRender} in this
     * layer should check this in addition to its own individual {@link
     * #isVisible}.
     */
    public boolean isMasterVisible() {
        return ConfigManager.getInstance().getBoolean("hud.enabled", true);
    }

    public void setMasterVisible(boolean visible) {
        ConfigManager.getInstance().set("hud.enabled", visible);
    }

    public int getBackground(String id, int defaultColor) {
        return ConfigManager.getInstance().getInt("hud_layout." + id + ".background", defaultColor);
    }

    public void setBackground(String id, int color) {
        ConfigManager.getInstance().set("hud_layout." + id + ".background", color);
    }

    public int getTextColor(String id, int defaultColor) {
        return ConfigManager.getInstance().getInt("hud_layout." + id + ".text_color", defaultColor);
    }

    public void setTextColor(String id, int color) {
        ConfigManager.getInstance().set("hud_layout." + id + ".text_color", color);
    }

    /**
     * Wraps {@code content} so its draws scale relative to the box's own
     * top-left corner {@code (x, y)} instead of the screen origin - resizing
     * a HUD element now visibly grows/shrinks its rendered content (text,
     * icons), not just the background rectangle behind it. {@code content}
     * should draw using the same absolute screen coordinates it always has
     * (e.g. {@code x + PAD_X}) - the anchor-and-scale math below accounts
     * for that, no coordinate changes needed at the call site.
     *
     * <p>Confirmed via {@code javap}: {@code DrawContext.getMatrices()}
     * returns a real {@code org.joml.Matrix3x2fStack} (already on the
     * classpath as a Minecraft/JOML dependency, no new Gradle dependency
     * needed) with public {@code pushMatrix()/popMatrix()/scale(float,float)
     * /translate(float,float)}.</p>
     *
     * @param naturalW the element's own un-resized width (its {@code getWidth()})
     * @param naturalH the element's own un-resized height (its {@code getHeight()})
     * @param boxW     the current, possibly user-resized, stored width
     * @param boxH     the current, possibly user-resized, stored height
     */
    public static void withContentScale(DrawContext context, int x, int y,
                                         int naturalW, int naturalH, int boxW, int boxH,
                                         Runnable content) {
        float scaleX = naturalW > 0 ? (float) boxW / naturalW : 1f;
        float scaleY = naturalH > 0 ? (float) boxH / naturalH : 1f;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(scaleX, scaleY);
        context.getMatrices().translate(-x, -y);
        content.run();
        context.getMatrices().popMatrix();
    }
}
