package com.example.solstice.mixin.hud;

import com.example.solstice.core.hud.HudLayoutManager;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Repositions/resizes the real vanilla scoreboard sidebar instead of
 * reimplementing it. Unlike {@code BossBarHudMixin}, this doesn't cancel
 * and redraw from scratch - {@code InGameHud.renderScoreboardSidebar(
 * DrawContext, ScoreboardObjective)}'s real body (confirmed via decompile)
 * has no explicit x/y overload; every position is a local expression
 * derived from {@code context.getScaledWindowWidth/Height()} inline at
 * each {@code fill}/{@code drawText} call site, not a clean parameter -
 * so instead of guessing local-variable slot ordinals (fragile - this
 * method reuses several int slots for unrelated values partway through,
 * confirmed via a full {@code javap -c -l} bytecode read), this redirects
 * the actual draw calls themselves and shifts/scales their already-computed
 * coordinates. Robust because it only depends on each call's own method
 * descriptor (plus an ordinal to tell the two identical {@code fill} calls
 * apart), never on local-slot numbering.
 *
 * <p>The first {@code fill} call (the header background bar) is always the
 * top-left corner of the whole panel, at vanilla's own natural position -
 * confirmed via decompile: {@code fill(q - 2, u - 9 - 1, r, u - 1, t)}, so
 * undoing that {@code -2}/{@code -9-1}/{@code +1} recovers vanilla's own
 * {@code q}/{@code u} anchor without ever touching a local variable
 * directly. That header call's own width ({@code x2 - x1}) is also the
 * *only* natural-size number available without deeper hooks - real total
 * height varies with the number of scoreboard entries, which isn't known
 * this early - so resizing scales both axes uniformly off that one width
 * figure. A reasonable simplification, not pixel-perfect for every entry
 * count, but real: growing the box's stored width does grow the whole
 * panel (text included), not just reposition it.</p>
 */
@Mixin(InGameHud.class)
public abstract class InGameHudScoreboardMixin {

    private static final String TARGET = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V";

    @Unique private int solstice$deltaX;
    @Unique private int solstice$deltaY;
    @Unique private float solstice$scale = 1f;

    /** Matches {@code BossBarHudMixin}'s "hidden means fully invisible" behavior. */
    @Inject(method = TARGET, at = @At("HEAD"), cancellable = true)
    private void solstice$hideIfDisabled(DrawContext context, net.minecraft.scoreboard.ScoreboardObjective objective, CallbackInfo ci) {
        HudLayoutManager layout = HudLayoutManager.getInstance();
        if (!layout.isMasterVisible() || !layout.isVisible("scoreboard", true)) {
            ci.cancel();
        }
    }

    @Redirect(method = TARGET, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V", ordinal = 0))
    private void solstice$onHeaderFill(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        int naturalX = x1 + 2;
        int naturalY = y1 + 10;
        int naturalW = x2 - x1;

        // So the HUD editor's draggable box can use the same real, content-dependent
        // baseline this Mixin computes its own delta from - see ScoreboardHudElement's
        // own Javadoc for the bug this fixes.
        com.example.solstice.ui.ScoreboardHudElement.recordRealNaturalBounds(naturalX, naturalY, naturalW);

        int storedX = HudLayoutManager.getInstance().getX("scoreboard", naturalX);
        int storedY = HudLayoutManager.getInstance().getY("scoreboard", naturalY);
        int storedW = HudLayoutManager.getInstance().getWidth("scoreboard", naturalW);

        this.solstice$scale = naturalW > 0 ? (float) storedW / naturalW : 1f;
        this.solstice$deltaX = storedX - naturalX;
        this.solstice$deltaY = storedY - naturalY;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(naturalX, naturalY);
        context.getMatrices().scale(this.solstice$scale, this.solstice$scale);
        context.getMatrices().translate(-naturalX, -naturalY);

        context.fill(x1 + this.solstice$deltaX, y1 + this.solstice$deltaY,
                x2 + this.solstice$deltaX, y2 + this.solstice$deltaY, color);
    }

    @Redirect(method = TARGET, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V", ordinal = 1))
    private void solstice$onBodyFill(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        context.fill(x1 + this.solstice$deltaX, y1 + this.solstice$deltaY,
                x2 + this.solstice$deltaX, y2 + this.solstice$deltaY, color);
    }

    @Redirect(method = TARGET, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)V"))
    private void solstice$onDrawText(DrawContext context, TextRenderer renderer, Text text, int x, int y, int color, boolean shadow) {
        context.drawText(renderer, text, x + this.solstice$deltaX, y + this.solstice$deltaY, color, shadow);
    }

    @Inject(method = TARGET, at = @At("RETURN"))
    private void solstice$popMatrix(DrawContext context, net.minecraft.scoreboard.ScoreboardObjective objective, CallbackInfo ci) {
        context.getMatrices().popMatrix();
    }
}
