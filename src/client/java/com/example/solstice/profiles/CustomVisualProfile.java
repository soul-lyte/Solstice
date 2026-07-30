package com.example.solstice.profiles;

import com.example.solstice.core.config.ConfigManager;
import com.example.solstice.core.module.Module;
import com.example.solstice.core.module.ModuleCategory;
import com.example.solstice.core.module.ModuleRegistry;
import com.example.solstice.qol.crosshair.CrosshairModule;
import com.example.solstice.textures.TexturePackManager;
import com.example.solstice.textures.TextureSlots;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One user-saveable "Custom" slot for the Visual profile row. Captures
 * which QOL modules are enabled, the crosshair style, and the GUI skin
 * index at the moment it's saved (via {@link #captureAndSave()}), so it
 * can be re-applied and drift-compared against later - distinct from
 * whatever the live, unsaved state currently is.
 *
 * <p>Keyed by an {@code index} rather than being a singleton - {@link
 * ProfileManager} keeps a list of these (however many the user has saved),
 * each with its own persisted key prefix ({@code
 * profiles.visual.custom.<index>.*}), so saving a new custom profile no
 * longer overwrites the previous one. Default name is "Custom N" ({@code
 * index + 1}), matching the order they were saved in.</p>
 */
public final class CustomVisualProfile implements Profile, Renameable {

    private final int index;

    private String name;
    private final Map<String, Boolean> moduleEnabled = new LinkedHashMap<>();
    private int guiSkinIndex;
    private int crosshairStyleIndex;

    public CustomVisualProfile(int index) {
        this.index = index;
        load();
    }

    private String key(String suffix) {
        return "profiles.visual.custom." + index + "." + suffix;
    }

    private void load() {
        ConfigManager cfg = ConfigManager.getInstance();
        name = cfg.getString(key("name"), "Custom " + (index + 1));
        guiSkinIndex = cfg.getInt(key("gui_skin_index"), 0);
        crosshairStyleIndex = cfg.getInt(key("crosshair_style"), CrosshairModule.styleIndex);
        moduleEnabled.clear();
        for (Module module : ModuleRegistry.getInstance().getByCategory(ModuleCategory.QUALITY_OF_LIFE)) {
            moduleEnabled.put(module.getId(), cfg.getBoolean(key("module." + module.getId()), module.isEnabled()));
        }
    }

    /** Captures the current live QOL module states, crosshair style, and GUI skin as this profile's new saved snapshot. */
    public void captureAndSave() {
        ConfigManager cfg = ConfigManager.getInstance();
        cfg.set(key("name"), name);
        for (Module module : ModuleRegistry.getInstance().getByCategory(ModuleCategory.QUALITY_OF_LIFE)) {
            boolean enabled = module.isEnabled();
            moduleEnabled.put(module.getId(), enabled);
            cfg.set(key("module." + module.getId()), enabled);
        }
        crosshairStyleIndex = CrosshairModule.styleIndex;
        cfg.set(key("crosshair_style"), crosshairStyleIndex);
        guiSkinIndex = TexturePackManager.getInstance().getSelectedIndex(TextureSlots.GUI_SKIN);
        cfg.set(key("gui_skin_index"), guiSkinIndex);
    }

    @Override
    public void rename(String newName) {
        name = newName;
        ConfigManager.getInstance().set(key("name"), newName);
    }

    @Override public String getName() { return name; }
    @Override public String getDescription() { return "Your saved custom visual setup."; }
    @Override public ProfileCategory getCategory() { return ProfileCategory.VISUAL; }

    @Override
    public void apply() {
        for (Module module : ModuleRegistry.getInstance().getByCategory(ModuleCategory.QUALITY_OF_LIFE)) {
            if (!module.isAlwaysOn()) {
                module.setEnabled(moduleEnabled.getOrDefault(module.getId(), module.isEnabled()));
            }
        }
        CrosshairModule.setStyleIndex(crosshairStyleIndex);
        TexturePackManager.getInstance().applyIndex(TextureSlots.GUI_SKIN, guiSkinIndex);
        TexturePackManager.getInstance().reload();
    }

    @Override
    public boolean matchesCurrentState() {
        for (Module module : ModuleRegistry.getInstance().getByCategory(ModuleCategory.QUALITY_OF_LIFE)) {
            boolean expected = moduleEnabled.getOrDefault(module.getId(), module.isEnabled());
            if (module.isEnabled() != expected) {
                return false;
            }
        }
        if (CrosshairModule.styleIndex != crosshairStyleIndex) {
            return false;
        }
        return TexturePackManager.getInstance().getSelectedIndex(TextureSlots.GUI_SKIN) == guiSkinIndex;
    }
}
