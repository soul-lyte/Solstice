package com.example.solstice.mixin.shulker;

import com.example.solstice.qol.shulker.ShulkerBoxTooltipModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Blocks every mouse input method on the whole screen while a locked shulker
 * preview is showing (not just within the overlay's own bounds) - per
 * explicit request that the locked preview sit "on top of everything else":
 * without this, clicking anywhere - including through the overlay's own
 * empty grid cells - always reached the real inventory slot underneath.
 * Releasing the Full Preview Key (which also unlocks, see {@code
 * ShulkerBoxTooltipModule}) restores normal interaction immediately.
 *
 * <p>The actual ticking and drawing of the locked preview lives in {@code
 * ShulkerLockTickMixin} instead (on {@code Screen.renderWithTooltip}, not
 * here) - see that class's own Javadoc for why it has to be there and not
 * on this class's own {@code render}.</p>
 */
@Mixin(net.minecraft.client.gui.screen.ingame.HandledScreen.class)
public abstract class HandledScreenLockOverlayMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 1)
    private void solstice$blockClickWhileLocked(CallbackInfoReturnable<Boolean> cir) {
        if (ShulkerBoxTooltipModule.getInstance().getLockedPreview().isPresent()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true, require = 1)
    private void solstice$blockDragWhileLocked(CallbackInfoReturnable<Boolean> cir) {
        if (ShulkerBoxTooltipModule.getInstance().getLockedPreview().isPresent()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true, require = 1)
    private void solstice$blockReleaseWhileLocked(CallbackInfoReturnable<Boolean> cir) {
        if (ShulkerBoxTooltipModule.getInstance().getLockedPreview().isPresent()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true, require = 1)
    private void solstice$blockScrollWhileLocked(CallbackInfoReturnable<Boolean> cir) {
        if (ShulkerBoxTooltipModule.getInstance().getLockedPreview().isPresent()) {
            cir.setReturnValue(true);
        }
    }
}
