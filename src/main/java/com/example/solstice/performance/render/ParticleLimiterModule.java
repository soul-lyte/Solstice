package com.example.solstice.performance.render;

import com.example.solstice.core.module.AbstractModule;
import com.example.solstice.core.module.ModuleCategory;
import com.example.solstice.core.util.TimeUtil;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * ParticleLimiterModule - caps particle counts to prevent particle spam
 * from destroying frame time.
 *
 * <p>Minecraft's built-in "Particles: Minimal" option only reduces particles
 * spawned from blocks. This module additionally caps particle spawn rate
 * and drops low-priority particles when the estimate is high - the same
 * goal as Sodium's particle patch, re-implemented natively.</p>
 *
 * <p>Live particle count is estimated rather than read directly from
 * Minecraft's internal particle storage: that storage is an internal,
 * unstable implementation detail (its exact shape changed as part of the
 * 1.21.9 world-rendering rework), so depending on it via a shadowed Mixin
 * field would be fragile across versions. Instead, this module keeps its
 * own decaying counter - incremented on every accepted spawn in
 * {@code ParticleManagerMixin} and decayed once per tick - which is a
 * reasonable proxy for particle load without any dependency on Minecraft's
 * internal particle-storage layout.</p>
 */
public final class ParticleLimiterModule extends AbstractModule {

    private static final ParticleLimiterModule INSTANCE = new ParticleLimiterModule();

    /** Soft cap on the estimated spawn-rate counter. */
    public static int maxParticles = 4096;

    /** Below this FPS, aggressively halve the spawn rate of new particles. */
    public static int aggressiveCullFpsThreshold = 40;

    /** How many estimated particles decay away per tick (~ once every 50 ms). */
    private static final int DECAY_PER_TICK = 40;

    private final AtomicInteger estimatedCount = new AtomicInteger(0);
    private long lastDecayMs = 0;

    private ParticleLimiterModule() {}

    public static ParticleLimiterModule getInstance() { return INSTANCE; }

    @Override public String getId()          { return "particle_limiter"; }
    @Override public String getDisplayName() { return "Particle Limiter"; }
    @Override public String getDescription() { return "Caps particle spawn rate to prevent particle-heavy scenes from tanking frame time."; }

    @Override
    public java.util.List<String> getSearchKeywords() {
        return java.util.List.of("less particles", "fewer particles", "particle lag", "particle cap");
    }
    @Override public ModuleCategory getCategory() { return ModuleCategory.ADVANCED; }

    @Override
    protected void init() {
        maxParticles = com.example.solstice.core.config.ConfigManager.getInstance()
                .getInt("particle_limiter.max_particles", 4096);
        aggressiveCullFpsThreshold = com.example.solstice.core.config.ConfigManager.getInstance()
                .getInt("particle_limiter.aggressive_fps_threshold", 40);
    }

    @Override
    public java.util.List<com.example.solstice.core.module.ModuleSetting> getSettings() {
        return java.util.List.of(
                new com.example.solstice.core.module.ModuleSetting.IntSetting(
                        "Max Particles",
                        "The most particles Solstice will let exist at once. Lower this if particle-heavy effects (explosions, fireworks) tank your FPS.",
                        256, 16384,
                        () -> maxParticles,
                        v -> { maxParticles = v; com.example.solstice.core.config.ConfigManager.getInstance().set("particle_limiter.max_particles", v); }),
                new com.example.solstice.core.module.ModuleSetting.IntSetting(
                        "Aggressive Cull FPS Threshold",
                        "If your FPS drops below this while there are lots of particles on screen, Solstice starts dropping some new particles to help you recover.",
                        5, 120,
                        () -> aggressiveCullFpsThreshold,
                        v -> { aggressiveCullFpsThreshold = v; com.example.solstice.core.config.ConfigManager.getInstance().set("particle_limiter.aggressive_fps_threshold", v); })
        );
    }

    /**
     * Called from {@code ParticleManagerMixin} to decide whether a new particle
     * should be allowed to spawn, based on our own decaying spawn estimate.
     */
    public boolean canSpawnParticle(float currentFps) {
        if (!isEnabled()) return true;
        decayIfDue();

        int current = estimatedCount.get();
        if (current >= maxParticles) return false;

        if (currentFps < aggressiveCullFpsThreshold && current > maxParticles / 2) {
            // Drop ~50 % of new particles during low-FPS periods
            return (System.nanoTime() & 1) == 0;
        }
        return true;
    }

    /** Called from {@code ParticleManagerMixin} after a particle is actually spawned. */
    public void recordSpawn() {
        estimatedCount.incrementAndGet();
    }

    private void decayIfDue() {
        if (TimeUtil.elapsed(lastDecayMs, 50)) {
            estimatedCount.updateAndGet(v -> Math.max(0, v - DECAY_PER_TICK));
            lastDecayMs = TimeUtil.nowMs();
        }
    }
}
