package com.example.solstice.mixin.chat;

import com.example.solstice.qol.chatheads.ChatHeadsModule;
import com.example.solstice.qol.chatheads.ChatSenderCarrier;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Two independent hooks on {@code ChatHud} for Chat Heads (adapted from
 * dzwdz/chat_heads, MPL-2.0, see NOTICE.md):
 *
 * <p>1. {@code addVisibleMessage} stages the owning message's sender into
 * {@code ChatHeadsModule.pendingLineSender} right before it wraps the
 * message into one or more {@code ChatHudLine.Visible} lines - see {@code
 * ChatHudLineVisibleMixin} for the consuming side. Mirrors dzwdz/chat_heads'
 * {@code ChatComponentMixin2.chatheads$transferMessageOwner}.</p>
 *
 * <p>2. The real, public {@code render(DrawContext, ...)} overload
 * captures the live {@code DrawContext} into {@code
 * ChatHeadsModule.currentRenderContext} for the duration of the render
 * call, so {@code ChatLineConsumerMixin} (nested deep inside private
 * rendering internals with no {@code DrawContext} of its own) has
 * something to actually draw the head texture with - mirrors
 * dzwdz/chat_heads' {@code ChatComponentMixin.chatheads$captureGuiGraphics}/
 * {@code chatheads$forgetGraphics}.</p>
 */
@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

    @Inject(method = "addVisibleMessage", at = @At("HEAD"))
    private void solstice$stagePendingLineSender(ChatHudLine message, CallbackInfo ci) {
        if (!ChatHeadsModule.getInstance().isEnabled()) {
            ChatHeadsModule.setPendingLineSender(null);
            return;
        }
        ChatHeadsModule.setPendingLineSender(((ChatSenderCarrier) (Object) message).solstice$getSenderUuid());
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V",
            at = @At("HEAD"))
    private void solstice$captureContext(DrawContext context, TextRenderer textRenderer, int currentTick,
                                          int mouseX, int mouseY, boolean interactable, boolean bl, CallbackInfo ci) {
        ChatHeadsModule.currentRenderContext = context;
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V",
            at = @At("RETURN"))
    private void solstice$releaseContext(DrawContext context, TextRenderer textRenderer, int currentTick,
                                          int mouseX, int mouseY, boolean interactable, boolean bl, CallbackInfo ci) {
        ChatHeadsModule.currentRenderContext = null;
    }
}
