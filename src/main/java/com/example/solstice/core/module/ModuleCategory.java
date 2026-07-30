package com.example.solstice.core.module;

/**
 * Top-level grouping for Solstice modules, used in the settings UI. Enum
 * declaration order is the tab order shown in {@code SolsticeScreen}.
 */
public enum ModuleCategory {
    PROFILES("Profiles", "Bulk-apply visual and performance presets"),
    QUALITY_OF_LIFE("Quality of Life", "Optional features and conveniences"),
    TEXTURES("Textures", "Swap tools, GUI, utility, and font textures"),
    ADVANCED("Advanced", "Performance optimizations and everything else");

    private final String displayName;
    private final String description;

    ModuleCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
