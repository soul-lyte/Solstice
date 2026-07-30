package com.example.solstice.mixin.chat;

import com.example.solstice.qol.chatheads.ChatHeadsModule;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stages the real sender's UUID for {@link ChatHeadsModule}, consumed by
 * {@code ChatHudLineMixin} the moment the corresponding {@code
 * ChatHudLine} is constructed - confirmed real signature via decompile:
 * {@code MessageHandler.onChatMessage(SignedMessage, GameProfile,
 * MessageType.Parameters)} is the actual per-message entry point carrying
 * the real sender profile, called synchronously on the same call stack
 * that eventually reaches {@code ChatHud.addMessage} (a plain HEAD inject,
 * non-cancelling - this only observes, never alters chat delivery).
 */
@Mixin(MessageHandler.class)
public abstract class ChatSenderTrackerMixin {

    @Inject(method = "onChatMessage", at = @At("HEAD"))
    private void solstice$trackSender(SignedMessage message, GameProfile sender, MessageType.Parameters params, CallbackInfo ci) {
        if (ChatHeadsModule.getInstance().isEnabled()) {
            ChatHeadsModule.setPendingMessageSender(sender.id());
        }
    }
}
