package com.example.solstice.textures;

import net.minecraft.client.gui.DrawContext;

import java.util.List;

/**
 * One swappable "thing" shown as a card in the Textures tab - a tool skin, a
 * GUI skin, a font, a utility reskin. Holds its own list of {@link TextureOption}s
 * (vanilla is always index 0).
 *
 * <p>Preview works one of two ways:
 * <ul>
 *   <li>{@code previewAssetPath} set (tools/GUI/utilities): the actual icon
 *   PNG for <em>whichever option is currently being browsed</em> is loaded
 *   straight from that pack's bundled bytes via {@link TexturePreviewLoader} -
 *   a true per-option preview that never touches the real active resource pack.</li>
 *   <li>{@code previewAssetPath} null (fonts): per-option preview isn't
 *   practical (would mean building a whole separate font/glyph-provider
 *   instance just to preview), so {@code fallbackRenderer} draws a fixed
 *   sample using whatever font is currently actually active instead.</li>
 * </ul>
 */
public final class TextureSlot {

    /** Draws a small preview at (x, y), size x size - used only when previewAssetPath is null. */
    public interface PreviewRenderer {
        void render(DrawContext context, int x, int y, int size);
    }

    private final String id;
    private final String displayName;
    private final String description;
    private final List<TextureOption> options;
    private final String previewAssetPath;
    private final PreviewRenderer fallbackRenderer;
    private final int defaultIndex;

    /** Icon-preview slot (tools/GUI/utilities) - true per-option preview via {@link TexturePreviewLoader}. */
    public TextureSlot(String id, String displayName, String description, List<TextureOption> options,
                        String previewAssetPath) {
        this(id, displayName, description, options, previewAssetPath, null, 0);
    }

    /** Icon-preview slot with a non-vanilla default selection (e.g. Bow/Crossbow Pull Indicator, on by default). */
    public TextureSlot(String id, String displayName, String description, List<TextureOption> options,
                        String previewAssetPath, int defaultIndex) {
        this(id, displayName, description, options, previewAssetPath, null, defaultIndex);
    }

    /** Fallback-renderer slot (fonts) - previewAssetPath is null, fallbackRenderer always draws the same way. */
    public TextureSlot(String id, String displayName, String description, List<TextureOption> options,
                        PreviewRenderer fallbackRenderer) {
        this(id, displayName, description, options, null, fallbackRenderer, 0);
    }

    public TextureSlot(String id, String displayName, String description, List<TextureOption> options,
                        String previewAssetPath, PreviewRenderer fallbackRenderer, int defaultIndex) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.options = options;
        this.previewAssetPath = previewAssetPath;
        this.fallbackRenderer = fallbackRenderer;
        this.defaultIndex = defaultIndex;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public List<TextureOption> getOptions() { return options; }
    public String getPreviewAssetPath() { return previewAssetPath; }
    public int getDefaultIndex() { return defaultIndex; }
    public PreviewRenderer getFallbackRenderer() { return fallbackRenderer; }

    /** Only used when {@link #getPreviewAssetPath()} is null. */
    public void renderFallback(DrawContext context, int x, int y, int size) {
        fallbackRenderer.render(context, x, y, size);
    }
}
