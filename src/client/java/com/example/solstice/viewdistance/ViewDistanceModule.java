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
 * <p>Unlike the old implementation, there is no separate "Extra Chunks"
 * number. {@link com.example.solstice.mixin.viewdistance.GameOptionsMixin}
 * makes {@code GameOptions.getClampedViewDistance()} return the client's own
 * raw render-distance option instead of clamping it to the server's real
 * radius - so raising the existing (already-uncapped) Render Distance
 * slider in Video Settings directly controls how far retained chunks
 * extend, with fog/far-plane/render-storage sizing all automatically
 * agreeing since they all read from that same one method.</p>
 *
 * <p>In singleplayer, {@link
 * com.example.solstice.mixin.viewdistance.IntegratedServerMixin} keeps the
 * embedded server's own real chunk-sending radius fixed at {@link
 * #serverViewDistanceOverwrite} regardless of how high the client's render
 * distance is raised - otherwise the server would just send real data out to
 * the same inflated distance, leaving nothing for retention to fill in.</p>
 *
 * <p><b>Known limitation, same as upstream Bobby</b>: the retention system
 * only attaches itself when a world's {@code ClientChunkManager} is first
 * constructed (world join). Toggling this module while already in a world
 * takes effect on the next rejoin, not immediately.</p>
 */
public final class ViewDistanceModule extends AbstractModule {

    private static final ViewDistanceModule INSTANCE = new ViewDistanceModule();

    /** Fixed real chunk-sending radius for the singleplayer integrated server. 0 = no override. */
    public static int serverViewDistanceOverwrite = 8;

    private int unloadDelaySecs = 30;
    private boolean taintFakeChunks = false;
    private boolean skipBlockEntities = true;

    private ViewDistanceModule() {}

    public static ViewDistanceModule getInstance() { return INSTANCE; }

    @Override public String getId()          { return "view_distance"; }
    @Override public String getDisplayName() { return "Chunk Retention"; }
    @Override public String getDescription() { return "Keeps terrain you've already visited on screen instead of it popping out immediately. How far it extends is controlled by the Render Distance slider in Video Settings, not a separate setting here."; }

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
                        this::setTaintFakeChunks),
                new ModuleSetting.IntSetting(
                        "Singleplayer Real Chunk Radius",
                        "How far ahead of you, into terrain you haven't visited yet, the singleplayer world actually generates and sends real data - independent of your (uncapped) Render Distance slider. Raising this closes the gap between unexplored terrain ahead of you and already-explored terrain behind you (which can always extend to the full Render Distance for free from cache), at the cost of real chunk generation load. Setting it equal to Render Distance removes the gap entirely.",
                        2, 64,
                        () -> serverViewDistanceOverwrite,
                        this::setServerViewDistanceOverwrite)
        );
    }

    @Override
    protected void init() {
        unloadDelaySecs = ConfigManager.getInstance().getInt("view_distance.unload_delay_secs", 30);
        skipBlockEntities = ConfigManager.getInstance().getBoolean("view_distance.skip_block_entities", true);
        taintFakeChunks = ConfigManager.getInstance().getBoolean("view_distance.taint_fake_chunks", false);
        serverViewDistanceOverwrite = ConfigManager.getInstance().getInt("view_distance.server_view_distance", 8);
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

    public void setServerViewDistanceOverwrite(int value) {
        serverViewDistanceOverwrite = value;
        ConfigManager.getInstance().set("view_distance.server_view_distance", value);
    }
}
