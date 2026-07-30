package com.example.solstice.ui;

import net.minecraft.client.gui.screen.Screen;

/**
 * Implemented by modules whose settings don't fit the standard
 * {@link com.example.solstice.core.module.ModuleSetting} row list on
 * {@link ModuleDetailScreen} - instead they open their own dedicated screen.
 * {@link com.example.solstice.ui.widget.ModuleCardWidget} draws a small
 * "Edit" button on the card for any module implementing this, matching the
 * per-widget "Edit" button already used by {@link HudEditorScreen}.
 */
public interface EditableModule {

    /** Builds the screen to open when the card's "Edit" button is clicked. */
    Screen createEditScreen(Screen parent);
}
