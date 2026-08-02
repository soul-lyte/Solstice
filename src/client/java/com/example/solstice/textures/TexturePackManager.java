package com.example.solstice.textures;

import com.example.solstice.SolsticeMod;
import com.example.solstice.core.config.ConfigManager;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Registers every non-vanilla {@link TextureOption} across all {@link TextureSlot}s
 * (plus any other standalone bundled pack, e.g. Visuals' shield-position pack) as a
 * Fabric builtin resource pack, and handles enabling/disabling them.
 *
 * <p>A builtin pack registered under {@code Identifier.of("solstice", packId)} ends
 * up known to {@link ResourcePackManager} under the id {@code "solstice:" + packId}
 * (its {@code Identifier.toString()}) - confirmed directly from
 * {@code ResourceLoaderImpl}'s real source, not guessed.</p>
 *
 * <p>Changing a pack's enabled state and reloading are deliberately separate calls -
 * {@link com.example.solstice.ui.TextureCategoryScreen}'s "Done" button applies every
 * pending change across several slots and reloads exactly once, rather than once per
 * slot. Cycling the preview with the left/right arrows never calls anything here at
 * all - see {@link TexturePreviewLoader}.</p>
 */
public final class TexturePackManager {

    private static final TexturePackManager INSTANCE = new TexturePackManager();

    private TexturePackManager() {}

    public static TexturePackManager getInstance() { return INSTANCE; }

    /** Call once during client init, before the first resource reload. */
    public void registerBuiltinPacks(List<TextureSlot> slots) {
        for (TextureSlot slot : slots) {
            for (TextureOption option : slot.getOptions()) {
                if (!option.isVanilla()) registerBuiltinPack(option.packId());
            }
        }
    }

    /** Registers one bundled pack (under {@code src/main/resources/resourcepacks/<packId>/}) as a builtin pack. */
    public void registerBuiltinPack(String packId) {
        ModContainer container = FabricLoader.getInstance().getModContainer(SolsticeMod.MOD_ID)
                .orElseThrow(() -> new IllegalStateException("Solstice mod container not found"));
        ResourceLoader.registerBuiltinPack(Identifier.of(SolsticeMod.MOD_ID, packId), container, PackActivationType.NORMAL);
    }

    /**
     * Re-enables whatever was persisted for each slot. Called from
     * {@code ClientLifecycleEvents.CLIENT_STARTED} - deliberately does NOT
     * reload itself. See {@code SolsticeClient}'s CLIENT_STARTED handler:
     * every persisted-selection source (this, {@code VisualsModule}) applies
     * its enable/disable calls first, then a single {@link #reloadBlocking()}
     * covers all of them together - previously each source reloaded on its
     * own, so up to two separate async reloads could run back to back,
     * which is the real cause behind "the texture pack loads twice."
     *
     * @return true if any non-vanilla selection was actually restored (i.e. a reload is needed)
     */
    public boolean applyPersistedSelections(List<TextureSlot> slots) {
        boolean anyEnabled = false;
        for (TextureSlot slot : DynamicTextureRegistry.getInstance().mergeSlots(slots)) {
            int index = getSelectedIndex(slot);
            if (index >= slot.getOptions().size()) continue;
            TextureOption option = slot.getOptions().get(index);
            if (!option.isVanilla()) {
                setPackEnabled(option.packId(), true);
                anyEnabled = true;
            }
        }
        return anyEnabled;
    }

    public int getSelectedIndex(TextureSlot slot) {
        int stored = ConfigManager.getInstance().getInt("textures." + slot.getId(), slot.getDefaultIndex());
        return stored >= 0 && stored < slot.getOptions().size() ? stored : slot.getDefaultIndex();
    }

    /**
     * The option this slot's category is <em>actually</em> showing right
     * now, determined fresh from the real {@link ResourcePackManager}'s
     * enabled-pack state instead of trusting the persisted {@code
     * "textures.&lt;slot&gt;"} config value {@link #getSelectedIndex} reads.
     *
     * <p>The two can genuinely disagree - real bug, not hypothetical:
     * selecting a whole Preset (see {@code TexturePresetManager#select})
     * only enables/disables Preset packs, it never touches an
     * independently-set Advanced-row category's own persisted index at
     * all. So after switching Presets, the Advanced row's card still read
     * back whatever was last explicitly chosen there even though the
     * screen was no longer showing it - stale by construction, not just
     * occasionally out of sync. Deriving live avoids that class of bug
     * entirely, for this cause and any other future one: whatever pack the
     * manager actually reports as enabled for one of this slot's own
     * non-vanilla options wins, full stop. Falls back to the slot's own
     * vanilla option (or {@link #getSelectedIndex} as a last resort, if a
     * slot somehow has no vanilla option at all) when none of them are
     * currently enabled.</p>
     *
     * <p>Used for display (so a card never shows a choice that isn't
     * really active) and by {@link #applyIndex} to know what to actually
     * disable - see that method's own note.</p>
     */
    public int getLiveSelectedIndex(TextureSlot slot) {
        ResourcePackManager manager = MinecraftClient.getInstance().getResourcePackManager();
        java.util.Collection<String> enabledIds = manager.getEnabledIds();
        List<TextureOption> options = slot.getOptions();

        for (int i = 0; i < options.size(); i++) {
            TextureOption option = options.get(i);
            if (!option.isVanilla() && enabledIds.contains(resolveProfileId(option))) {
                return i;
            }
        }
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).isVanilla()) return i;
        }
        return getSelectedIndex(slot);
    }

    /** Mirrors {@link #setPackEnabled}'s own packId-to-real-profile-id mapping, just without the side effect. */
    private String resolveProfileId(TextureOption option) {
        DynamicTextureRegistry.DynamicSource source = DynamicTextureRegistry.getInstance().lookup(option.packId());
        if (source != null) {
            return "file/" + DynamicTextureRegistry.DYNAMIC_PACK_PREFIX + option.packId();
        }
        return SolsticeMod.MOD_ID + ":" + option.packId();
    }

    /**
     * Applies {@code newIndex} for this slot - disables the old pack, enables the new
     * one, persists the choice - but does <em>not</em> reload. Caller is responsible
     * for calling {@link #reload()} once after applying every slot it's changing.
     *
     * <p>Disables whatever {@link #getLiveSelectedIndex} says is really
     * active, not {@link #getSelectedIndex}'s persisted value - so this can
     * never leave a stale-but-still-enabled dynamic pack behind just
     * because the persisted index didn't match what was genuinely on
     * screen (see that method's own Javadoc for a real scenario where the
     * two disagree).</p>
     */
    public void applyIndex(TextureSlot slot, int newIndex) {
        List<TextureOption> options = slot.getOptions();
        int clamped = ((newIndex % options.size()) + options.size()) % options.size();
        int oldIndex = getLiveSelectedIndex(slot);
        if (clamped != oldIndex) {
            TextureOption oldOption = options.get(oldIndex);
            TextureOption newOption = options.get(clamped);

            if (!oldOption.isVanilla()) setPackEnabled(oldOption.packId(), false);
            if (!newOption.isVanilla()) setPackEnabled(newOption.packId(), true);
        }

        ConfigManager.getInstance().set("textures." + slot.getId(), clamped);
    }

    public void setPackEnabled(String packId, boolean enabled) {
        if (DynamicTextureRegistry.getInstance().lookup(packId) != null) {
            DynamicTextureRegistry.getInstance().applyDynamicOption(packId, enabled);
            return;
        }
        ResourcePackManager manager = MinecraftClient.getInstance().getResourcePackManager();
        String profileId = SolsticeMod.MOD_ID + ":" + packId;
        if (enabled) {
            manager.enable(profileId);
        } else {
            manager.disable(profileId);
        }
    }

    public void reload() {
        MinecraftClient.getInstance().reloadResources();
    }
}
