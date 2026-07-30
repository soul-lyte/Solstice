package com.example.solstice.profiles;

/** Implemented by profiles the user can rename - saved Custom profiles, not the fixed built-ins. */
public interface Renameable {
    String getName();
    void rename(String newName);
}
