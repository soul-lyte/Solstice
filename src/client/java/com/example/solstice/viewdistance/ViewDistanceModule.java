package com.example.solstice.viewdistance;

import com.example.solstice.core.config.ConfigManager;
import com.example.solstice.core.module.AbstractModule;
import com.example.solstice.core.module.ModuleCategory;
import com.example.solstice.core.module.ModuleSetting;
import net.minecraft.client.MinecraftClient;

import java.util.List;

/**
 * ViewDistanceModule - keeps already-explored chunks visible a bit past what
 * the server actually sends, similar to Voxy: no new data, no entities, no
 * disk persistence, just the last-known terrain staying on screen instead of
 * popping out immediately. See {@code FakeChunkManager}/{@code
 * ClientChunkManagerMixin} for the actual mechanism.
 *
 * <p>How far to extend ("Extra Chunks") is configured from the new Solstice
 * Video Settings screen, alongside vanilla's own render distance, not from
 * here - this module is just the on/off switch.</p>
 *
 * <p>Off by default given the real risk profile of directly touching chunk
 * loading - this is the newest, least-precedented part of Solstice.</p>
 */
public final class ViewDistanceModule extends AbstractModule {

    private static final ViewDistanceModule INSTANCE = new ViewDistanceModule();

    /** How many chunks beyond what the server sends to keep visible. Set from the Video Settings screen. */
    public static int extraChunks = 4;

    private ViewDistanceModule() {}

    public static ViewDistanceModule getInstance() { return INSTANCE; }

    @Override public String getId()          { return "view_distance"; }
    @Override public String getDisplayName() { return "Extended View Distance"; }
    @Override public String getDescription() { return "Keeps already-explored chunks visible a bit past what the server sends, instead of them popping out immediately."; }

    @Override
    public java.util.List<String> getSearchKeywords() {
        return java.util.List.of("voxy", "bobby", "distant horizons", "extra chunks", "chunk retention", "fake chunks");
    }
    @Override public ModuleCategory getCategory() { return ModuleCategory.ADVANCED; }
    @Override protected boolean defaultEnabled() { return false; }

    @Override
    public List<ModuleSetting> getSettings() {
        return List.of(
                new ModuleSetting.IntSetting(
                        "Extra Chunks",
                        "How many extra chunks beyond your render distance to keep visible once already explored, instead of them popping out immediately. Also editable from the Video Settings screen.",
                        0, 16,
                        () -> extraChunks,
                        this::setExtraChunks)
        );
    }

    @Override
    protected void init() {
        extraChunks = ConfigManager.getInstance().getInt("view_distance.extra_chunks", 4);
    }

    /** 0 turns the module off; 1+ turns it on - the slider doubles as the on/off switch. */
    public void setExtraChunks(int value) {
        extraChunks = value;
        ConfigManager.getInstance().set("view_distance.extra_chunks", value);
        setEnabled(value > 0);
        reloadRenderStorage();
    }

    @Override
    public void onEnable() { reloadRenderStorage(); }

    @Override
    public void onDisable() { reloadRenderStorage(); }

    /**
     * {@code BuiltChunkStorageMixin} only re-reads {@code extraChunks} the next time
     * {@code BuiltChunkStorage} is (re)constructed, which only happens from
     * {@code WorldRenderer.reload()} - normally triggered by vanilla's own render
     * distance slider, world join, etc. Without an explicit call here, toggling this
     * module or dragging "Extra Chunks" wouldn't resize the render storage until the
     * next unrelated reload (or a world rejoin), even though the setting itself
     * already took effect - so trigger it directly, same as vanilla's own slider does.
     */
    private void reloadRenderStorage() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            client.worldRenderer.reload();
        }
    }
}
