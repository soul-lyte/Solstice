package com.example.solstice.ui.widget;

import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;

/**
 * Shared base for every custom-clickable Solstice widget - suppresses
 * vanilla's own default UI click sound so it never stacks with Solstice's
 * own ({@code SolsticeSounds.playClick()}, called explicitly from each
 * subclass's own {@code mouseClicked} override, same as {@link SolsticeButton}
 * already did before this class existed).
 *
 * <p>Only {@code ClickableWidget} subclasses need this - {@link
 * SolsticeSliderWidget} extends vanilla's {@code SliderWidget} instead (a
 * continuous-drag widget with its own click machinery) and handles its own
 * sound override directly.</p>
 */
public abstract class SolsticeClickableWidget extends ClickableWidget {

    protected SolsticeClickableWidget(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        // Suppressed - see class Javadoc.
    }
}
