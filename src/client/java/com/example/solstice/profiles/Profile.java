package com.example.solstice.profiles;

/** One bulk-applyable preset shown as a card in the Profiles tab. */
public interface Profile {
    String getName();
    String getDescription();
    ProfileCategory getCategory();
    void apply();

    /** Whether the live, current settings already equal what {@link #apply()} would set. */
    boolean matchesCurrentState();
}
