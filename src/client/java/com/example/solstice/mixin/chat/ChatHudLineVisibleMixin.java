package com.example.solstice.mixin.chat;

import com.example.solstice.qol.chatheads.ChatHeadsModule;
import com.example.solstice.qol.chatheads.ChatSenderCarrier;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Carries the sender UUID onto the specific wrapped visual line that
 * should actually get the head - {@code ChatHudMixin} stages the owning
 * message's sender into {@code ChatHeadsModule.pendingLineSender} once per
 * {@code addVisibleMessage} call, and only the FIRST {@code
 * ChatHudLine.Visible} constructed from that call consumes it (this
 * constructor clears it immediately after reading), so a wrapped
 * multi-line message only gets one head, on its first line - mirrors
 * dzwdz/chat_heads' {@code GuiMessageLineMixin} exactly (MPL-2.0, see
 * NOTICE.md).
 */
@Mixin(ChatHudLine.Visible.class)
public abstract class ChatHudLineVisibleMixin implements ChatSenderCarrier {

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
    private void solstice$attachSender(int addedTime, OrderedText content, MessageIndicator indicator,
                                        boolean endOfEntry, CallbackInfo ci) {
        solstice$senderUuid = ChatHeadsModule.consumePendingLineSender();
    }
}
