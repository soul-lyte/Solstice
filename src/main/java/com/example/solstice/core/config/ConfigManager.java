package com.example.solstice.core.config;

import com.example.solstice.SolsticeMod;
import com.google.gson.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Solstice configuration manager.
 *
 * <p>All config is stored as a single JSON file at
 * {@code .minecraft/config/solstice.json}. The file is written atomically
 * (write to temp, then rename) to avoid corruption on crash.</p>
 *
 * <p>Structure:
 * <pre>
 * {
 *   "modules": {
 *     "render":          true,
 *     "entity_culling":  true,
 *     ...
 *   },
 *   "settings": {
 *     "entity_culling.interval_ms": 50,
 *     ...
 *   }
 * }
 * </pre>
 * </p>
 */
public final class ConfigManager {

    private static final ConfigManager INSTANCE = new ConfigManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config", "solstice.json");

    private final Map<String, Boolean> moduleStates = new HashMap<>();
    private final Map<String, Object> settings = new HashMap<>();
    private boolean dirty = false;

    private ConfigManager() {}

    public static ConfigManager getInstance() {
        return INSTANCE;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public void load() {
        if (!Files.exists(CONFIG_PATH)) {
            SolsticeMod.LOGGER.info("[Solstice] No config file found, using defaults.");
            return;
        }
        try (Reader reader = new InputStreamReader(
                Files.newInputStream(CONFIG_PATH), StandardCharsets.UTF_8)) {

            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            if (root.has("modules")) {
                JsonObject modules = root.getAsJsonObject("modules");
                for (Map.Entry<String, JsonElement> entry : modules.entrySet()) {
                    moduleStates.put(entry.getKey(), entry.getValue().getAsBoolean());
                }
            }

            if (root.has("settings")) {
                JsonObject s = root.getAsJsonObject("settings");
                for (Map.Entry<String, JsonElement> entry : s.entrySet()) {
                    JsonPrimitive p = entry.getValue().getAsJsonPrimitive();
                    if (p.isBoolean()) settings.put(entry.getKey(), p.getAsBoolean());
                    else if (p.isNumber()) settings.put(entry.getKey(), p.getAsDouble());
                    else settings.put(entry.getKey(), p.getAsString());
                }
            }

            SolsticeMod.LOGGER.info("[Solstice] Config loaded from {}.", CONFIG_PATH);
        } catch (Exception e) {
            SolsticeMod.LOGGER.error("[Solstice] Failed to load config: {}", e.getMessage());
        }
    }

    public void save() {
        if (!dirty) return;
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Path tmp = CONFIG_PATH.resolveSibling("solstice.json.tmp");

            JsonObject root = new JsonObject();

            JsonObject modules = new JsonObject();
            moduleStates.forEach(modules::addProperty);
            root.add("modules", modules);

            JsonObject s = new JsonObject();
            settings.forEach((k, v) -> {
                if (v instanceof Boolean b) s.addProperty(k, b);
                else if (v instanceof Number n) s.addProperty(k, n);
                else s.addProperty(k, v.toString());
            });
            root.add("settings", s);

            try (Writer writer = new OutputStreamWriter(
                    Files.newOutputStream(tmp), StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }

            Files.move(tmp, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            dirty = false;
            SolsticeMod.LOGGER.debug("[Solstice] Config saved.");
        } catch (Exception e) {
            SolsticeMod.LOGGER.error("[Solstice] Failed to save config: {}", e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Module state
    // -------------------------------------------------------------------------

    public boolean isModuleEnabled(String moduleId, boolean defaultValue) {
        return moduleStates.getOrDefault(moduleId, defaultValue);
    }

    public void setModuleEnabled(String moduleId, boolean enabled) {
        moduleStates.put(moduleId, enabled);
        dirty = true;
        save();
    }

    // -------------------------------------------------------------------------
    // Generic settings
    // -------------------------------------------------------------------------

    public int getInt(String key, int defaultValue) {
        Object v = settings.get(key);
        return v instanceof Number n ? n.intValue() : defaultValue;
    }

    public double getDouble(String key, double defaultValue) {
        Object v = settings.get(key);
        return v instanceof Number n ? n.doubleValue() : defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object v = settings.get(key);
        return v instanceof Boolean b ? b : defaultValue;
    }

    public String getString(String key, String defaultValue) {
        Object v = settings.get(key);
        return v != null ? v.toString() : defaultValue;
    }

    public void set(String key, Object value) {
        settings.put(key, value);
        dirty = true;
        save();
    }
}
