package com.example.solstice.textures;

import com.example.solstice.SolsticeMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Scans every resource pack the client currently knows about - enabled or
 * not, whether it came from vanilla's own Resource Packs screen or
 * Solstice's "Add your own" picker, or was just dropped into
 * {@code .minecraft/resourcepacks/} and never touched - for real Advanced-row
 * content, and merges the result into a fresh {@link TextureSlot} each time
 * a category screen is opened. Replaces the old build-time-curated approach
 * for the four categories this covers (Tools/Armor/Utilities/GUI/Fonts);
 * narrow single-purpose slots (cobweb, bow/crossbow) are passed through
 * unchanged.
 *
 * <p>Selecting a detected option extracts just that category's real files
 * from the source pack into a small synthetic pack under
 * {@code .minecraft/resourcepacks/} (never Solstice's own jar - nothing is
 * bundled or redistributed), then enables it through the same real
 * {@code "file/<name>"} mechanism vanilla's own Resource Packs screen and
 * "Add your own" already use.</p>
 */
public final class DynamicTextureRegistry {

    private static final DynamicTextureRegistry INSTANCE = new DynamicTextureRegistry();

    public static final String DYNAMIC_PACK_PREFIX = "solstice_dynamic_";

    private static final Map<String, PackContentScanner.Category> SLOT_CATEGORY = Map.of(
            "tools_all", PackContentScanner.Category.TOOLS,
            "armor_all", PackContentScanner.Category.ARMOR,
            "utilities_all", PackContentScanner.Category.UTILITIES,
            "gui_skin", PackContentScanner.Category.GUI,
            "font", PackContentScanner.Category.FONT
    );

    private static final List<String> TOOL_MATERIALS = List.of("wooden", "stone", "iron", "golden", "diamond", "netherite");
    private static final List<String> TOOL_TYPES = List.of("sword", "pickaxe", "axe", "shovel", "hoe");
    private static final List<String> ARMOR_MATERIALS = List.of("leather", "chainmail", "iron", "golden", "diamond", "netherite");
    private static final List<String> ARMOR_PIECES = List.of("helmet", "chestplate", "leggings", "boots");

    /** Where a dynamic {@link TextureOption}'s {@code packId} actually came from. */
    public record DynamicSource(String sourceProfileId, PackContentScanner.Category category) {}

    private final Map<String, DynamicSource> sources = new HashMap<>();

    private DynamicTextureRegistry() {}

    public static DynamicTextureRegistry getInstance() { return INSTANCE; }

    public DynamicSource lookup(String optionPackId) {
        return sources.get(optionPackId);
    }

    /** Returns a fresh copy of each slot, with dynamically-detected options appended where applicable. */
    public List<TextureSlot> mergeSlots(List<TextureSlot> baseSlots) {
        List<TextureSlot> result = new ArrayList<>(baseSlots.size());
        for (TextureSlot base : baseSlots) {
            PackContentScanner.Category category = SLOT_CATEGORY.get(base.getId());
            result.add(category == null ? base : mergeOne(base, category));
        }
        return result;
    }

    private TextureSlot mergeOne(TextureSlot base, PackContentScanner.Category category) {
        List<TextureOption> merged = new ArrayList<>(base.getOptions());

        List<ResourcePackProfile> candidates = new ArrayList<>(
                MinecraftClient.getInstance().getResourcePackManager().getProfiles());
        candidates.sort(Comparator.comparing(ResourcePackProfile::getId));

        for (ResourcePackProfile profile : candidates) {
            String id = profile.getId();
            if (id.equals("vanilla") || id.startsWith(SolsticeMod.MOD_ID + ":") || id.startsWith("file/" + DYNAMIC_PACK_PREFIX)) {
                // "vanilla" is itself a real ResourcePackProfile (vanilla's own built-in
                // assets, usually displayed as "Default") - it trivially has every
                // signature file by definition, which without this exclusion showed up
                // as a duplicate "Default" option alongside the base slot's own "Vanilla".
                continue;
            }
            if (!PackContentScanner.scan(profile).contains(category)) {
                continue;
            }
            String optionId = "dynamic_" + sanitize(id);
            String optionPackId = "dyn_" + sanitize(id) + "_" + base.getId();
            sources.put(optionPackId, new DynamicSource(id, category));
            merged.add(new TextureOption(optionId, profile.getDisplayName().getString(), optionPackId));
        }

        return new TextureSlot(base.getId(), base.getDisplayName(), base.getDescription(), merged,
                base.getPreviewAssetPath(), base.getFallbackRenderer(), base.getDefaultIndex());
    }

    /**
     * Ensures a dynamic option's synthetic pack exists and is enabled/disabled
     * as requested. Extraction only happens the first time an option is
     * actually selected, not during scanning.
     */
    public boolean applyDynamicOption(String optionPackId, boolean enabled) {
        DynamicSource source = sources.get(optionPackId);
        if (source == null) return false;

        MinecraftClient client = MinecraftClient.getInstance();
        ResourcePackManager manager = client.getResourcePackManager();
        String folderName = DYNAMIC_PACK_PREFIX + optionPackId;
        String profileId = "file/" + folderName;

        if (enabled) {
            if (!manager.hasProfile(profileId)) {
                if (!extract(source, folderName)) return false;
                manager.scanPacks();
            }
            if (!manager.hasProfile(profileId)) {
                SolsticeMod.LOGGER.warn("[Solstice] Dynamic texture pack {} extracted but not found by scanPacks()", folderName);
                return false;
            }
            return manager.enable(profileId);
        } else {
            if (manager.hasProfile(profileId)) {
                return manager.disable(profileId);
            }
            return true;
        }
    }

    private boolean extract(DynamicSource source, String folderName) {
        MinecraftClient client = MinecraftClient.getInstance();
        ResourcePackProfile sourceProfile = client.getResourcePackManager().getProfile(source.sourceProfileId());
        if (sourceProfile == null) return false;

        Path dest = client.getResourcePackDir().resolve(folderName);
        try (ResourcePack pack = sourceProfile.createResourcePack()) {
            if (pack == null) return false;
            Files.createDirectories(dest);
            switch (source.category()) {
                case TOOLS -> { for (String p : toolAssetPaths()) copyIfPresent(pack, p, dest); }
                case ARMOR -> { for (String p : armorAssetPaths()) copyIfPresent(pack, p, dest); }
                case GUI -> copyPrefix(pack, "textures/gui", dest, Set.of());
                case FONT -> copyPrefix(pack, "textures/font", dest, Set.of());
                case UTILITIES -> {
                    Set<String> exclude = new LinkedHashSet<>();
                    exclude.addAll(toolAssetPaths());
                    exclude.addAll(armorAssetPaths());
                    copyPrefix(pack, "textures/item", dest, exclude);
                }
            }
            writePackMcmeta(dest, "Solstice dynamic - " + source.category() + " (from " + sourceProfile.getId() + ")");
            return true;
        } catch (Exception e) {
            SolsticeMod.LOGGER.warn("[Solstice] Failed to extract dynamic texture pack from {}: {}", source.sourceProfileId(), e.getMessage());
            return false;
        }
    }

    private static Set<String> toolAssetPaths() {
        Set<String> set = new LinkedHashSet<>();
        for (String m : TOOL_MATERIALS) {
            for (String t : TOOL_TYPES) {
                set.add("textures/item/" + m + "_" + t + ".png");
            }
        }
        return set;
    }

    private static Set<String> armorAssetPaths() {
        Set<String> set = new LinkedHashSet<>();
        for (String m : ARMOR_MATERIALS) {
            for (String p : ARMOR_PIECES) {
                set.add("textures/item/" + m + "_" + p + ".png");
            }
            set.add("textures/entity/equipment/humanoid/" + m + ".png");
            set.add("textures/entity/equipment/humanoid_leggings/" + m + ".png");
        }
        return set;
    }

    private static void copyIfPresent(ResourcePack pack, String assetPath, Path destRoot) {
        InputSupplier<InputStream> supplier = pack.open(ResourceType.CLIENT_RESOURCES, Identifier.of("minecraft", assetPath));
        if (supplier == null) return;
        try (InputStream in = supplier.get()) {
            if (in == null) return;
            Path dest = destRoot.resolve("assets/minecraft").resolve(assetPath);
            Files.createDirectories(dest.getParent());
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            SolsticeMod.LOGGER.warn("[Solstice] Failed copying {} while extracting dynamic pack: {}", assetPath, e.getMessage());
        }
    }

    private static void copyPrefix(ResourcePack pack, String prefix, Path destRoot, Set<String> exclude) {
        pack.findResources(ResourceType.CLIENT_RESOURCES, "minecraft", prefix, (identifier, supplier) -> {
            String path = identifier.getPath();
            if (exclude.contains(path)) return;
            try (InputStream in = supplier.get()) {
                if (in == null) return;
                Path dest = destRoot.resolve("assets/minecraft").resolve(path);
                Files.createDirectories(dest.getParent());
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                SolsticeMod.LOGGER.warn("[Solstice] Failed copying {} while extracting dynamic pack: {}", path, e.getMessage());
            }
        });
    }

    private static void writePackMcmeta(Path dest, String description) throws IOException {
        String escaped = description.replace("\\", "\\\\").replace("\"", "'");
        String json = "{\"pack\":{\"pack_format\":34,\"description\":\"" + escaped + "\"}}";
        Files.writeString(dest.resolve("pack.mcmeta"), json, StandardCharsets.UTF_8);
    }

    private static String sanitize(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }
}
