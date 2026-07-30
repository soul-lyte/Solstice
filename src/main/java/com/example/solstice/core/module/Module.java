package com.example.solstice.core.module;

/**
 * Core contract every Solstice performance module must satisfy.
 *
 * <p>A module is a self-contained unit of optimization. It owns its own
 * configuration slice, lifecycle hooks, and documentation metadata.
 * The framework never assumes a module is client-only: common-side
 * modules (e.g. networking) must also implement this interface.</p>
 */
public interface Module {

    /** Short internal identifier, e.g. "entity_culling". No spaces. */
    String getId();

    /** Human-readable name shown in the settings screen. */
    String getDisplayName();

    /** One-sentence description shown in the settings tooltip. */
    String getDescription();

    /** Category used to group this module in the settings UI. */
    ModuleCategory getCategory();

    /**
     * Whether this module is active. Disabled modules must not alter
     * any game state, issue any Mixin side-effects, or add overhead.
     */
    boolean isEnabled();

    /** Enable or disable this module at runtime. */
    void setEnabled(boolean enabled);

    /**
     * Called once during client initialization, after config is loaded.
     * Modules should register event listeners and Mixin hooks here.
     * Must be idempotent (safe to call if module is disabled).
     */
    void onClientInit();

    /**
     * Called each client tick while the module is enabled.
     * Keep work minimal - this runs on the main thread.
     */
    default void onTick() {}

    /** Called when the module is enabled at runtime (e.g. via settings UI). */
    default void onEnable() {}

    /** Called when the module is disabled at runtime. */
    default void onDisable() {}

    /**
     * Whether this module is permanently active and cannot be toggled off
     * from the settings UI. Always-on modules still report {@link #isEnabled()}
     * as {@code true} and ignore {@link #setEnabled(boolean)}.
     */
    default boolean isAlwaysOn() {
        return false;
    }

    /**
     * Adjustable controls shown on this module's settings screen, in display
     * order. Empty by default - override to expose tunable fields.
     */
    default java.util.List<ModuleSetting> getSettings() {
        return java.util.List.of();
    }

    /**
     * Extra search terms this module should match on, beyond its own name
     * and description - e.g. the name of the mod it was inspired by, or a
     * common alias a player might type instead of Solstice's own naming.
     * Empty by default.
     */
    default java.util.List<String> getSearchKeywords() {
        return java.util.List.of();
    }

    /**
     * Whether this module should be skipped by {@code VisualPvpProfile}'s
     * blanket "turn every QOL module on" loop, for modules that are
     * explicitly never meant to be on by default anywhere (e.g. Chat Heads,
     * Locator Heads) rather than merely off-by-default-but-PVP-enables-it.
     * False by default - most modules are fine being swept up by PVP.
     */
    default boolean excludeFromPvpProfile() {
        return false;
    }
}
