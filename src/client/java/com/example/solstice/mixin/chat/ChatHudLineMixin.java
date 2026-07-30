package com.example.solstice.mixin.chat;

import com.example.solstice.qol.chatheads.ChatHeadsModule;
import com.example.solstice.qol.chatheads.ChatSenderCarrier;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Attaches a sender UUID to each real {@code ChatHudLine} - a real signed
 * message's sender (captured by {@code ChatSenderTrackerMixin}) takes
 * priority, falling back to {@link ChatHeadsModule#scanForSender} for
 * messages without one (system messages, some servers' custom chat).
 * Mirrors dzwdz/chat_heads' {@code GuiMessageLineMixin}/{@code HeadData}
 * pattern, adapted to this project's own {@code ChatHudLine} shape
 * (MPL-2.0, see NOTICE.md).
 */
@Mixin(ChatHudLine.class)
public abstract class ChatHudLineMixin implements ChatSenderCarrier {

    @Unique
    private UUID solstice$senderUuid;

    @Override
    public UUID solstice$getSenderUuid() {
        return solstice$senderUuid;
    }

    @Override
    public void solstice$setSenderUuid(UUID uuid) {
        solstice$senderUuid = uuid;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void solstice$attachSender(int creationTick, Text content, MessageSignatureData signature,
                                        MessageIndicator indicator, CallbackInfo ci) {
        if (!ChatHeadsModule.getInstance().isEnabled()) {
            return;
        }
        UUID sender = ChatHeadsModule.consumePendingMessageSender();
        if (sender == null) {
            sender = ChatHeadsModule.getInstance().scanForSender(content.getString());
        }
        solstice$senderUuid = sender;
    }
}
