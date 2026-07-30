package com.example.solstice.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Solstice's own UI click sound - played by {@link com.example.solstice.ui.widget.SolsticeButton}
 * instead of vanilla's default menu click, and when the main {@link SolsticeScreen} opens.
 */
public final class SolsticeSounds {

    public static final SoundEvent CLICK = register("click");

    private SolsticeSounds() {}

    private static SoundEvent register(String path) {
        Identifier id = Identifier.of("solstice", path);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    /** Call once during client init so the registration above actually runs. */
    public static void init() {}

    public static void playClick() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.getSoundManager().play(PositionedSoundInstance.ui(CLICK, 1.0f));
    }
}
