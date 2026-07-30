package com.example.solstice.textures;

/**
 * One choice within a {@link TextureSlot} - either vanilla ({@code packId == null},
 * nothing to enable/disable) or a specific bundled resource pack registered under
 * {@code src/main/resources/resourcepacks/<packId>/}.
 */
public record TextureOption(String id, String displayName, String packId) {

    public boolean isVanilla() {
        return packId == null;
    }
}
