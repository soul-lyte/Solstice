package com.example.solstice.profiles;

import com.example.solstice.core.config.ConfigManager;
import com.example.solstice.performance.memory.MemoryModule;
import com.example.solstice.performance.network.NetworkModule;
import com.example.solstice.performance.render.EntityCullingModule;
import com.example.solstice.performance.render.ParticleLimiterModule;
import com.example.solstice.performance.render.RenderModule;

/**
 * The heaviest tier of all, after Aggressive - built specifically for PvP
 * practice servers where nothing besides other players matters, per explicit
 * direction: "make it ultra aggressive cull every entity besides player
 * (never cull players), make every performance module pushed to their max
 * level of optimization, really the maximum optimization possible."
 *
 * <p>Uses {@link EntityCullingModule.CullMode#PRACTICE} - every non-player
 * entity is culled outright, unconditionally, with no distance or occlusion
 * check at all (players are still never culled - see {@code
 * EntityCullingModule.shouldRender}'s own player check, unconditional across
 * every mode). Every other tunable this project exposes is pushed to
 * whichever end of its real, allowed range ({@code getSettings()}'s own
 * min/max) is the more aggressive direction - not just "as far as
 * Aggressive," genuinely as far as the sliders go.</p>
 *
 * <p>Never touches {@code RenderModule}'s dynamic-render-distance fields or
 * {@link com.example.solstice.viewdistance.ViewDistanceModule} at all -
 * per explicit instruction, none of the performance profiles should touch
 * view-distance-related settings in any way, not even to reset them.</p>
 */
public final class PerformancePracticeProfile implements Profile {

    private static final int MINIMIZED_SLEEP_MS = 800;

    private static final double GC_THRESHOLD = 0.50;
    private static final long GC_INTERVAL_MS = 5_000;
    private static final int MAX_PARTICLES = 256;
    private static final int PARTICLE_FPS_THRESHOLD = 120;
    private static final EntityCullingModule.CullMode CULL_MODE = EntityCullingModule.CullMode.PRACTICE;
    private static final int CULLING_INTERVAL_MS = 16;
    private static final int MAX_RENDER_DISTANCE_BLOCKS = 16;
    private static final int SEND_BUFFER_BYTES = 16384;
    private static final int RECEIVE_BUFFER_BYTES = 16384;
    private static final boolean TCP_NO_DELAY = true;

    @Override public String getName() { return "Practice"; }
    @Override public String getDescription() { return "Made for PvP practice servers, where nothing besides other players matters - culls every non-player entity outright and pushes every performance tunable to its maximum."; }
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

        EntityCullingModule.cullMode = CULL_MODE;
        EntityCullingModule.cullingIntervalMs = CULLING_INTERVAL_MS;
        EntityCullingModule.maxRenderDistanceBlocks = MAX_RENDER_DISTANCE_BLOCKS;
        cfg.set("entity_culling.cull_mode", CULL_MODE.name());
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
                && EntityCullingModule.cullMode == CULL_MODE
                && EntityCullingModule.cullingIntervalMs == CULLING_INTERVAL_MS
                && EntityCullingModule.maxRenderDistanceBlocks == MAX_RENDER_DISTANCE_BLOCKS
                && NetworkModule.sendBufferBytes == SEND_BUFFER_BYTES
                && NetworkModule.receiveBufferBytes == RECEIVE_BUFFER_BYTES
                && NetworkModule.tcpNoDelay == TCP_NO_DELAY;
    }
}
