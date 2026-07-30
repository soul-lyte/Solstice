package com.example.solstice.ui.widget;

import com.example.solstice.textures.TexturePackIconLoader;
import com.example.solstice.textures.TexturePreset;
import com.example.solstice.textures.TexturePreviewLoader;
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
 * A card for one whole-pack {@link TexturePreset} in the Textures tab's
 * Presets row - shows the pack's own real {@code pack.png} icon (see
 * {@link TexturePackIconLoader}) plus its credit line where known (just the
 * first line under the name; the full text - every named contributor for
 * multi-artist packs like Tournament - shows as a hover tooltip, since a
 * card this size can't fit it all inline), applies immediately on click (no
 * confirmation, matching {@code ProfileCardWidget}), and shows an accent
 * border when it's the currently active preset.
 */
public class TexturePresetCardWidget extends SolsticeClickableWidget {

    public static final int HEIGHT = 40;
    private static final int PADDING = 5;
    private static final int ICON_SIZE = 28;

    private final TexturePreset preset;
    private final TextRenderer textRenderer;
    private final boolean selected;
    private final Runnable onApplied;

    public TexturePresetCardWidget(int x, int y, int width, TexturePreset preset, TextRenderer textRenderer,
                                    boolean selected, Runnable onApplied) {
        super(x, y, width, HEIGHT, Text.of(preset.displayName()));
        this.preset = preset;
        this.textRenderer = textRenderer;
        this.selected = selected;
        this.onApplied = onApplied;
        if (preset.credit() != null) {
            setTooltip(Tooltip.of(Text.literal(preset.credit())));
        }
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        SolsticeTheme.drawCard(context, getX(), getY(), getWidth(), HEIGHT, isHovered());
        if (selected) {
            SolsticeTheme.drawBorder(context, getX(), getY(), getWidth(), HEIGHT, ColorPalette.ACCENT_NEUTRAL);
        }

        int iconX = getX() + PADDING;
        int iconY = getY() + (HEIGHT - ICON_SIZE) / 2;
        renderIcon(context, iconX, iconY);

        int textX = iconX + ICON_SIZE + PADDING;
        int maxTextW = getWidth() - (textX - getX()) - PADDING;

        String name = trimToWidth(getMessage().getString(), maxTextW);
        context.drawText(textRenderer, name, textX, getY() + 6,
                selected ? ColorPalette.TEXT_ACCENT : ColorPalette.TEXT_PRIMARY, false);

        if (preset.credit() != null) {
            String firstLine = preset.credit().split("\n", 2)[0];
            String credit = trimToWidth(firstLine, maxTextW);
            context.drawText(textRenderer, credit, textX, getY() + 20, ColorPalette.TEXT_SECONDARY, false);
        }
    }

    private String trimToWidth(String text, int maxWidth) {
        if (textRenderer.getWidth(text) <= maxWidth) return text;
        return textRenderer.trimToWidth(text, Math.max(0, maxWidth - textRenderer.getWidth("…"))) + "…";
    }

    private void renderIcon(DrawContext context, int x, int y) {
        TexturePreviewLoader.Loaded loaded = TexturePackIconLoader.getInstance().load(preset);
        if (loaded == null) {
            SolsticeTheme.fillRect(context, x, y, ICON_SIZE, ICON_SIZE, ColorPalette.BG_CARD);
            return;
        }
        // Sample the whole real pack.png regardless of its native resolution -
        // see TextureSwapCardWidget's identical fix for why the 8-arg overload
        // (region size = on-screen size) crops instead of scaling down.
        context.drawTexture(RenderPipelines.GUI_TEXTURED, loaded.textureId(), x, y, 0f, 0f,
                ICON_SIZE, ICON_SIZE, loaded.width(), loaded.height(), loaded.width(), loaded.height());
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            SolsticeSounds.playClick();
            onApplied.run();
            return true;
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
