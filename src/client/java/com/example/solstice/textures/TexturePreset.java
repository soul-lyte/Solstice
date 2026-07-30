package com.example.solstice.textures;

/**
 * One whole-pack choice in the Textures tab's Presets row - either vanilla
 * ({@code packId == null}) or a real resource pack profile id, ready to
 * hand straight to {@code ResourcePackManager.enable/disable(String)}.
 * Covers three real sources: Solstice's own bundled preset packs
 * ({@code "solstice:preset_<name>"}, registered as builtin packs), a pack
 * added via the "Add your own" file picker ({@code "file/<filename>"},
 * vanilla's own convention for packs copied into the real resourcepacks
 * folder), and saved custom combos (see {@link TexturePresetManager}).
 */
public record TexturePreset(String id, String displayName, String packId, String credit) {

    public boolean isVanilla() {
        return packId == null;
    }
}
