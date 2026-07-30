package com.example.solstice.mixin.viewdistance;

import com.example.solstice.viewdistance.ViewDistanceModule;
import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Ported from Johni0702/bobby (LGPL-3.0-only, see NOTICE.md).
 *
 * <p>In singleplayer, {@code IntegratedServer.tick()} normally syncs the
 * embedded server's own per-player tracking distance straight to {@code
 * client.options.getViewDistance()} (confirmed via decompile - the exact
 * source of the vanilla "Changing view distance to {}, from {}" log line):
 * {@code Math.max(2, client.options.getViewDistance().getValue())}. Once
 * {@link com.example.solstice.mixin.viewdistance.GameOptionsMixin} lets the
 * client's own render-distance option go arbitrarily high, that same sync
 * would make the *server* also try to send genuinely real data out to the
 * full distance - leaving nothing left over for retention to fill in. This
 * overrides that computed value with a fixed, independently-configured
 * number when set, so the embedded server keeps sending only a modest real
 * radius no matter how far the client's own render distance is raised.</p>
 */
@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin {
    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I", ordinal = 0), index = 1)
    private int solstice$overwriteServerViewDistance(int viewDistance) {
        int overwrite = ViewDistanceModule.serverViewDistanceOverwrite;
        if (overwrite != 0) {
            return overwrite;
        }
        return viewDistance;
    }
}
