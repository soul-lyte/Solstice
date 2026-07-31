package com.example.solstice.viewdistance;

import com.example.solstice.core.config.ConfigManager;
import com.example.solstice.core.module.AbstractModule;
import com.example.solstice.core.module.ModuleCategory;
import com.example.solstice.core.module.ModuleSetting;

import java.util.List;

/**
 * ViewDistanceModule - rebuilt on Johni0702/bobby's real architecture
 * (LGPL-3.0-only, see NOTICE.md), replacing this project's own earlier
 * from-scratch retention attempt entirely. Chunks the player has already
 * explored are cached (in memory, and on disk under {@code
 * .solstice-chunkcache/} in the run directory) and redrawn as "fake" chunks
 * when they fall outside the server's real view distance, instead of
 * popping out the instant they unload.
 *
 * <p><b>Two independent, additive sliders</b> (both on Video Settings, right
 * next to each other): <b>Render Distance</b> is the real radius - it's
 * vanilla's own render-distance option, unchanged in meaning, sent to real
 * servers exactly as before and used by the singleplayer integrated server
 * to decide how far to actually generate/send real chunk data (nothing new
 * needed there - {@code IntegratedServer} already syncs its own tracking
 * distance to this option on its own). <b>Retention Distance</b> is a purely
 * additive number of extra rings beyond that, filled with cached/retained
 * data where available. {@link
 * com.example.solstice.mixin.viewdistance.GameOptionsMixin} makes {@code
 * GameOptions.getClampedViewDistance()} - the one method fog/far-plane/
 * render-storage sizing all read from - return {@code renderDistance +
 * retentionDistance}, so raising either slider does exactly what its name
 * says and nothing else. (The old design used a single Render Distance
 * slider as both "how far real data extends" AND "the total visual cap,"
 * with a second, separately-scoped "Singleplayer Real Chunk Radius" setting
 * that only affected singleplayer - genuinely confusing since the two
 * numbers didn't compose the way their names implied. This replaces both
 * with one clear additive relationship that also works identically in
 * multiplayer, not just singleplayer.)</p>
 *
 * <p><b>Known limitation, same as upstream Bobby</b>: the retention system
 * only attaches itself when a world's {@code ClientChunkManager} is first
 * constructed (world join). Toggling this module while already in a world
 * takes effect on the next rejoin, not immediately.</p>
 */
public final class ViewDistanceModule extends AbstractModule {

    private static final ViewDistanceModule INSTANCE = new ViewDistanceModule();

    private int unloadDelaySecs = 30;
    private boolean taintFakeChunks = false;
    private boolean skipBlockEntities = true;
    private int retentionDistance = 8;

    private ViewDistanceModule() {}

    public static ViewDistanceModule getInstance() { return INSTANCE; }

    @Override public String getId()          { return "view_distance"; }
    @Override public String getDisplayName() { return "Chunk Retention"; }
    @Override public String getDescription() { return "Keeps terrain you've already visited on screen instead of it popping out immediately. How far it extends beyond your real Render Distance is controlled by the Retention Distance slider in Video Settings."; }

    @Override
    public List<String> getSearchKeywords() {
        return List.of("voxy", "bobby", "distant horizons", "chunk retention", "fake chunks", "chunk cache");
    }
    @Override public ModuleCategory getCategory() { return ModuleCategory.ADVANCED; }
    @Override protected boolean defaultEnabled() { return false; }

    @Override
    public List<ModuleSetting> getSettings() {
        return List.of(
                new ModuleSetting.IntSetting(
                        "Unload Delay (seconds)",
                        "How long a chunk stays cached in memory after moving out of range before it's dropped (still recoverable from disk afterwards). Higher values smooth out back-and-forth movement at the edge of your view distance.",
                        0, 300,
                        () -> unloadDelaySecs,
                        this::setUnloadDelaySecs),
                new ModuleSetting.BooleanSetting(
                        "Skip Block Entities",
                        "Don't load chests/signs/etc. inside cached chunks. Faster and lighter on memory - the block geometry itself is unaffected.",
                        () -> skipBlockEntities,
                        this::setSkipBlockEntities),
                new ModuleSetting.BooleanSetting(
                        "Taint Fake Chunks",
                        "Debug aid: dims the lighting on cached chunks so you can visually tell them apart from real terrain.",
                        () -> taintFakeChunks,
                        this::setTaintFakeChunks)
        );
    }

    @Override
    protected void init() {
        unloadDelaySecs = ConfigManager.getInstance().getInt("view_distance.unload_delay_secs", 30);
        skipBlockEntities = ConfigManager.getInstance().getBoolean("view_distance.skip_block_entities", true);
        taintFakeChunks = ConfigManager.getInstance().getBoolean("view_distance.taint_fake_chunks", false);
        retentionDistance = ConfigManager.getInstance().getInt("view_distance.retention_distance", 8);
    }

    public int getUnloadDelaySecs() { return unloadDelaySecs; }

    public void setUnloadDelaySecs(int value) {
        unloadDelaySecs = value;
        ConfigManager.getInstance().set("view_distance.unload_delay_secs", value);
    }

    public boolean isSkipBlockEntities() { return skipBlockEntities; }

    public void setSkipBlockEntities(boolean value) {
        skipBlockEntities = value;
        ConfigManager.getInstance().set("view_distance.skip_block_entities", value);
    }

    public boolean isTaintFakeChunks() { return taintFakeChunks; }

    public void setTaintFakeChunks(boolean value) {
        taintFakeChunks = value;
        ConfigManager.getInstance().set("view_distance.taint_fake_chunks", value);
    }

    public int getRetentionDistance() { return retentionDistance; }

    public void setRetentionDistance(int value) {
        retentionDistance = value;
        ConfigManager.getInstance().set("view_distance.retention_distance", value);
    }
}
