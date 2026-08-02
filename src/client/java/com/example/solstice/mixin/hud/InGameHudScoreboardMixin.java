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
 * the actual draw calls themselves, leaves their own coordinates untouched,
 * and instead wraps the whole thing in one matrix transform. Robust because
 * it only depends on each call's own method descriptor (plus an ordinal to
 * tell the two identical {@code fill} calls apart), never on local-slot
 * numbering.
 *
 * <p>The first {@code fill} call (the header background bar) is always the
 * top-left corner of the whole panel, at vanilla's own natural position -
 * confirmed via decompile: {@code fill(q - 2, u - 9 - 1, r, u - 1, t)}, so
 * undoing that {@code -2}/{@code -9-1}/{@code +1} recovers vanilla's own
 * {@code q}/{@code u} anchor without ever touching a local variable
 * directly. The second {@code fill} call (the body background, confirmed
 * via decompile to be a single call spanning every row, not one per row)
 * gives the real total height, which the header call alone can't - varies
 * with the live entry count, not knowable that early. Resizing still
 * scales both axes uniformly off the header's width figure (not
 * pixel-perfect for every entry count, but real: growing the box's stored
 * width does grow the whole panel, text included, not just reposition it).</p>
 *
 * <p><b>Stored X/Y/width are offsets from the live natural anchor, not
 * absolute targets</b> - see {@link com.example.solstice.core.hud.HudElement#hasLiveNaturalAnchor()}.
 * The transform below applies the offset as a plain, unscaled outer
 * translate (first call, so it's outermost - JOML's {@code
 * Matrix3x2fStack} composes each subsequent call as an additional inner
 * transform, confirmed by this project's own established
 * translate/scale/translate-back pattern already scaling correctly around
 * the natural anchor), specifically so the offset itself is never affected
 * by the resize scale - a fixed "moved 10px left of natural" stays exactly
 * that on every server, instead of also growing/shrinking by whatever the
 * current resize scale happens to be.</p>
 *
 * <p>No longer skips its own body (the old HEAD-cancel) when the element is
 * toggled off - {@link com.example.solstice.ui.ScoreboardHudElement}'s live
 * bounds cache needs this to keep running (and {@link
 * com.example.solstice.ui.ScoreboardHudElement#recordRealNaturalBounds}
 * called) even while hidden, so the HUD editor still knows exactly where a
 * currently-hidden scoreboard would be without needing to re-enable it
 * first. {@link #solstice$hidden} instead individually gates each actual
 * draw call, leaving the position/size math (and the bounds capture it
 * feeds) running unconditionally.</p>
 */
@Mixin(InGameHud.class)
public abstract class InGameHudScoreboardMixin {

    private static final String TARGET = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V";

    @Unique private boolean solstice$hidden;
    @Unique private int solstice$naturalTopY;
    @Unique private int solstice$naturalX;
    @Unique private int solstice$naturalY;
    @Unique private int solstice$naturalW;

    @Inject(method = TARGET, at = @At("HEAD"))
    private void solstice$checkHidden(DrawContext context, net.minecraft.scoreboard.ScoreboardObjective objective, CallbackInfo ci) {
        HudLayoutManager layout = HudLayoutManager.getInstance();
        this.solstice$hidden = !layout.isMasterVisible() || !layout.isVisible("scoreboard", true);
    }

    @Redirect(method = TARGET, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V", ordinal = 0))
    private void solstice$onHeaderFill(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        this.solstice$naturalTopY = y1;
        this.solstice$naturalX = x1 + 2;
        this.solstice$naturalY = y1 + 10;
        this.solstice$naturalW = x2 - x1;

        HudLayoutManager layout = HudLayoutManager.getInstance();
        int offsetX = layout.getOffsetX("scoreboard", 0);
        int offsetY = layout.getOffsetY("scoreboard", 0);
        int offsetW = layout.getOffsetWidth("scoreboard", 0);
        int effectiveW = this.solstice$naturalW + offsetW;
        float scale = this.solstice$naturalW > 0 ? (float) effectiveW / this.solstice$naturalW : 1f;

        context.getMatrices().pushMatrix();
        // Outermost - applied last, so the offset itself is never scaled (see class Javadoc).
        context.getMatrices().translate(offsetX, offsetY);
        context.getMatrices().translate(this.solstice$naturalX, this.solstice$naturalY);
        context.getMatrices().scale(scale, scale);
        context.getMatrices().translate(-this.solstice$naturalX, -this.solstice$naturalY);

        if (!this.solstice$hidden) {
            context.fill(x1, y1, x2, y2, color);
        }
    }

    @Redirect(method = TARGET, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V", ordinal = 1))
    private void solstice$onBodyFill(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        int naturalH = y2 - this.solstice$naturalTopY;
        com.example.solstice.ui.ScoreboardHudElement.recordRealNaturalBounds(
                this.solstice$naturalX, this.solstice$naturalY, this.solstice$naturalW, naturalH);

        if (!this.solstice$hidden) {
            context.fill(x1, y1, x2, y2, color);
        }
    }

    @Redirect(method = TARGET, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)V"))
    private void solstice$onDrawText(DrawContext context, TextRenderer renderer, Text text, int x, int y, int color, boolean shadow) {
        if (!this.solstice$hidden) {
            context.drawText(renderer, text, x, y, color, shadow);
        }
    }

    @Inject(method = TARGET, at = @At("RETURN"))
    private void solstice$popMatrix(DrawContext context, net.minecraft.scoreboard.ScoreboardObjective objective, CallbackInfo ci) {
        context.getMatrices().popMatrix();
    }
}
