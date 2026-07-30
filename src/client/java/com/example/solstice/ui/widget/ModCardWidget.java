package com.example.solstice.ui.widget;

import com.example.solstice.ui.SolsticeSounds;
import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * A horizontal box for one installed mod in {@link com.example.solstice.ui.SolsticeModsScreen}
 * - icon, name + version, one-line truncated description. Click opens
 * {@link com.example.solstice.ui.ModDetailScreen} for the full picture, same
 * click-for-details pattern as the real ModMenu mod.
 */
public class ModCardWidget extends SolsticeClickableWidget {

    public static final int HEIGHT = 48;
    private static final int PADDING = 8;
    private static final int ICON_SIZE = 28;

    private final ModContainer container;
    private final MinecraftClient client;
    private final TextRenderer textRenderer;
    private final Runnable onClick;
    private Identifier iconId;
    private boolean iconLoadAttempted;

    public ModCardWidget(int x, int y, int width, ModContainer container, MinecraftClient client,
                          TextRenderer textRenderer, Runnable onClick) {
        super(x, y, width, HEIGHT, Text.of(container.getMetadata().getName()));
        this.container = container;
        this.client = client;
        this.textRenderer = textRenderer;
        this.onClick = onClick;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        SolsticeTheme.drawCard(context, getX(), getY(), getWidth(), HEIGHT, isHovered());

        ModMetadata meta = container.getMetadata();
        int iconY = getY() + (HEIGHT - ICON_SIZE) / 2;
        Identifier icon = loadIcon(meta);
        int textX = getX() + PADDING;
        if (icon != null) {
            context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, icon,
                    getX() + PADDING, iconY, 0f, 0f, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            textX = getX() + PADDING + ICON_SIZE + 8;
        }

        int maxTextW = getX() + getWidth() - textX - PADDING;

        String nameLine = meta.getName() + " " + meta.getVersion().getFriendlyString();
        if (textRenderer.getWidth(nameLine) > maxTextW) {
            nameLine = textRenderer.trimToWidth(nameLine, maxTextW - textRenderer.getWidth("…")) + "…";
        }
        context.drawText(textRenderer, nameLine, textX, getY() + 8, ColorPalette.TEXT_PRIMARY, false);

        String desc = meta.getDescription();
        if (desc != null && !desc.isBlank()) {
            if (textRenderer.getWidth(desc) > maxTextW) {
                desc = textRenderer.trimToWidth(desc, maxTextW - textRenderer.getWidth("…")) + "…";
            }
            context.drawText(textRenderer, desc, textX, getY() + 20, ColorPalette.TEXT_SECONDARY, false);
        }

        context.drawText(textRenderer, "Click for details", textX, getY() + HEIGHT - 12, ColorPalette.ACCENT_DIM, false);
    }

    /** Lazily loads and registers this mod's icon as its own standalone texture, once. */
    private Identifier loadIcon(ModMetadata meta) {
        if (iconLoadAttempted) return iconId;
        iconLoadAttempted = true;

        Optional<String> iconPath = meta.getIconPath(ICON_SIZE);
        if (iconPath.isEmpty()) return null;
        Optional<Path> path = container.findPath(iconPath.get());
        if (path.isEmpty()) return null;

        try (InputStream stream = Files.newInputStream(path.get())) {
            NativeImage image = NativeImage.read(stream);
            Identifier id = Identifier.of("solstice", "modicon/" + meta.getId());
            client.getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> meta.getId() + " icon", image));
            iconId = id;
        } catch (IOException ignored) {
            iconId = null;
        }
        return iconId;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            SolsticeSounds.playClick();
            onClick.run();
            return true;
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
