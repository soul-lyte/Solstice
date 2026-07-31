package com.example.solstice.textures;

import com.example.solstice.SolsticeMod;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Inspects a real resource pack's actual contents (never a build-time
 * curated list) to determine which Advanced-row categories it provides art
 * for - the runtime replacement for the old hand-picked, build-time-baked
 * {@link TextureSlots} options. Works on any {@link ResourcePackProfile},
 * enabled or not - {@link ResourcePackProfile#createResourcePack()} just
 * opens the folder/zip on disk, it doesn't require the pack to be active.
 */
public final class PackContentScanner {

    public enum Category { TOOLS, ARMOR, UTILITIES, GUI, FONT }

    private static final Map<Category, String> SIGNATURE_PATH = new EnumMap<>(Category.class);
    static {
        SIGNATURE_PATH.put(Category.TOOLS, "textures/item/diamond_sword.png");
        SIGNATURE_PATH.put(Category.ARMOR, "textures/item/diamond_chestplate.png");
        SIGNATURE_PATH.put(Category.UTILITIES, "textures/item/golden_apple.png");
        SIGNATURE_PATH.put(Category.GUI, "textures/gui/sprites/container/slot.png");
        SIGNATURE_PATH.put(Category.FONT, "textures/font/default.png");
    }

    private PackContentScanner() {}

    /** Which categories this pack provides real content for. Opens and closes its own ResourcePack handle. */
    public static Set<Category> scan(ResourcePackProfile profile) {
        Set<Category> found = EnumSet.noneOf(Category.class);
        try (ResourcePack pack = profile.createResourcePack()) {
            if (pack == null) return found;
            for (Map.Entry<Category, String> entry : SIGNATURE_PATH.entrySet()) {
                if (hasAsset(pack, entry.getValue())) {
                    found.add(entry.getKey());
                }
            }
        } catch (Exception e) {
            SolsticeMod.LOGGER.warn("[Solstice] Couldn't scan resource pack {} for texture categories: {}", profile.getId(), e.getMessage());
        }
        return found;
    }

    static boolean hasAsset(ResourcePack pack, String assetPath) {
        InputSupplier<InputStream> supplier = pack.open(ResourceType.CLIENT_RESOURCES, Identifier.of("minecraft", assetPath));
        if (supplier == null) return false;
        try (InputStream in = supplier.get()) {
            return in != null;
        } catch (IOException e) {
            return false;
        }
    }
}
