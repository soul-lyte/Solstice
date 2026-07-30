package com.example.solstice.mixin.viewdistance;

import com.example.solstice.viewdistance.ext.LightingProviderExt;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.chunk.light.LightingProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ported from Johni0702/bobby (LGPL-3.0-only, see NOTICE.md).
 *
 * <p>Solstice's own earlier attempt at chunk retention tried calling {@code
 * LightingProvider.setColumnEnabled(pos, true)} directly on the real,
 * stateful light engine to make a retained chunk's mesh eligible to build
 * again - a fragile approach, since it depends on not racing or conflicting
 * with the engine's own internal invariants about when a column is genuinely
 * enabled. This is the actual robust fix: a completely separate,
 * Solstice-owned set of "enabled" columns that {@code isLightingEnabled}
 * consults FIRST, before ever touching the real engine's own state. A
 * retained chunk's column simply never needs the real engine to agree it's
 * valid - this Mixin makes that check succeed on its own.</p>
 */
@Mixin(value = LightingProvider.class)
public abstract class LightingProviderMixin implements LightingProviderExt {
    @Unique
    private final LongSet solstice$activeColumns = new LongOpenHashSet();

    @Override
    public void solstice$enabledColumn(long pos) {
        this.solstice$activeColumns.add(pos);
    }

    @Override
    public void solstice$disableColumn(long pos) {
        this.solstice$activeColumns.remove(pos);
    }

    @Inject(method = "isLightingEnabled", at = @At("HEAD"), cancellable = true)
    private void solstice$isLightingEnabled(long sectionPos, CallbackInfoReturnable<Boolean> ci) {
        if (solstice$activeColumns.contains(sectionPos)) {
            ci.setReturnValue(true);
        }
    }
}
