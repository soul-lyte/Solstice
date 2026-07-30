package com.example.solstice.core.hud;

import net.minecraft.client.gui.DrawContext;

/**
 * A freely repositionable floating HUD box, manageable from the HUD editor
 * (see {@code HudEditorScreen}). Not every HUD-ish thing in Solstice is one
 * of these - overlays anchored to a specific vanilla element's own position
 * (like the AppleSkin-style hunger bar overlay) aren't, since "where" isn't
 * meaningful for them.
 */
public interface HudElement {

    /** Stable id used as the {@link HudLayoutManager} config key prefix. */
    String getId();

    /** Shown as the label on its draggable box in the HUD editor. */
    String getDisplayName();

    /** Approximate footprint for the editor's draggable box - real rendered size may vary slightly with content. */
    int getWidth();
    int getHeight();

    /**
     * Where this element sits before the user ever drags it - matters for
     * elements that mirror a real vanilla HUD piece (Boss Bar, Scoreboard),
     * so their editor box starts at the same spot vanilla actually draws
     * them instead of the screen's top-left corner. Screen-size-independent
     * elements can ignore the parameters. Defaults to (0, 0).
     */
    default int getDefaultX(int screenWidth, int screenHeight) { return 0; }
    default int getDefaultY(int screenWidth, int screenHeight) { return 0; }

    /** Whether this element starts visible before the user ever touches its toggle. True by default. */
    default boolean isVisibleByDefault() { return true; }

    /** Draws this element at the given top-left position. */
    void render(DrawContext context, int x, int y);
}
