package com.example.solstice.profiles;

import com.example.solstice.core.config.ConfigManager;
import com.example.solstice.performance.memory.MemoryModule;
import com.example.solstice.performance.network.NetworkModule;
import com.example.solstice.performance.render.EntityCullingModule;
import com.example.solstice.performance.render.ParticleLimiterModule;
import com.example.solstice.performance.render.RenderModule;

/**
 * The heaviest tier - built for genuinely low-end machines, cutting every
 * tunable this project exposes as far as reasonable for maximum FPS: a
 * small particle cap, a short entity-culling distance, frequent/eager GC
 * hints, small network buffers. Per explicit direction: "for lower end
 * PCs, cut everything you can for maximum fps".
 *
 * <p>Never touches {@code RenderModule}'s dynamic-render-distance fields or
 * {@link com.example.solstice.viewdistance.ViewDistanceModule} at all -
 * per explicit instruction, none of the three performance profiles should
 * touch view-distance-related settings in any way, not even to reset them.</p>
 */
public final class PerformanceAggressiveProfile implements Profile {

    private static final int MINIMIZED_SLEEP_MS = 500;

    private static final double GC_THRESHOLD = 0.65;
    private static final long GC_INTERVAL_MS = 10_000;
    private static final int MAX_PARTICLES = 256;
    private static final int PARTICLE_FPS_THRESHOLD = 55;
    private static final int CULLING_INTERVAL_MS = 25;
    private static final int MAX_RENDER_DISTANCE_BLOCKS = 16;
    private static final int SEND_BUFFER_BYTES = 32768;
    private static final int RECEIVE_BUFFER_BYTES = 65536;
    private static final boolean TCP_NO_DELAY = true;

    @Override public String getName() { return "Aggressive"; }
    @Override public String getDescription() { return "Built for lower-end PCs - cuts every tunable as far as reasonable for maximum FPS. Never touches render/view distance."; }
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
