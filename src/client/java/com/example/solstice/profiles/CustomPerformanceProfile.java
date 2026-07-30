package com.example.solstice.profiles;

import com.example.solstice.core.config.ConfigManager;
import com.example.solstice.performance.memory.MemoryModule;
import com.example.solstice.performance.network.NetworkModule;
import com.example.solstice.performance.render.EntityCullingModule;
import com.example.solstice.performance.render.ParticleLimiterModule;
import com.example.solstice.performance.render.RenderModule;
import com.example.solstice.viewdistance.ViewDistanceModule;

/**
 * One user-saveable "Custom" slot for the Performance profile row. Captures
 * the same fields {@link PerformanceBalancedProfile} (and Lite/Aggressive)
 * touch at the moment it's saved (via {@link #captureAndSave()}).
 *
 * <p>Keyed by an {@code index} rather than being a singleton - {@link
 * ProfileManager} keeps a list of these (however many the user has saved),
 * each with its own persisted key prefix ({@code
 * profiles.performance.custom.<index>.*}), so saving a new custom profile
 * no longer overwrites the previous one. Default name is "Custom N"
 * ({@code index + 1}), matching the order they were saved in.</p>
 */
public final class CustomPerformanceProfile implements Profile, Renameable {

    private final int index;

    private String name;
    private int minimizedSleepMs;
    private boolean rdEnabled;
    private int rdFpsThreshold;
    private int rdGraceFrames;
    private int extraChunks;
    private boolean vdEnabled;
    private double gcThreshold;
    private long gcIntervalMs;
    private int maxParticles;
    private int particleFpsThreshold;
    private int cullingIntervalMs;
    private int maxRenderDistanceBlocks;
    private int sendBufferBytes;
    private int receiveBufferBytes;
    private boolean tcpNoDelay;

    public CustomPerformanceProfile(int index) {
        this.index = index;
        load();
    }

    private String key(String suffix) {
        return "profiles.performance.custom." + index + "." + suffix;
    }

    private void load() {
        ConfigManager cfg = ConfigManager.getInstance();
        name = cfg.getString(key("name"), "Custom " + (index + 1));
        minimizedSleepMs = cfg.getInt(key("minimized_sleep_ms"), RenderModule.minimizedSleepMs);
        rdEnabled = cfg.getBoolean(key("rd_enabled"), RenderModule.dynamicRenderDistanceEnabled);
        rdFpsThreshold = cfg.getInt(key("rd_fps_threshold"), RenderModule.dynamicRenderDistanceFpsThreshold);
        rdGraceFrames = cfg.getInt(key("rd_grace_frames"), RenderModule.dynamicRenderDistanceGraceFrames);
        extraChunks = cfg.getInt(key("extra_chunks"), ViewDistanceModule.extraChunks);
        vdEnabled = cfg.getBoolean(key("vd_enabled"), ViewDistanceModule.getInstance().isEnabled());
        gcThreshold = cfg.getDouble(key("gc_threshold"), MemoryModule.gcHintThreshold);
        gcIntervalMs = cfg.getInt(key("gc_interval_ms"), (int) MemoryModule.gcHintIntervalMs);
        maxParticles = cfg.getInt(key("max_particles"), ParticleLimiterModule.maxParticles);
        particleFpsThreshold = cfg.getInt(key("particle_fps_threshold"), ParticleLimiterModule.aggressiveCullFpsThreshold);
        cullingIntervalMs = cfg.getInt(key("culling_interval_ms"), EntityCullingModule.cullingIntervalMs);
        maxRenderDistanceBlocks = cfg.getInt(key("max_render_distance_blocks"), EntityCullingModule.maxRenderDistanceBlocks);
        sendBufferBytes = cfg.getInt(key("send_buffer_bytes"), NetworkModule.sendBufferBytes);
        receiveBufferBytes = cfg.getInt(key("receive_buffer_bytes"), NetworkModule.receiveBufferBytes);
        tcpNoDelay = cfg.getBoolean(key("tcp_no_delay"), NetworkModule.tcpNoDelay);
    }

    /** Captures the current live performance tunables as this profile's new saved snapshot. */
    public void captureAndSave() {
        ConfigManager cfg = ConfigManager.getInstance();
        minimizedSleepMs = RenderModule.minimizedSleepMs;
        rdEnabled = RenderModule.dynamicRenderDistanceEnabled;
        rdFpsThreshold = RenderModule.dynamicRenderDistanceFpsThreshold;
        rdGraceFrames = RenderModule.dynamicRenderDistanceGraceFrames;
        extraChunks = ViewDistanceModule.extraChunks;
        vdEnabled = ViewDistanceModule.getInstance().isEnabled();
        gcThreshold = MemoryModule.gcHintThreshold;
        gcIntervalMs = MemoryModule.gcHintIntervalMs;
        maxParticles = ParticleLimiterModule.maxParticles;
        particleFpsThreshold = ParticleLimiterModule.aggressiveCullFpsThreshold;
        cullingIntervalMs = EntityCullingModule.cullingIntervalMs;
        maxRenderDistanceBlocks = EntityCullingModule.maxRenderDistanceBlocks;
        sendBufferBytes = NetworkModule.sendBufferBytes;
        receiveBufferBytes = NetworkModule.receiveBufferBytes;
        tcpNoDelay = NetworkModule.tcpNoDelay;

        cfg.set(key("name"), name);
        cfg.set(key("minimized_sleep_ms"), minimizedSleepMs);
        cfg.set(key("rd_enabled"), rdEnabled);
        cfg.set(key("rd_fps_threshold"), rdFpsThreshold);
        cfg.set(key("rd_grace_frames"), rdGraceFrames);
        cfg.set(key("extra_chunks"), extraChunks);
        cfg.set(key("vd_enabled"), vdEnabled);
        cfg.set(key("gc_threshold"), gcThreshold);
        cfg.set(key("gc_interval_ms"), (int) gcIntervalMs);
        cfg.set(key("max_particles"), maxParticles);
        cfg.set(key("particle_fps_threshold"), particleFpsThreshold);
        cfg.set(key("culling_interval_ms"), cullingIntervalMs);
        cfg.set(key("max_render_distance_blocks"), maxRenderDistanceBlocks);
        cfg.set(key("send_buffer_bytes"), sendBufferBytes);
        cfg.set(key("receive_buffer_bytes"), receiveBufferBytes);
        cfg.set(key("tcp_no_delay"), tcpNoDelay);
    }

    @Override
    public void rename(String newName) {
        name = newName;
        ConfigManager.getInstance().set(key("name"), newName);
    }

    @Override public String getName() { return name; }
    @Override public String getDescription() { return "Your saved custom performance setup."; }
    @Override public ProfileCategory getCategory() { return ProfileCategory.PERFORMANCE; }

    @Override
    public void apply() {
        ConfigManager cfg = ConfigManager.getInstance();

        RenderModule.minimizedSleepMs = minimizedSleepMs;
        RenderModule.dynamicRenderDistanceEnabled = rdEnabled;
        RenderModule.dynamicRenderDistanceFpsThreshold = rdFpsThreshold;
        RenderModule.dynamicRenderDistanceGraceFrames = rdGraceFrames;
        cfg.set("render.minimized_sleep_ms", minimizedSleepMs);
        cfg.set("render.dynamic_rd_enabled", rdEnabled);
        cfg.set("render.dynamic_rd_fps_threshold", rdFpsThreshold);
        cfg.set("render.dynamic_rd_grace_frames", rdGraceFrames);

        ViewDistanceModule.getInstance().setExtraChunks(extraChunks);
        ViewDistanceModule.getInstance().setEnabled(vdEnabled);

        MemoryModule.gcHintThreshold = gcThreshold;
        MemoryModule.gcHintIntervalMs = gcIntervalMs;
        cfg.set("memory.gc_threshold", gcThreshold);
        cfg.set("memory.gc_interval_ms", gcIntervalMs);

        ParticleLimiterModule.maxParticles = maxParticles;
        ParticleLimiterModule.aggressiveCullFpsThreshold = particleFpsThreshold;
        cfg.set("particle_limiter.max_particles", maxParticles);
        cfg.set("particle_limiter.aggressive_fps_threshold", particleFpsThreshold);

        EntityCullingModule.cullingIntervalMs = cullingIntervalMs;
        EntityCullingModule.maxRenderDistanceBlocks = maxRenderDistanceBlocks;
        cfg.set("entity_culling.interval_ms", cullingIntervalMs);
        cfg.set("entity_culling.max_distance", maxRenderDistanceBlocks);

        NetworkModule.sendBufferBytes = sendBufferBytes;
        NetworkModule.receiveBufferBytes = receiveBufferBytes;
        NetworkModule.tcpNoDelay = tcpNoDelay;
        cfg.set("network.send_buffer_bytes", sendBufferBytes);
        cfg.set("network.receive_buffer_bytes", receiveBufferBytes);
        cfg.set("network.tcp_no_delay", tcpNoDelay);
    }

    @Override
    public boolean matchesCurrentState() {
        return RenderModule.minimizedSleepMs == minimizedSleepMs
                && RenderModule.dynamicRenderDistanceEnabled == rdEnabled
                && RenderModule.dynamicRenderDistanceFpsThreshold == rdFpsThreshold
                && RenderModule.dynamicRenderDistanceGraceFrames == rdGraceFrames
                && ViewDistanceModule.extraChunks == extraChunks
                && ViewDistanceModule.getInstance().isEnabled() == vdEnabled
                && MemoryModule.gcHintThreshold == gcThreshold
                && MemoryModule.gcHintIntervalMs == gcIntervalMs
                && ParticleLimiterModule.maxParticles == maxParticles
                && ParticleLimiterModule.aggressiveCullFpsThreshold == particleFpsThreshold
                && EntityCullingModule.cullingIntervalMs == cullingIntervalMs
                && EntityCullingModule.maxRenderDistanceBlocks == maxRenderDistanceBlocks
                && NetworkModule.sendBufferBytes == sendBufferBytes
                && NetworkModule.receiveBufferBytes == receiveBufferBytes
                && NetworkModule.tcpNoDelay == tcpNoDelay;
    }
}
