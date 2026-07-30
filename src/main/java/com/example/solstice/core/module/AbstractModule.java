package com.example.solstice.core.module;

import com.example.solstice.SolsticeMod;
import com.example.solstice.core.config.ConfigManager;

/**
 * Base class for all Solstice modules.
 *
 * <p>Handles enabled-state persistence through {@link ConfigManager} and
 * provides lifecycle scaffolding so concrete modules only override what
 * they need. All state changes are logged at DEBUG level.</p>
 */
public abstract class AbstractModule implements Module {

    private boolean enabled;
    private boolean initialized;

    protected AbstractModule() {
        // Restore enabled-state from config (defaults to true if not present)
        this.enabled = ConfigManager.getInstance().isModuleEnabled(getId(), defaultEnabled());
    }

    /**
     * Override to return {@code false} for modules that should ship disabled
     * until the user explicitly opts in.
     */
    protected boolean defaultEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;

        this.enabled = enabled;
        ConfigManager.getInstance().setModuleEnabled(getId(), enabled);

        if (enabled) {
            SolsticeMod.LOGGER.debug("[Solstice] Module enabled: {}", getDisplayName());
            onEnable();
        } else {
            SolsticeMod.LOGGER.debug("[Solstice] Module disabled: {}", getDisplayName());
            onDisable();
        }
    }

    @Override
    public final void onClientInit() {
        if (initialized) return;
        initialized = true;
        SolsticeMod.LOGGER.debug("[Solstice] Initializing module: {} (enabled={})",
                getDisplayName(), enabled);
        init();
    }

    /**
     * Concrete modules override this instead of {@link #onClientInit()}.
     * Called exactly once, regardless of enabled state, so that Mixin
     * access can be wired up unconditionally and the enabled flag can be
     * checked at call sites.
     */
    protected abstract void init();
}
