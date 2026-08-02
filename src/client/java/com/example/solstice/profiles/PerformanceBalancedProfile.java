package com.example.solstice.profiles;

import com.example.solstice.core.config.ConfigManager;
import com.example.solstice.performance.memory.MemoryModule;
import com.example.solstice.performance.network.NetworkModule;
import com.example.solstice.performance.render.EntityCullingModule;
import com.example.solstice.performance.render.ParticleLimiterModule;
import com.example.solstice.performance.render.RenderModule;

/**
 * The middle tier - a real, moderate cut from {@link PerformanceLiteProfile}'s
 * baseline (roughly half the particle cap, three-quarters the entity-culling
 * distance, a somewhat more proactive GC hint) without going as far as
 * {@link PerformanceAggressiveProfile}. Per explicit direction: "the perfect
 * compromise for performance without making too many cuts".
 *
 * <p>Uses {@link EntityCullingModule.CullMode#CONSERVATIVE} rather than the
 * occlusion-raycast mode the other tiers keep - real playtesting found the
 * raycast approach still culled plainly-visible animals at range (terrain
 * occlusion false positives are inherent to that technique, not a bug left
 * to fix), which isn't the "moderate, unobtrusive" experience this tier is
 * supposed to be. Conservative only culls stacked mob piles and cave mobs
 * buried deep below the player - see {@code EntityCullingModule}'s own
 * Javadoc. {@link PerformanceAggressiveProfile} deliberately keeps the old
 * occlusion mode unchanged, per explicit direction.</p>
 *
 * <p>Never touches {@code RenderModule}'s dynamic-render-distance fields or
 * {@link com.example.solstice.viewdistance.ViewDistanceModule} at all -
 * per explicit instruction, none of the three performance profiles should
 * touch view-distance-related settings in any way, not even to reset them.</p>
 */
public final class PerformanceBalancedProfile implements Profile {

    private static final int MINIMIZED_SLEEP_MS = 350;

    private static final double GC_THRESHOLD = 0.80;
    private static final long GC_INTERVAL_MS = 25_000;
    private static final int MAX_PARTICLES = 2048;
    private static final int PARTICLE_FPS_THRESHOLD = 45;
    private static final EntityCullingModule.CullMode CULL_MODE = EntityCullingModule.CullMode.CONSERVATIVE;
    private static final int CULLING_INTERVAL_MS = 40;
    private static final int MAX_RENDER_DISTANCE_BLOCKS = 48;
    private static final int SEND_BUFFER_BYTES = 98304;
    private static final int RECEIVE_BUFFER_BYTES = 196608;
    private static final boolean TCP_NO_DELAY = true;

    @Override public String getName() { return "Balanced"; }
    @Override public String getDescription() { return "The perfect compromise for performance without making too many cuts - a real, moderate reduction in particles and entity render distance."; }
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
