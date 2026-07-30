package com.example.solstice.textures;

import com.example.solstice.SolsticeMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Loads a preset's real {@code pack.png} icon for {@code
 * TexturePresetCardWidget} - unlike {@link TexturePreviewLoader} (bundled
 * packs only, via the classpath), this also handles packs added at runtime
 * via the file picker, which live on real disk as either an extracted
 * folder or a {@code .zip}.
 */
public final class TexturePackIconLoader {

    private static final TexturePackIconLoader INSTANCE = new TexturePackIconLoader();

    private final Map<String, TexturePreviewLoader.Loaded> cache = new HashMap<>();

    private TexturePackIconLoader() {}

    public static TexturePackIconLoader getInstance() { return INSTANCE; }

    public TexturePreviewLoader.Loaded load(TexturePreset preset) {
        if (preset.isVanilla()) return null;

        TexturePreviewLoader.Loaded cached = cache.get(preset.id());
        if (cached != null) return cached;

        try (InputStream stream = openStream(preset)) {
            if (stream == null) return null;
            NativeImage image = NativeImage.read(stream);
            Identifier textureId = Identifier.of(SolsticeMod.MOD_ID, "preset_icon/" + sanitize(preset.id()));
            MinecraftClient.getInstance().getTextureManager()
                    .registerTexture(textureId, new NativeImageBackedTexture(preset::id, image));
            TexturePreviewLoader.Loaded loaded = new TexturePreviewLoader.Loaded(textureId, image.getWidth(), image.getHeight());
            cache.put(preset.id(), loaded);
            return loaded;
        } catch (IOException e) {
            SolsticeMod.LOGGER.warn("[Solstice] Couldn't load preset icon for {}: {}", preset.id(), e.getMessage());
            return null;
        }
    }

    private InputStream openStream(TexturePreset preset) throws IOException {
        String packId = preset.packId();
        if (packId.startsWith("solstice:")) {
            String bare = packId.substring("solstice:".length());
            return TexturePackIconLoader.class.getResourceAsStream("/resourcepacks/" + bare + "/pack.png");
        }
        if (packId.startsWith("file/")) {
            String fileName = packId.substring("file/".length());
            Path path = MinecraftClient.getInstance().getResourcePackDir().resolve(fileName);
            if (Files.isDirectory(path)) {
                Path iconPath = path.resolve("pack.png");
                return Files.exists(iconPath) ? Files.newInputStream(iconPath) : null;
            }
            if (Files.isRegularFile(path)) {
                try (ZipFile zip = new ZipFile(path.toFile())) {
                    ZipEntry entry = zip.getEntry("pack.png");
                    if (entry == null) return null;
                    byte[] bytes;
                    try (InputStream in = zip.getInputStream(entry)) {
                        bytes = in.readAllBytes();
                    }
                    return new ByteArrayInputStream(bytes);
                }
            }
        }
        return null;
    }

    private static String sanitize(String key) {
        return key.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
    }
}
