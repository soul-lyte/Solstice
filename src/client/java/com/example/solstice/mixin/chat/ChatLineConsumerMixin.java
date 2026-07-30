package com.example.solstice.mixin.chat;

import com.example.solstice.qol.chatheads.ChatHeadsModule;
import com.example.solstice.qol.chatheads.ChatSenderCarrier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.UUID;

/**
 * The actual "draw head, shift text right, shift back" trick - ported
 * (MPL-2.0, see NOTICE.md) from dzwdz/chat_heads' {@code
 * ChatComponentInnerMixin} onto this project's own
 * anonymous {@code ChatHud$1} (the real per-visible-line {@code
 * LineConsumer} anonymous class {@code ChatHud.render} builds inline -
 * confirmed via {@code javap}, not guessed; {@code field_63870} is its
 * captured {@code ChatHud.Backend} field, an unmapped synthetic capture
 * with no real Yarn name, same class of situation as this project's other
 * previously-encountered {@code method_XXXXX}/{@code field_XXXXX} cases).
 *
 * <p>Split into a HEAD-inject (computes the offset/sender from the real
 * {@code accept(Visible, int, float)} parameters, which a plain {@code
 * @ModifyArgs} handler doesn't receive) and a {@code @ModifyArgs} handler
 * on the {@code Backend.text(...)} call itself (which does receive the
 * real {@code y} argument) - avoids relying on MixinExtras' {@code @Local}
 * sugar (the original mod's own technique), since this project's
 * build.gradle doesn't declare MixinExtras as a compile dependency.</p>
 */
@Mixin(targets = "net.minecraft.client.gui.hud.ChatHud$1")
public abstract class ChatLineConsumerMixin {

    @Shadow
    @Final
    private ChatHud.Backend field_63870;

    @Unique
    private int solstice$chatOffset;

    @Unique
    private UUID solstice$pendingSender;

    @Inject(method = "accept", at = @At("HEAD"))
    private void solstice$onAccept(ChatHudLine.Visible visible, int ix, float fx, CallbackInfo ci) {
        solstice$chatOffset = 0;
        solstice$pendingSender = null;
        if (!ChatHeadsModule.getInstance().isEnabled()) {
            return;
        }
        UUID sender = ((ChatSenderCarrier) (Object) visible).solstice$getSenderUuid();
        if (sender != null) {
            solstice$pendingSender = sender;
            solstice$chatOffset = ChatHeadsModule.HEAD_WIDTH;
        }
    }

    @ModifyArgs(method = "accept", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/hud/ChatHud$Backend;text(IFLnet/minecraft/text/OrderedText;)Z"))
    private void solstice$beforeText(Args args) {
        if (solstice$chatOffset <= 0) {
            return;
        }
        int y = args.get(0);
        float opacity = args.get(1);
        DrawContext context = ChatHeadsModule.currentRenderContext;
        if (context != null && solstice$pendingSender != null) {
            ChatHeadsModule.getInstance().drawHead(context, solstice$pendingSender, 0, y, opacity);
        }
        field_63870.updatePose(matrix -> matrix.translate(solstice$chatOffset, 0));
    }

    @Inject(method = "accept", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/hud/ChatHud$Backend;text(IFLnet/minecraft/text/OrderedText;)Z",
            shift = At.Shift.AFTER))
    private void solstice$afterText(ChatHudLine.Visible visible, int ix, float fx, CallbackInfo ci) {
        if (solstice$chatOffset > 0) {
            field_63870.updatePose(matrix -> matrix.translate(-solstice$chatOffset, 0));
        }
    }
}
