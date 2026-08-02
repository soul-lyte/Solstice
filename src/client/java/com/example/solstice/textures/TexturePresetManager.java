package com.example.solstice.textures;

import com.example.solstice.SolsticeMod;
import com.example.solstice.core.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Presets row logic: which whole pack is currently active (mutually
 * exclusive with every other known preset - selecting one disables all the
 * others, unlike the independent per-category Advanced-row slots), packs
 * added at runtime via the "Add your own" file picker, and saved custom
 * combos that snapshot a Preset choice plus the Advanced row's three
 * category selections (Tools/Utilities/Armor), mirroring {@code
 * ProfileManager}'s Custom-profile pattern.
 */
public final class TexturePresetManager {

    private static final String SELECTED_KEY = "textures.preset.selected";
    private static final String ADDED_COUNT_KEY = "textures.preset.added.count";
    private static final String SAVED_COUNT_KEY = "textures.preset.saved.count";

    public static final TexturePreset VANILLA = new TexturePreset("vanilla", "Vanilla", null, null);

    // Tiny Tools/Vanilla+/Tournament/SMP Essentials used to be bundled here as
    // whole-pack presets, but none of them ever came with a redistribution
    // license - see NOTICE.md. Removed on this branch in favor of dynamic
    // detection (DynamicTextureRegistry): if the user has one of those packs
    // installed themselves, its content now shows up automatically in the
    // Advanced row instead of Solstice ever shipping the pack itself.
    public static final List<TexturePreset> BUILTIN = List.of(VANILLA);

    // Must come after VANILLA/BUILTIN above, not before - real bug, not just style: static
    // fields initialize top-to-bottom in declaration order, and the constructor below reads
    // VANILLA.id() (for a saved combo's default preset id) - with INSTANCE declared first,
    // VANILLA was still null at that point, throwing a NullPointerException wrapped in
    // ExceptionInInitializerError the moment this class was first touched. Only crashed once
    // a real saved combo actually existed in config (SAVED_COUNT_KEY > 0), which is also why
    // launch-verifying against a fresh dev-environment config never caught it - same class of
    // gap as this project's other "only exercised by real content/state" Mixin cases.
    private static final TexturePresetManager INSTANCE = new TexturePresetManager();

    /** One saved combo: which Preset (by id) plus the Advanced row's three category slot indices. */
    public record SavedCombo(int index, String name, String presetId, int toolsIndex, int utilitiesIndex, int armorIndex) {}

    private final List<TexturePreset> addedPacks = new ArrayList<>();
    private final List<SavedCombo> savedCombos = new ArrayList<>();
    private String lastError;

    private TexturePresetManager() {
        ConfigManager cfg = ConfigManager.getInstance();
        int addedCount = cfg.getInt(ADDED_COUNT_KEY, 0);
        for (int i = 0; i < addedCount; i++) {
            String profileId = cfg.getString("textures.preset.added." + i + ".profile_id", null);
            String name = cfg.getString("textures.preset.added." + i + ".name", "Custom " + (i + 1));
            if (profileId != null) {
                addedPacks.add(new TexturePreset("added_" + i, name, profileId, null));
            }
        }
        int savedCount = cfg.getInt(SAVED_COUNT_KEY, 0);
        for (int i = 0; i < savedCount; i++) {
            String name = cfg.getString("textures.preset.saved." + i + ".name", "Custom " + (i + 1));
            String presetId = cfg.getString("textures.preset.saved." + i + ".preset_id", VANILLA.id());
            int tools = cfg.getInt("textures.preset.saved." + i + ".tools_index", 0);
            int utilities = cfg.getInt("textures.preset.saved." + i + ".utilities_index", 0);
            int armor = cfg.getInt("textures.preset.saved." + i + ".armor_index", 0);
            savedCombos.add(new SavedCombo(i, name, presetId, tools, utilities, armor));
        }
    }

    public static TexturePresetManager getInstance() { return INSTANCE; }

    /** Every selectable Presets-row card, in display order: built-ins, then added-via-file-picker packs. */
    public List<TexturePreset> getAllPresets() {
        List<TexturePreset> all = new ArrayList<>(BUILTIN);
        all.addAll(addedPacks);
        return all;
    }

    public List<TexturePreset> getAddedPacks() {
        return addedPacks;
    }

    public List<SavedCombo> getSavedCombos() {
        return savedCombos;
    }

    /** Set whenever {@link #select} or {@link #addPackFromDisk} fails - cleared at the start of each call. */
    public String getLastError() { return lastError; }

    public String getSelectedId() {
        return ConfigManager.getInstance().getString(SELECTED_KEY, VANILLA.id());
    }

    public TexturePreset getSelected() {
        String id = getSelectedId();
        for (TexturePreset preset : getAllPresets()) {
            if (preset.id().equals(id)) return preset;
        }
        return VANILLA;
    }

    /**
     * Called from {@code ClientLifecycleEvents.CLIENT_STARTED} - enables
     * whatever preset was persisted from a previous session, if it isn't
     * vanilla. Deliberately does NOT reload itself, matching {@code
     * TexturePackManager.applyPersistedSelections} - the caller reloads
     * once after every persisted-selection source has applied its own
     * enable/disable calls.
     *
     * @return true if a non-vanilla preset was actually enabled (i.e. a reload is needed)
     */
    public boolean applyPersistedSelection() {
        TexturePreset selected = getSelected();
        if (selected.isVanilla()) {
            return false;
        }
        MinecraftClient.getInstance().getResourcePackManager().enable(selected.packId());
        return true;
    }

    /** Enables the given preset's pack (if any) and disables every other known preset pack, then reloads once. */
    public void select(TexturePreset preset) {
        lastError = null;
        ResourcePackManager manager = MinecraftClient.getInstance().getResourcePackManager();
        for (TexturePreset other : getAllPresets()) {
            if (!other.isVanilla() && !other.packId().equals(preset.packId())) {
                manager.disable(other.packId());
            }
        }
        if (!preset.isVanilla()) {
            // enable() silently returns false (no exception, no log) if scanPacks() never
            // produced a matching profile - previously discarded entirely, which is why a
            // pack that failed to register looked exactly like a successful, working
            // selection. Surface it instead of pretending it worked.
            if (!manager.hasProfile(preset.packId()) || !manager.enable(preset.packId())) {
                lastError = "\"" + preset.displayName() + "\" couldn't be enabled - the pack wasn't found on disk.";
                SolsticeMod.LOGGER.warn("[Solstice] {} (profile id: {})", lastError, preset.packId());
            }
        }
        ConfigManager.getInstance().set(SELECTED_KEY, preset.id());
        MinecraftClient.getInstance().reloadResources();
    }

    /**
     * Copies a pack the user picked (a zip file or an extracted folder) into
     * the real {@code .minecraft/resourcepacks/} directory, rescans, and
     * registers it as a new selectable preset under vanilla's own real
     * {@code "file/<filename>"} profile id - the exact same mechanism as
     * manually dropping a pack into that folder, just automated. Runs
     * synchronously; callers invoke this off the render thread (see the
     * file-picker call site) since it touches disk.
     *
     * @return the newly added preset, or null if the copy failed
     */
    public TexturePreset addPackFromDisk(Path source) {
        lastError = null;
        MinecraftClient client = MinecraftClient.getInstance();
        Path targetDir = client.getResourcePackDir();
        String fileName = source.getFileName().toString();
        Path target = targetDir.resolve(fileName);

        try {
            if (Files.isDirectory(source)) {
                copyDirectory(source, target);
                flattenWrapperFolder(target);
            } else {
                Files.createDirectories(targetDir);
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            lastError = "Couldn't copy that pack: " + e.getMessage();
            SolsticeMod.LOGGER.warn("[Solstice] Couldn't copy resource pack {} into {}: {}", source, targetDir, e.getMessage());
            return null;
        }

        ResourcePackManager manager = client.getResourcePackManager();
        manager.scanPacks();

        String profileId = "file/" + fileName;
        if (!manager.hasProfile(profileId)) {
            lastError = "Couldn't find a pack.mcmeta in \"" + fileName + "\" - make sure you picked the folder that "
                    + "directly contains pack.mcmeta, not a parent folder.";
            SolsticeMod.LOGGER.warn("[Solstice] {} (expected profile id: {})", lastError, profileId);
            return null;
        }

        String displayName = stripPackExtension(fileName);
        int index = addedPacks.size();
        addedPacks.add(new TexturePreset("added_" + index, displayName, profileId, null));

        ConfigManager cfg = ConfigManager.getInstance();
        cfg.set("textures.preset.added." + index + ".profile_id", profileId);
        cfg.set("textures.preset.added." + index + ".name", displayName);
        cfg.set(ADDED_COUNT_KEY, addedPacks.size());

        return addedPacks.get(index);
    }

    /**
     * Common real-world case: a zip that unpacks to a single wrapper folder
     * (e.g. {@code MyPack-1.21/MyPack-1.21/pack.mcmeta}) rather than
     * {@code pack.mcmeta} sitting directly at the top level. Vanilla's own
     * {@code FileResourcePackProvider} only scans direct children of the real
     * resourcepacks directory, so a nested pack.mcmeta one level too deep
     * would never register at all - if the copied folder has no pack.mcmeta
     * of its own but contains exactly one subdirectory that does, flatten
     * that subdirectory's contents up a level.
     */
    private static void flattenWrapperFolder(Path target) throws IOException {
        if (Files.exists(target.resolve("pack.mcmeta"))) return;

        List<Path> subdirs;
        try (var stream = Files.list(target)) {
            subdirs = stream.filter(Files::isDirectory).toList();
        }
        if (subdirs.size() != 1) return;

        Path wrapper = subdirs.get(0);
        if (!Files.exists(wrapper.resolve("pack.mcmeta"))) return;

        try (var stream = Files.list(wrapper)) {
            for (Path child : stream.toList()) {
                Files.move(child, target.resolve(child.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        Files.delete(wrapper);
    }

    private static String stripPackExtension(String fileName) {
        return fileName.endsWith(".zip") ? fileName.substring(0, fileName.length() - 4) : fileName;
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /** Saves the current Preset selection + Advanced row's three category slot indices as a new named combo. */
    public void saveCombo(int toolsIndex, int utilitiesIndex, int armorIndex) {
        int index = savedCombos.size();
        String name = "Custom " + (index + 1);
        SavedCombo combo = new SavedCombo(index, name, getSelectedId(), toolsIndex, utilitiesIndex, armorIndex);
        savedCombos.add(combo);

        ConfigManager cfg = ConfigManager.getInstance();
        cfg.set("textures.preset.saved." + index + ".name", name);
        cfg.set("textures.preset.saved." + index + ".preset_id", combo.presetId());
        cfg.set("textures.preset.saved." + index + ".tools_index", toolsIndex);
        cfg.set("textures.preset.saved." + index + ".utilities_index", utilitiesIndex);
        cfg.set("textures.preset.saved." + index + ".armor_index", armorIndex);
        cfg.set(SAVED_COUNT_KEY, savedCombos.size());
    }

    /** Re-applies a saved combo: selects its Preset, then applies its three Advanced-row category indices and reloads once. */
    public void applyCombo(SavedCombo combo) {
        TexturePreset preset = getAllPresets().stream()
                .filter(p -> p.id().equals(combo.presetId()))
                .findFirst()
                .orElse(VANILLA);

        ResourcePackManager manager = MinecraftClient.getInstance().getResourcePackManager();
        for (TexturePreset other : getAllPresets()) {
            if (!other.isVanilla() && !other.packId().equals(preset.packId())) {
                manager.disable(other.packId());
            }
        }
        if (!preset.isVanilla()) {
            manager.enable(preset.packId());
        }
        ConfigManager.getInstance().set(SELECTED_KEY, preset.id());

        TexturePackManager packs = TexturePackManager.getInstance();
        DynamicTextureRegistry registry = DynamicTextureRegistry.getInstance();
        packs.applyIndex(registry.mergeSlots(List.of(TextureSlots.TOOLS_ALL)).get(0), combo.toolsIndex());
        packs.applyIndex(registry.mergeSlots(List.of(TextureSlots.UTILITIES_ALL)).get(0), combo.utilitiesIndex());
        packs.applyIndex(registry.mergeSlots(List.of(TextureSlots.ARMOR_ALL)).get(0), combo.armorIndex());

        MinecraftClient.getInstance().reloadResources();
    }
}
