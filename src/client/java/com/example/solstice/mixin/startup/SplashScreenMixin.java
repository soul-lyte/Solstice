package com.example.solstice.mixin.startup;

import com.example.solstice.performance.startup.StartupModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Real body of {@link StartupModule}'s splash-screen behavior - see that
 * class's Javadoc for why this exists and what it replaces (a previously
 * dead, never-wired setting).
 *
 * <p>{@code reloadCompleteTime} (confirmed via decompile) gets set exactly
 * once, inside {@code tick()}, the moment the underlying reload/load
 * finishes - a TAIL inject observing it go from unset ({@code -1L}) to set
 * fires {@link StartupModule#onSplashComplete()} (itself idempotent) and,
 * if enabled, immediately dismisses the overlay instead of waiting through
 * the ~2-second fade {@code render()} would otherwise run. Guarded by a
 * {@code @Unique} flag so this only ever fires once per real overlay
 * instance, not every tick after completion.</p>
 */
@Mixin(SplashOverlay.class)
public abstract class SplashScreenMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    private long reloadCompleteTime;

    @Unique
    private boolean solstice$handled = false;

    @Inject(method = "tick", at = @At("TAIL"))
    private void solstice$onLoadComplete(CallbackInfo ci) {
        if (this.solstice$handled || this.reloadCompleteTime == -1L) {
            return;
        }
        this.solstice$handled = true;

        StartupModule.getInstance().onSplashComplete();
        if (StartupModule.skipSplashFadeOut) {
            this.client.setOverlay(null);
        }
    }
}
