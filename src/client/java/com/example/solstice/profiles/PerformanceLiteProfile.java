package com.example.solstice.profiles;

import com.example.solstice.core.config.ConfigManager;
import com.example.solstice.performance.memory.MemoryModule;
import com.example.solstice.performance.network.NetworkModule;
import com.example.solstice.performance.render.EntityCullingModule;
import com.example.solstice.performance.render.ParticleLimiterModule;
import com.example.solstice.performance.render.RenderModule;

/**
 * The gentlest of the three tiers - applies the modules' own shipped
 * defaults verbatim, making no visual/gameplay cuts at all. This is
 * deliberately just "confirm the sensible baseline", not a reduced-from-
 * baseline tier - per explicit direction, Lite should read as "optimized,
 * but without making any cuts for extra performance".
 *
 * <p>Never touches {@code RenderModule}'s dynamic-render-distance fields or
 * {@link com.example.solstice.viewdistance.ViewDistanceModule} at all -
 * per explicit instruction, none of the three performance profiles should
 * touch view-distance-related settings in any way, not even to reset them.</p>
 */
public final class PerformanceLiteProfile implements Profile {

    private static final int MINIMIZED_SLEEP_MS = 250;

    private static final double GC_THRESHOLD = 0.85;
    private static final long GC_INTERVAL_MS = 30_000;
    private static final int MAX_PARTICLES = 4096;
    private static final int PARTICLE_FPS_THRESHOLD = 40;
    private static final int CULLING_INTERVAL_MS = 50;
    private static final int MAX_RENDER_DISTANCE_BLOCKS = 64;
    private static final int SEND_BUFFER_BYTES = 131072;
    private static final int RECEIVE_BUFFER_BYTES = 262144;
    private static final boolean TCP_NO_DELAY = true;

    @Override public String getName() { return "Lite"; }
    @Override public String getDescription() { return "Optimized, but without making any cuts - full particle count, full entity render distance, just the shipped defaults confirmed."; }
    @Override public ProfileCategory getCategory() { return ProfileCategory.PERFORMANCE; }

    @Override
    public void apply() {
        ConfigManager cfg = ConfigManager.getInstance();

        RenderModule.minimizedSleepMs = MINIMIZED_SLEEP_MS;
        cfg.set("render.minimized_sleep_ms", MINIMIZED_SLEEP_MS);

        MemoryModule.gcHintThreshold = GC_THRESHOLD;
        MemoryModule.gcHintIntervalMs = GC_INTERVAL_MS;
        cfg.set("memory.gc_threshold", GC_THRESHOLD);
        cfg.set("memory.gc_interval_ms", GC_INTERVAL_MS);

        ParticleLimiterModule.maxParticles = MAX_PARTICLES;
        ParticleLimiterModule.aggressiveCullFpsThreshold = PARTICLE_FPS_THRESHOLD;
        cfg.set("particle_limiter.max_particles", MAX_PARTICLES);
        cfg.set("particle_limiter.aggressive_fps_threshold", PARTICLE_FPS_THRESHOLD);

        EntityCullingModule.cullingIntervalMs = CULLING_INTERVAL_MS;
        EntityCullingModule.maxRenderDistanceBlocks = MAX_RENDER_DISTANCE_BLOCKS;
        cfg.set("entity_culling.interval_ms", CULLING_INTERVAL_MS);
        cfg.set("entity_culling.max_distance", MAX_RENDER_DISTANCE_BLOCKS);

        NetworkModule.sendBufferBytes = SEND_BUFFER_BYTES;
        NetworkModule.receiveBufferBytes = RECEIVE_BUFFER_BYTES;
        NetworkModule.tcpNoDelay = TCP_NO_DELAY;
        cfg.set("network.send_buffer_bytes", SEND_BUFFER_BYTES);
        cfg.set("network.receive_buffer_bytes", RECEIVE_BUFFER_BYTES);
        cfg.set("network.tcp_no_delay", TCP_NO_DELAY);
    }

    @Override
    public boolean matchesCurrentState() {
        return RenderModule.minimizedSleepMs == MINIMIZED_SLEEP_MS
                && MemoryModule.gcHintThreshold == GC_THRESHOLD
                && MemoryModule.gcHintIntervalMs == GC_INTERVAL_MS
                && ParticleLimiterModule.maxParticles == MAX_PARTICLES
                && ParticleLimiterModule.aggressiveCullFpsThreshold == PARTICLE_FPS_THRESHOLD
                && EntityCullingModule.cullingIntervalMs == CULLING_INTERVAL_MS
                && EntityCullingModule.maxRenderDistanceBlocks == MAX_RENDER_DISTANCE_BLOCKS
                && NetworkModule.sendBufferBytes == SEND_BUFFER_BYTES
                && NetworkModule.receiveBufferBytes == RECEIVE_BUFFER_BYTES
                && NetworkModule.tcpNoDelay == TCP_NO_DELAY;
    }
}
