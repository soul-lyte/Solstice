package com.example.solstice.ui;

import com.example.solstice.ui.theme.ColorPalette;
import com.example.solstice.ui.theme.SolsticeTheme;
import com.example.solstice.ui.widget.SolsticeButton;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Full-info popup for one installed mod, opened by clicking its card on
 * {@link SolsticeModsScreen} - name, version, icon, full (wrapped) description,
 * authors, and license, the same "click a mod to see more" pattern as the
 * real ModMenu mod.
 */
public class ModDetailScreen extends Screen {

    private static final int PANEL_W = 280;
    private static final int PADDING = 14;
    private static final int ICON_SIZE = 40;
    private static final int LINE_H = 10;

    private final Screen parent;
    private final ModContainer container;

    private List<OrderedText> descriptionLines = List.of();
    private Identifier iconId;
    private int panelX, panelY, panelH;

    public ModDetailScreen(Screen parent, ModContainer container) {
        super(Text.of(container.getMetadata().getName()));
        this.parent = parent;
        this.container = container;
    }

    @Override
    protected void init() {
        ModMetadata meta = container.getMetadata();
        int innerW = PANEL_W - PADDING * 2;
        descriptionLines = meta.getDescription() != null && !meta.getDescription().isBlank()
                ? textRenderer.wrapLines(Text.of(meta.getDescription()), innerW)
                : List.of();
        iconId = loadIcon(meta);

        int y = ICON_SIZE + PADDING * 2 + 4;       // header block (icon + name/version)
        y += descriptionLines.size() * LINE_H + 6;  // description
        y += 6;                                      // divider

        String authors = meta.getAuthors().stream().map(Person::getName).collect(Collectors.joining(", "));
        if (!authors.isBlank()) y += LINE_H + 2;

        String license = String.join(", ", meta.getLicense());
        if (!license.isBlank()) y += LINE_H + 2;

        Optional<String> homepage = meta.getContact().get("homepage");
        if (homepage.isPresent()) y += LINE_H + 2;

        y += 8;
        panelH = y + 24;
        panelX = (width - PANEL_W) / 2;
        panelY = Math.max(20, (height - panelH) / 2);

        addDrawableChild(new SolsticeButton(panelX + (PANEL_W - 100) / 2, panelY + panelH - 22, 100, 18,
                textRenderer, "Back", this::close));
    }

    private Identifier loadIcon(ModMetadata meta) {
        Optional<String> iconPath = meta.getIconPath(ICON_SIZE);
        if (iconPath.isEmpty()) return null;
        Optional<Path> path = container.findPath(iconPath.get());
        if (path.isEmpty()) return null;

        try (InputStream stream = Files.newInputStream(path.get())) {
            NativeImage image = NativeImage.read(stream);
            Identifier id = Identifier.of("solstice", "modicon_detail/" + meta.getId());
            assert client != null;
            client.getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> meta.getId() + " icon detail", image));
            return id;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, ColorPalette.OVERLAY_DARK);
        SolsticeTheme.drawPanel(context, panelX, panelY, PANEL_W, panelH);

        ModMetadata meta = container.getMetadata();
        int textX = panelX + PADDING;
        int iconY = panelY + PADDING;
        if (iconId != null) {
            context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, iconId,
                    textX, iconY, 0f, 0f, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            textX += ICON_SIZE + 8;
        }

        context.drawText(textRenderer, meta.getName(), textX, iconY + 4, ColorPalette.TEXT_PRIMARY, false);
        context.drawText(textRenderer, meta.getVersion().getFriendlyString(), textX, iconY + 16, ColorPalette.TEXT_SECONDARY, false);
        context.drawText(textRenderer, meta.getId(), textX, iconY + 27, ColorPalette.ACCENT_DIM, false);

        int y = panelY + ICON_SIZE + PADDING * 2 + 4;
        int dx = panelX + PADDING;
        for (OrderedText line : descriptionLines) {
            context.drawText(textRenderer, line, dx, y, ColorPalette.TEXT_SECONDARY, false);
            y += LINE_H;
        }
        y += 6;
        SolsticeTheme.drawDivider(context, dx, y, PANEL_W - PADDING * 2);
        y += 6;

        String authors = meta.getAuthors().stream().map(Person::getName).collect(Collectors.joining(", "));
        if (!authors.isBlank()) {
            context.drawText(textRenderer, "Authors: " + authors, dx, y, ColorPalette.TEXT_SECONDARY, false);
            y += LINE_H + 2;
        }

        String license = String.join(", ", meta.getLicense());
        if (!license.isBlank()) {
            context.drawText(textRenderer, "License: " + license, dx, y, ColorPalette.TEXT_SECONDARY, false);
            y += LINE_H + 2;
        }

        Optional<String> homepage = meta.getContact().get("homepage");
        if (homepage.isPresent()) {
            context.drawText(textRenderer, "Homepage: " + homepage.get(), dx, y, ColorPalette.TEXT_SECONDARY, false);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        assert client != null;
        client.setScreen(parent);
    }

    // No shouldPause() override - see SolsticeScreen's identical note.
}
