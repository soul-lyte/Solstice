package com.example.solstice.mixin.optimization;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import net.minecraft.world.explosion.ExplosionImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

/**
 * Adapted from Lithium's {@code ServerExplosionMixin} (block_raycast package) -
 * CaffeineMC, LGPL-3.0-only, see NOTICE.md and licenses/LICENSE-LGPL-3.0.txt.
 * Original authors per Lithium's own Javadoc: Jellyquid (original), 2No2Name
 * (perf/compat improvements, direct-mapped cache), jcw780 (ray generation),
 * pwouik (original resistance-caching PR).
 *
 * <p>Lithium's own version targets a newer Minecraft (Mojmap {@code
 * ServerExplosion}, split into {@code calculateExplodedPositions}/{@code
 * performRayCast}/{@code traverseBlock} helper methods) - this is not a literal
 * copy. It's the same core technique (cache block state + blast resistance per
 * absolute position, since neither can change during one synchronous explosion
 * call, so revisits from other rays are free) ported onto 1.21.11's actual
 * {@code ExplosionImpl.getBlocksToDestroy()} - a single private method with no
 * equivalent extraction points, decompile-verified earlier this session.</p>
 *
 * <p>Deliberately simpler than Lithium's raw direct-mapped array cache (which
 * needs a hash-mix + fixed-size-collision scheme to get right): a plain {@code
 * Long2ObjectOpenHashMap}, discarded per explosion instead of thread-local reuse.
 * Trades a little peak throughput for something easier to verify correct without
 * being able to load-test it live.</p>
 *
 * <p>Preserves vanilla's exact ray-generation math, {@code random.nextFloat()}
 * call order/count (unchanged - still once per ray, same position in the loop),
 * step size, falloff, and resistance formula - only the block-lookup path is
 * cached. {@code canDestroyBlock} depends on the ray's current strength, not
 * just position, so it still runs every step, uncached - only {@code
 * getBlastResistance} (position-only) and the block/fluid state lookup are
 * cached. Output is intended to be identical to vanilla, not just similar.</p>
 */
@Mixin(ExplosionImpl.class)
public abstract class ExplosionImplMixin {

    @Shadow @Final private ServerWorld world;
    @Shadow @Final private Vec3d pos;
    @Shadow @Final private float power;
    @Shadow @Final private ExplosionBehavior behavior;

    private record CachedBlockInfo(BlockState blockState, float resistanceReduction) {}

    @Inject(method = "getBlocksToDestroy", at = @At("HEAD"), cancellable = true)
    private void solstice$cachedGetBlocksToDestroy(CallbackInfoReturnable<List<BlockPos>> cir) {
        Long2ObjectOpenHashMap<CachedBlockInfo> cache = new Long2ObjectOpenHashMap<>();
        LongOpenHashSet touched = new LongOpenHashSet();
        BlockPos.Mutable mutablePos = new BlockPos.Mutable();

        for (int j = 0; j < 16; j++) {
            for (int k = 0; k < 16; k++) {
                for (int l = 0; l < 16; l++) {
                    if (j != 0 && j != 15 && k != 0 && k != 15 && l != 0 && l != 15) {
                        continue;
                    }

                    double d = j / 15.0F * 2.0F - 1.0F;
                    double e = k / 15.0F * 2.0F - 1.0F;
                    double f = l / 15.0F * 2.0F - 1.0F;
                    double g = Math.sqrt(d * d + e * e + f * f);
                    d /= g;
                    e /= g;
                    f /= g;
                    float h = this.power * (0.7F + this.world.random.nextFloat() * 0.6F);
                    double m = this.pos.x;
                    double n = this.pos.y;
                    double o = this.pos.z;

                    for (; h > 0.0F; h -= 0.22500001F) {
                        int blockX = MathHelper.floor(m);
                        int blockY = MathHelper.floor(n);
                        int blockZ = MathHelper.floor(o);
                        long packed = BlockPos.asLong(blockX, blockY, blockZ);

                        CachedBlockInfo info = cache.get(packed);
                        if (info == null) {
                            mutablePos.set(blockX, blockY, blockZ);
                            if (!this.world.isInBuildLimit(mutablePos)) {
                                break;
                            }
                            BlockPos immutablePos = mutablePos.toImmutable();
                            BlockState blockState = this.world.getBlockState(immutablePos);
                            FluidState fluidState = blockState.getFluidState();
                            Optional<Float> resistance = this.behavior.getBlastResistance(
                                    (Explosion) (Object) this, this.world, immutablePos, blockState, fluidState);
                            float reduction = resistance.isPresent() ? (resistance.get() + 0.3F) * 0.3F : 0.0F;
                            info = new CachedBlockInfo(blockState, reduction);
                            cache.put(packed, info);
                        }

                        h -= info.resistanceReduction();

                        if (h > 0.0F) {
                            mutablePos.set(blockX, blockY, blockZ);
                            if (this.behavior.canDestroyBlock((Explosion) (Object) this, this.world, mutablePos, info.blockState(), h)) {
                                touched.add(packed);
                            }
                        }

                        m += d * 0.3F;
                        n += e * 0.3F;
                        o += f * 0.3F;
                    }
                }
            }
        }

        ObjectArrayList<BlockPos> result = new ObjectArrayList<>(touched.size());
        LongIterator it = touched.iterator();
        while (it.hasNext()) {
            result.add(BlockPos.fromLong(it.nextLong()));
        }
        cir.setReturnValue(result);
    }
}
