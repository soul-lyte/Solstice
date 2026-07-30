package com.example.solstice.textures;

import com.example.solstice.SolsticeMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads a preview icon straight from a bundled pack's raw PNG bytes (or, for
 * the vanilla option, straight from the game's own jar on the classpath) and
 * registers it as its own standalone GPU texture - completely independent of
 * whichever resource pack is actually active right now.
 *
 * <p>This is what lets {@link com.example.solstice.ui.widget.TextureSwapCardWidget}
 * show a true preview of an option you're merely browsing (via the left/right
 * arrows) without touching the real, currently-applied resource pack - only
 * the "Done" button in {@link com.example.solstice.ui.TextureCategoryScreen}
 * actually enables/disables packs and reloads.</p>
 */
public final class TexturePreviewLoader {

    private static final TexturePreviewLoader INSTANCE = new TexturePreviewLoader();

    /** Cached by option id + asset path so re-hovering the same option doesn't re-read/re-upload. */
    private final Map<String, Loaded> cache = new HashMap<>();

    public record Loaded(Identifier textureId, int width, int height) {}

    private TexturePreviewLoader() {}

    public static TexturePreviewLoader getInstance() { return INSTANCE; }

    /**
     * @param assetPath path relative to {@code assets/minecraft/}, e.g. {@code "textures/item/diamond_sword.png"}
     * @return the loaded preview, or null if this option/pack doesn't actually have that asset
     */
    public Loaded load(TextureOption option, String assetPath) {
        String cacheKey = (option.isVanilla() ? "vanilla" : option.packId()) + "|" + assetPath;
        Loaded cached = cache.get(cacheKey);
        if (cached != null) return cached;

        try (InputStream stream = openStream(option, assetPath)) {
            if (stream == null) return null;
            NativeImage image = NativeImage.read(stream);
            Identifier textureId = Identifier.of(SolsticeMod.MOD_ID, "preview/" + sanitize(cacheKey));
            MinecraftClient.getInstance().getTextureManager()
                    .registerTexture(textureId, new NativeImageBackedTexture(() -> cacheKey, image));
            Loaded loaded = new Loaded(textureId, image.getWidth(), image.getHeight());
            cache.put(cacheKey, loaded);
            return loaded;
        } catch (IOException e) {
            SolsticeMod.LOGGER.warn("[Solstice] Couldn't load texture preview for {} ({}): {}",
                    option.packId(), assetPath, e.getMessage());
            return null;
        }
    }

    private InputStream openStream(TextureOption option, String assetPath) {
        if (option.isVanilla()) {
            return TexturePreviewLoader.class.getResourceAsStream("/assets/minecraft/" + assetPath);
        }
        // A pack-root "preview.png" (a purpose-made preview graphic, distinct from
        // the actual in-game asset) wins over the shared slot asset path if present.
        InputStream dedicated = TexturePreviewLoader.class.getResourceAsStream(
                "/resourcepacks/" + option.packId() + "/preview.png");
        if (dedicated != null) return dedicated;
        return TexturePreviewLoader.class.getResourceAsStream(
                "/resourcepacks/" + option.packId() + "/assets/minecraft/" + assetPath);
    }

    private static String sanitize(String key) {
        return key.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
    }
}
