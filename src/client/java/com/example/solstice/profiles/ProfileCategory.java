package com.example.solstice.profiles;

/** The two independent profile rows shown in the Profiles tab. */
public enum ProfileCategory {
    VISUAL("Visual Profiles"),
    PERFORMANCE("Performance Profiles");

    private final String rowLabel;

    ProfileCategory(String rowLabel) {
        this.rowLabel = rowLabel;
    }

    public String getRowLabel() {
        return rowLabel;
    }
}
