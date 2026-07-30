package com.example.solstice.ui.widget;

import com.example.solstice.textures.TextureOption;
import com.example.solstice.textures.TexturePackManager;
import com.example.solstice.textures.TexturePreviewLoader;
import com.example.solstice.textures.TextureSlot;
import com.example.solstice.ui.SolsticeSounds;
import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;

/**
 * One row in a {@link com.example.solstice.ui.TextureCategoryScreen}: a live
 * preview of whichever option you're currently browsing, its name, and
 * left/right arrows to cycle through {@link TextureSlot#getOptions()}.
 *
 * <p>Cycling only changes local, uncommitted "pending" state and re-loads the
 * browsed option's real texture bytes directly via {@link TexturePreviewLoader}
 * (bypassing the atlas entirely) - it never touches the actually-applied
 * resource pack. Nothing real happens until the screen's "Done" button calls
 * {@link #getPendingIndex()} and applies it - see {@link TexturePackManager#applyIndex}.</p>
 */
public class TextureSwapCardWidget extends SolsticeClickableWidget {

    public static final int HEIGHT = 40;
    private static final int ARROW_W = 20;
    private static final int PREVIEW_SIZE = 24;
    private static final int PADDING = 8;

    private final TextureSlot slot;
    private final TextRenderer textRenderer;
    private int pendingIndex;

    public TextureSwapCardWidget(int x, int y, int width, TextureSlot slot, TextRenderer textRenderer) {
        super(x, y, width, HEIGHT, Text.of(slot.getDisplayName()));
        this.slot = slot;
        this.textRenderer = textRenderer;
        this.pendingIndex = TexturePackManager.getInstance().getSelectedIndex(slot);
        setTooltip(Tooltip.of(Text.of(slot.getDescription())));
    }

    public int getPendingIndex() { return pendingIndex; }
    public TextureSlot getSlot() { return slot; }

    public boolean hasPendingChange() {
        return pendingIndex != TexturePackManager.getInstance().getSelectedIndex(slot);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        SolsticeTheme.drawCard(context, getX(), getY(), getWidth(), HEIGHT, isHovered());

        int[] leftRect = leftArrowRect();
        int[] rightRect = rightArrowRect();
        drawArrow(context, leftRect, "<", contains(leftRect, mouseX, mouseY));
        drawArrow(context, rightRect, ">", contains(rightRect, mouseX, mouseY));

        int previewX = getX() + ARROW_W + PADDING;
        int previewY = getY() + (HEIGHT - PREVIEW_SIZE) / 2;
        SolsticeTheme.fillRect(context, previewX, previewY, PREVIEW_SIZE, PREVIEW_SIZE, ColorPalette.BG_PANEL);
        SolsticeTheme.drawBorder(context, previewX, previewY, PREVIEW_SIZE, PREVIEW_SIZE, ColorPalette.BORDER_DEFAULT);
        renderPreview(context, previewX + 4, previewY + 4, PREVIEW_SIZE - 8);

        int textX = previewX + PREVIEW_SIZE + PADDING;
        TextureOption current = slot.getOptions().get(pendingIndex);
        context.drawText(textRenderer, slot.getDisplayName(), textX, getY() + 8, ColorPalette.TEXT_PRIMARY, false);
        String label = current.displayName() + (hasPendingChange() ? " *" : "");
        context.drawText(textRenderer, label, textX, getY() + 22,
                hasPendingChange() ? ColorPalette.TEXT_ACCENT : ColorPalette.TEXT_SECONDARY, false);
    }

    private void renderPreview(DrawContext context, int x, int y, int size) {
        String assetPath = slot.getPreviewAssetPath();
        if (assetPath == null) {
            slot.renderFallback(context, x, y, size);
            return;
        }
        TextureOption option = slot.getOptions().get(pendingIndex);
        TexturePreviewLoader.Loaded loaded = TexturePreviewLoader.getInstance().load(option, assetPath);
        if (loaded == null) {
            // Load failed (shouldn't normally happen) - a blank box beats a crash.
            return;
        }
        // The 8-arg drawTexture overload treats the on-screen size as the
        // SOURCE region size too (confirmed via decompile: it delegates to
        // the 12-arg version with regionWidth/regionHeight = width/height) -
        // for any texture bigger than the icon box (several of these packs
        // use higher-resolution art, e.g. Vanilla+), that only sampled a
        // same-size corner instead of the whole image scaled down. Passing
        // the real loaded dimensions as the region size (12-arg overload)
        // samples the entire image regardless of its native resolution.
        context.drawTexture(RenderPipelines.GUI_TEXTURED, loaded.textureId(), x, y, 0f, 0f,
                size, size, loaded.width(), loaded.height(), loaded.width(), loaded.height());
    }

    private void drawArrow(DrawContext context, int[] rect, String label, boolean hovered) {
        SolsticeTheme.fillRect(context, rect[0], rect[1], rect[2], rect[3], hovered ? ColorPalette.BG_HOVER : ColorPalette.BG_CARD);
        SolsticeTheme.drawBorder(context, rect[0], rect[1], rect[2], rect[3], ColorPalette.BORDER_DEFAULT);
        int tw = textRenderer.getWidth(label);
        context.drawText(textRenderer, label, rect[0] + (rect[2] - tw) / 2, rect[1] + (rect[3] - 8) / 2,
                ColorPalette.TEXT_PRIMARY, false);
    }

    private int[] leftArrowRect() {
        return new int[]{getX() + 4, getY() + (HEIGHT - ARROW_W) / 2, ARROW_W, ARROW_W};
    }

    private int[] rightArrowRect() {
        return new int[]{getX() + getWidth() - ARROW_W - 4, getY() + (HEIGHT - ARROW_W) / 2, ARROW_W, ARROW_W};
    }

    private boolean contains(int[] rect, int px, int py) {
        return px >= rect[0] && px <= rect[0] + rect[2] && py >= rect[1] && py <= rect[1] + rect[3];
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x();
        int my = (int) click.y();
        int count = slot.getOptions().size();

        if (contains(leftArrowRect(), mx, my)) {
            SolsticeSounds.playClick();
            pendingIndex = ((pendingIndex - 1) % count + count) % count;
            return true;
        }
        if (contains(rightArrowRect(), mx, my)) {
            SolsticeSounds.playClick();
            pendingIndex = (pendingIndex + 1) % count;
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
