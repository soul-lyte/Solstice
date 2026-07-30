package com.example.solstice.core.module;

import com.example.solstice.SolsticeMod;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Central registry for all Solstice modules.
 *
 * <p>Modules are registered by their respective initializers:
 * <ul>
 *   <li>Common modules (memory, chunk, network, startup) are registered
 *       in {@link com.example.solstice.SolsticeMod#onInitialize()}.</li>
 *   <li>Client-only modules (render, entity culling, particle limiter, UI)
 *       are registered in {@code SolsticeClient.onInitializeClient()}.</li>
 * </ul>
 *
 * <p>The registry stores only {@link Module} interface references - no
 * client-only types - so it is safe in the common source set.</p>
 */
public final class ModuleRegistry {

    private static final ModuleRegistry INSTANCE = new ModuleRegistry();

    /** Insertion-ordered so the settings UI renders modules in declaration order. */
    private final LinkedHashMap<String, Module> modules = new LinkedHashMap<>();

    private ModuleRegistry() {}

    public static ModuleRegistry getInstance() {
        return INSTANCE;
    }

    public void register(Module module) {
        if (modules.containsKey(module.getId())) {
            SolsticeMod.LOGGER.warn("[Solstice] Duplicate module ID '{}' - skipping.", module.getId());
            return;
        }
        modules.put(module.getId(), module);
        SolsticeMod.LOGGER.debug("[Solstice] Registered module: {}", module.getDisplayName());
    }

    public Optional<Module> getById(String id) {
        return Optional.ofNullable(modules.get(id));
    }

    public Collection<Module> getAllModules() {
        return Collections.unmodifiableCollection(modules.values());
    }

    public List<Module> getByCategory(ModuleCategory category) {
        return modules.values().stream()
                .filter(m -> m.getCategory() == category)
                .collect(Collectors.toList());
    }

    public int getModuleCount() {
        return modules.size();
    }
}
