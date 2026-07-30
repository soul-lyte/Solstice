package com.example.solstice.mixin.optimization;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Adapted from ImmediatelyFast's {@code fast_text_lookup} mixin - RaphiMC,
 * LGPL-3.0-only, see NOTICE.md and licenses/LICENSE-LGPL-3.0.txt.
 *
 * <p>Ported onto 1.21.11's real class (confirmed via decompile - Yarn calls
 * the per-glyph render callback {@code TextRenderer.GlyphDrawer}, not
 * Mojmap's {@code Font.GlyphVisitor}; its anonymous implementation - the real
 * class {@code TextRenderer$GlyphDrawer$1}, confirmed to exist via javap -
 * has a private {@code draw(TextDrawable)} method calling {@code
 * VertexConsumerProvider.getBuffer(RenderLayer)} once per glyph/rectangle
 * drawn). Consecutive glyphs in the same text run almost always share the
 * same render layer (same font/style), so {@code getBuffer} - a real lookup,
 * not free - gets called redundantly with an identical argument, over and
 * over, for every character of every string rendered (chat, GUI, nameplates,
 * tooltips, the F3 screen). Caching the last (layer, buffer) pair per
 * drawer instance and skipping the redundant lookup when the layer hasn't
 * changed is output-identical - {@code getBuffer} is deterministic and
 * side-effect-free for a fixed buffer-provider instance within one frame.</p>
 */
@Mixin(targets = "net.minecraft.client.font.TextRenderer$GlyphDrawer$1")
public abstract class GlyphDrawerBufferCacheMixin {

    @Unique
    private RenderLayer solstice$lastRenderLayer;
    @Unique
    private VertexConsumer solstice$lastVertexConsumer;

    @Redirect(method = "draw", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/VertexConsumerProvider;getBuffer(Lnet/minecraft/client/render/RenderLayer;)Lnet/minecraft/client/render/VertexConsumer;"))
    private VertexConsumer solstice$reuseBufferForSameLayer(VertexConsumerProvider instance, RenderLayer layer) {
        if (this.solstice$lastRenderLayer == layer) {
            return this.solstice$lastVertexConsumer;
        }
        this.solstice$lastRenderLayer = layer;
        this.solstice$lastVertexConsumer = instance.getBuffer(layer);
        return this.solstice$lastVertexConsumer;
    }
}
