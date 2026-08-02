package com.example.solstice.performance.render;

import com.example.solstice.core.config.ConfigManager;
import com.example.solstice.core.module.AbstractModule;
import com.example.solstice.core.module.ModuleCategory;
import com.example.solstice.core.module.ModuleSetting;
import com.example.solstice.core.util.TimeUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EntityCullingModule - skips rendering entities occluded by terrain
 * (raycast visibility test) or beyond a max render distance.
 *
 * <p>Hooked via {@code mixin.render.EntityRenderManagerMixin} on {@code
 * EntityRenderManager.shouldRender} - the real per-frame per-entity decision
 * point in 1.21.11's post-1.21.9-rework render pipeline (replaces the old,
 * now-nonexistent {@code EntityRenderer.render(Entity, ...)} target this
 * module's Mixin used to guess at). Cancelling here skips both render-state
 * extraction and draw submission for that entity, not just the draw call.</p>
 *
 * <p>Each entity re-checks its own occlusion independently, staggered by its
 * own last-checked timestamp, rather than re-raycasting every entity in the
 * world in one synchronous burst every interval - the latter is a real stutter
 * risk in mob-dense areas (farms) that this was rewritten to avoid.</p>
 *
 * <p>Three culling strategies, selected via {@link #cullMode}, since the
 * original single occlusion-raycast approach (now {@link CullMode#AGGRESSIVE})
 * turned out to still read as too eager for everyday play even after several
 * rounds of false-positive fixes below - real report: clearly-visible animals
 * 20 blocks away across open ground getting culled anyway. Per explicit
 * direction, only {@link CullMode#AGGRESSIVE} keeps that behavior (still used
 * by the Aggressive performance profile, unchanged); {@link
 * CullMode#CONSERVATIVE} (the Balanced profile) only culls two narrow,
 * genuinely-never-a-problem cases - see that enum constant's own Javadoc -
 * and {@link CullMode#PRACTICE} culls every non-player entity outright, for
 * a PvP practice server where nothing else matters.</p>
 *
 * <h2>{@link CullMode#AGGRESSIVE} occlusion detection</h2>
 *
 * <p>Occlusion is sampled at all 8 corners of the entity's bounding box (not
 * just the center) - occluded only if <em>every</em> corner is blocked - and
 * an entity only actually gets hidden after staying continuously occluded
 * for {@link #CULL_GRACE_PERIOD_MS}. Both exist so a genuinely still-visible
 * sliver of an entity (only its feet showing below a wall, an arm sticking
 * out past a corner) never gets culled, and momentary/partial occlusion
 * (a fence post, walking past a single block) doesn't make entities flicker
 * in and out. The grace period is a fixed floor, not something any
 * performance profile (including Aggressive) can shorten.</p>
 *
 * <p>Each sample ray also sees through non-occluding blocks instead of
 * stopping at the first thing it geometrically touches - {@link
 * net.minecraft.world.RaycastContext.ShapeType#VISUAL} registers a hit on
 * <em>any</em> block with real geometry, including glass (a full cube shape,
 * just rendered translucent) and partial shapes like slabs/stairs/fences
 * (real geometry in their own occupied portion). A block only actually
 * counts as blocking if {@link net.minecraft.block.BlockState#isOpaqueFullCube()}
 * is true for it - real vanilla API for "genuinely opaque and fills the
 * whole voxel," the same check {@code SectionBuilder} itself uses. Anything
 * else (glass, slabs, fences, trapdoors, ...) gets stepped past and the ray
 * continues toward the target, matching what you can actually see with your
 * own eyes through/around them.</p>
 *
 * <p>Client-only: uses {@link MinecraftClient} and client rendering types.
 * Lives in src/client/java.</p>
 */
public final class EntityCullingModule extends AbstractModule {

    private static final EntityCullingModule INSTANCE = new EntityCullingModule();

    /** Which culling strategy {@link #shouldRender} uses - see this class's own Javadoc. */
    public enum CullMode {
        /** The original occlusion-raycast approach - real, but eager enough that the Balanced profile no longer uses it. */
        AGGRESSIVE,
        /**
         * Only culls two narrow, genuinely-never-visible cases, ignoring
         * plain terrain occlusion entirely: a pile of same-type entities
         * crammed together (a mob farm/grinder - all but one representative
         * per cluster are culled, since drawing 40 overlapping cows adds
         * nothing you can actually distinguish), and an entity buried deep
         * enough below the player <em>and</em> occluded (a genuinely
         * unreachable-by-sight cave mob) - see {@link #isStackedAndNotRepresentative}
         * and {@link #isDeepAndNeverVisible}. Deliberately does not apply
         * {@link #maxRenderDistanceBlocks} at all - a distant-but-plainly-visible
         * animal across open ground is exactly the false positive this mode
         * exists to stop, and a flat distance cutoff would just reintroduce
         * it a different way.
         */
        CONSERVATIVE,
        /** Culls every non-player entity outright, no checks at all - see the Practice performance profile. */
        PRACTICE
    }

    public static CullMode cullMode = CullMode.AGGRESSIVE;

    /** How long a cached visibility result stays valid before re-checking that entity, in ms. */
    public static int cullingIntervalMs = 50;

    /** Entities beyond this distance (blocks) are always culled. Only consulted by {@link CullMode#AGGRESSIVE}. */
    public static int maxRenderDistanceBlocks = 64;

    /** Full-cache sweep interval - bounds memory from despawned entities' stale entries. */
    private static final long CACHE_SWEEP_INTERVAL_MS = 30_000L;

    /**
     * How long an entity has to stay continuously occluded before it's actually
     * hidden - a fixed floor, not something any profile (including Aggressive)
     * can shorten. Without this, a single block/fence post/leaf briefly crossing
     * the line of sight (e.g. walking past it) made entities flicker in and out
     * instead of staying visible through a momentary occlusion. Only applies to
     * {@link CullMode#AGGRESSIVE} - {@link CullMode#CONSERVATIVE}'s own two
     * triggers are stable enough (a cluster's UUID ordering, being deep
     * underground) not to need a settling period.
     */
    private static final long CULL_GRACE_PERIOD_MS = 3000L;

    /** {@link CullMode#CONSERVATIVE}: same-type entities within this many blocks of each other count as one cluster. */
    private static final double STACK_RADIUS = 2.0;

    /** {@link CullMode#CONSERVATIVE}: a cluster needs at least this many members (self included) before culling kicks in. */
    private static final int STACK_THRESHOLD = 5;

    /** {@link CullMode#CONSERVATIVE}: how far below the player (blocks) an occluded entity has to be to count as "never visible anyway." */
    private static final double DEEP_BELOW_THRESHOLD = 10.0;

    private final Map<UUID, Boolean> visibilityCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastCheckedMs = new ConcurrentHashMap<>();
    private final Map<UUID, Long> occludedSinceMs = new ConcurrentHashMap<>();
    private long lastSweepMs = 0;

    private EntityCullingModule() {}

    public static EntityCullingModule getInstance() { return INSTANCE; }

    @Override public String getId()          { return "entity_culling"; }
    @Override public String getDisplayName() { return "Entity Culling"; }
    @Override public String getDescription() { return "Skips rendering entities hidden behind terrain, reducing GPU draw calls significantly."; }

    @Override
    public java.util.List<String> getSearchKeywords() {
        return java.util.List.of("entityculling", "occlusion culling", "mob lag", "hidden entities", "entity lag");
    }
    @Override public ModuleCategory getCategory() { return ModuleCategory.ADVANCED; }

    private static final String CULL_MODE_CONFIG = "entity_culling.cull_mode";

    @Override
    protected void init() {
        cullingIntervalMs = ConfigManager.getInstance()
                .getInt("entity_culling.interval_ms", 50);
        maxRenderDistanceBlocks = ConfigManager.getInstance()
                .getInt("entity_culling.max_distance", 64);
        String storedMode = ConfigManager.getInstance().getString(CULL_MODE_CONFIG, CullMode.AGGRESSIVE.name());
        try {
            cullMode = CullMode.valueOf(storedMode);
        } catch (IllegalArgumentException e) {
            cullMode = CullMode.AGGRESSIVE;
        }
    }

    @Override
    public java.util.List<ModuleSetting> getSettings() {
        return java.util.List.of(
                new ModuleSetting.ChoiceSetting(
                        "Cull Mode",
                        "Aggressive checks real terrain occlusion (works well most of the time, but can occasionally cull something you'd technically still see). Conservative only culls stacked mob piles and cave mobs buried deep below you - the least noticeable option. Practice culls every non-player entity outright, for PvP practice servers where only players matter.",
                        java.util.List.of("Aggressive", "Conservative", "Practice"),
                        () -> cullMode.ordinal(),
                        v -> { cullMode = CullMode.values()[v]; ConfigManager.getInstance().set(CULL_MODE_CONFIG, cullMode.name()); }),
                new ModuleSetting.IntSetting(
                        "Recheck Interval (ms)",
                        "How long a cached occlusion result for one entity stays trusted before checking it again.",
                        16, 500,
                        () -> cullingIntervalMs,
                        v -> { cullingIntervalMs = v; ConfigManager.getInstance().set("entity_culling.interval_ms", v); }),
                new ModuleSetting.IntSetting(
                        "Max Render Distance (blocks)",
                        "Entities farther than this are always culled, regardless of visibility. Only used by Aggressive mode.",
                        16, 128,
                        () -> maxRenderDistanceBlocks,
                        v -> { maxRenderDistanceBlocks = v; ConfigManager.getInstance().set("entity_culling.max_distance", v); })
        );
    }

    /**
     * Called from {@code EntityRenderManagerMixin} after vanilla's own frustum
     * check already returned {@code true} for this entity.
     *
     * @return {@code true} if the entity should be rendered, {@code false} to skip.
     */
    public boolean shouldRender(Entity entity, MinecraftClient client) {
        if (!isEnabled() || client.player == null) return true;
        // Never cull players - a culled-out player behind a wall corner or at range is a real
        // PvP/SMP awareness problem (missing a nearby player is a much worse outcome than the
        // small GPU cost of always drawing them), not just a visual nicety like it is for mobs.
        if (entity instanceof PlayerEntity) return true;

        if (cullMode == CullMode.PRACTICE) {
            // Nothing besides players matters here - no distance/occlusion check needed at all.
            return false;
        }
        if (cullMode == CullMode.CONSERVATIVE) {
            return shouldRenderConservative(entity, client);
        }
        return shouldRenderAggressive(entity, client);
    }

    private boolean shouldRenderAggressive(Entity entity, MinecraftClient client) {
        double distSq = client.player.squaredDistanceTo(entity);
        double maxDistSq = (double) maxRenderDistanceBlocks * maxRenderDistanceBlocks;
        if (distSq > maxDistSq) return false;

        sweepStaleEntriesIfDue();

        UUID id = entity.getUuid();
        long now = TimeUtil.nowMs();
        boolean occludedNow;
        Long checkedAt = lastCheckedMs.get(id);
        if (checkedAt == null || now - checkedAt >= cullingIntervalMs) {
            occludedNow = isOccluded(entity.getBoundingBox(), client);
            visibilityCache.put(id, !occludedNow);
            lastCheckedMs.put(id, now);
        } else {
            occludedNow = !visibilityCache.getOrDefault(id, true);
        }

        if (!occludedNow) {
            // Visible right now - always render, and forget any occlusion streak
            // that was building up, so a brief re-block later starts fresh.
            occludedSinceMs.remove(id);
            return true;
        }

        // Occluded right now, but don't actually hide it until occlusion has held
        // continuously for the full grace period - a single block/leaf/fence post
        // crossing the line of sight for a moment shouldn't make the entity flicker.
        Long since = occludedSinceMs.get(id);
        if (since == null) {
            occludedSinceMs.put(id, now);
            return true;
        }
        return now - since < CULL_GRACE_PERIOD_MS;
    }

    private void sweepStaleEntriesIfDue() {
        long now = TimeUtil.nowMs();
        if (!TimeUtil.elapsed(lastSweepMs, CACHE_SWEEP_INTERVAL_MS)) return;
        lastSweepMs = now;
        visibilityCache.clear();
        lastCheckedMs.clear();
        occludedSinceMs.clear();
    }

    /**
     * {@link CullMode#CONSERVATIVE}: culls only a stacked-cluster member (see
     * {@link #isStackedAndNotRepresentative}) or a deep, occluded cave mob
     * (see {@link #isDeepAndNeverVisible}) - nothing else, regardless of
     * plain terrain occlusion or distance. Reuses the same per-entity
     * recheck-interval staggering {@link CullMode#AGGRESSIVE} already uses
     * (the stacked check runs a real spatial world query, not free), just
     * without the grace-period settling that mode also needs - both triggers
     * here are already stable frame to frame (a cluster's UUID ordering
     * doesn't flicker, and "deep underground" rarely toggles moment to
     * moment), so a settling period would only add latency with no benefit.
     */
    private boolean shouldRenderConservative(Entity entity, MinecraftClient client) {
        if (client.world == null) return true;

        sweepStaleEntriesIfDue();

        UUID id = entity.getUuid();
        long now = TimeUtil.nowMs();
        Long checkedAt = lastCheckedMs.get(id);
        if (checkedAt == null || now - checkedAt >= cullingIntervalMs) {
            boolean cull = isStackedAndNotRepresentative(entity, client.world)
                    || isDeepAndNeverVisible(entity, client);
            visibilityCache.put(id, !cull);
            lastCheckedMs.put(id, now);
            return !cull;
        }
        return visibilityCache.getOrDefault(id, true);
    }

    /**
     * True when at least {@link #STACK_THRESHOLD} entities of the exact same
     * type (a mob farm/grinder pile, not just "any nearby animals") sit
     * within {@link #STACK_RADIUS} blocks of each other, and this entity
     * isn't the one representative kept visible - chosen as whichever
     * cluster member has the lowest {@link UUID} (a fixed, arbitrary but
     * perfectly stable tiebreaker - never changes frame to frame, so exactly
     * one member of a given cluster always renders with no flicker).
     */
    private boolean isStackedAndNotRepresentative(Entity entity, World world) {
        Box searchBox = entity.getBoundingBox().expand(STACK_RADIUS);
        java.util.List<Entity> sameType = world.getOtherEntities(entity, searchBox,
                other -> other.getType() == entity.getType() && !(other instanceof PlayerEntity));

        if (sameType.size() + 1 < STACK_THRESHOLD) return false;

        for (Entity other : sameType) {
            if (other.getUuid().compareTo(entity.getUuid()) < 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when the entity sits at least {@link #DEEP_BELOW_THRESHOLD}
     * blocks below the player <em>and</em> is occluded (reusing {@link
     * #isOccluded}'s own see-through-glass-and-slabs raycast) - a real cave
     * mob under the player's feet that terrain genuinely hides, not a
     * surface animal that merely dipped behind a rise for a moment.
     */
    private boolean isDeepAndNeverVisible(Entity entity, MinecraftClient client) {
        if (client.player.getY() - entity.getY() < DEEP_BELOW_THRESHOLD) return false;
        return isOccluded(entity.getBoundingBox(), client);
    }

    /** Bounded so a thick stack of see-through blocks (many glass panes in a row) can't loop forever. */
    private static final int MAX_RAY_STEPS = 6;

    /**
     * Occluded only if every one of the box's 8 corners is blocked - if even one
     * corner has a clear line of sight, some real part of the entity is visible
     * and it must render (e.g. just its feet showing below the bottom edge of a
     * wall, hit by the two bottom-Y corners on that side).
     */
    private boolean isOccluded(Box box, MinecraftClient client) {
        if (client.world == null) return false;

        Vec3d cameraPos = client.gameRenderer.getCamera().getCameraPos();
        double[] xs = {box.minX, box.maxX};
        double[] ys = {box.minY, box.maxY};
        double[] zs = {box.minZ, box.maxZ};

        for (double x : xs) {
            for (double y : ys) {
                for (double z : zs) {
                    if (!isRayBlocked(cameraPos, new Vec3d(x, y, z), client)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Casts toward {@code to}, but a hit on a block that isn't genuinely opaque
     * and full ({@link BlockState#isOpaqueFullCube()}) doesn't stop the ray - it
     * steps just past that block and keeps going, so glass/slabs/fences/etc.
     * don't count as blocking even though they have real raycast geometry.
     */
    private boolean isRayBlocked(Vec3d from, Vec3d to, MinecraftClient client) {
        World world = client.world;
        if (world == null) return false;

        double totalDistSq = from.squaredDistanceTo(to);
        Vec3d currentFrom = from;

        for (int step = 0; step < MAX_RAY_STEPS; step++) {
            if (currentFrom.squaredDistanceTo(from) >= totalDistSq) {
                // Already stepped at or past the target - nothing left in the way.
                return false;
            }

            RaycastContext ctx = new RaycastContext(
                    currentFrom, to,
                    RaycastContext.ShapeType.VISUAL,
                    RaycastContext.FluidHandling.NONE,
                    ShapeContext.absent()
            );

            BlockHitResult result = world.raycast(ctx);
            if (result.getType() == HitResult.Type.MISS) {
                return false;
            }

            double hitDistSq = result.getPos().squaredDistanceTo(from);
            if (hitDistSq >= totalDistSq - 0.5) {
                // The "hit" landed at/past the target itself - not a real occluder in the way.
                return false;
            }

            BlockState hitState = world.getBlockState(result.getBlockPos());
            if (hitState.isOpaqueFullCube()) {
                return true;
            }

            Vec3d remaining = to.subtract(currentFrom);
            double length = remaining.length();
            if (length < 1.0E-4) {
                return false;
            }
            // Step a FULL block past the hit point, not a tiny fraction of one - a slab,
            // fence, or wall can occupy close to a full block's depth along the ray
            // depending on approach angle, and the previous 0.05-block step needed far
            // more than the step budget to actually clear that geometry, so it kept
            // re-hitting the same non-opaque block over and over until the step budget
            // ran out and this method defensively (and wrongly) reported "blocked" - the
            // real cause of clearly-visible entities still getting culled. A full-block
            // step guarantees clearing whatever was hit in a single step, regardless of
            // its thickness or orientation.
            currentFrom = result.getPos().add(remaining.multiply(1.0 / length));
        }
        return true;
    }

    public void clearCache() {
        visibilityCache.clear();
        lastCheckedMs.clear();
        occludedSinceMs.clear();
    }
}
