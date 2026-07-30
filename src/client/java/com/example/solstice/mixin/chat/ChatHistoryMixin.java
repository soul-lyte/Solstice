package com.example.solstice.mixin.chat;

import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Always-on "more chat history" - not a toggleable module, per explicit
 * request. Raises the chat message buffer from vanilla's 100 to 1000.
 *
 * <p>{@code ChatHud.MAX_MESSAGES} is a real {@code private static final
 * int} field but compiler-inlined at every use site (confirmed via
 * decompile - no field read, just a literal {@code 100}), so {@code
 * @ModifyConstant} on each real trim site is the right technique
 * regardless. The two actual trim sites (confirmed by decompiling the
 * whole class, not guessed) are both **private** {@code ChatHudLine}-taking
 * overloads - {@code addVisibleMessage(ChatHudLine)} (the wrapped/rendered
 * line buffer shown on screen) and the private {@code addMessage(ChatHudLine)}
 * (the raw, unwrapped message list {@code refresh()} rebuilds from on a
 * GUI-scale change) - **not** the two public {@code addMessage(Text, ...)}
 * overloads, which only construct a {@code ChatHudLine} and delegate to
 * these two; they never trim anything themselves. {@code
 * addToMessageHistory(String)} is a separate, unrelated buffer (the up/down
 * arrow chat *input* recall list, also capped at 100) - confirmed via
 * decompile to be its own method with its own unrelated {@code 100},
 * deliberately not touched; only the two methods named above are targeted,
 * so there's no risk of accidentally raising that one too.</p>
 */
@Mixin(ChatHud.class)
public abstract class ChatHistoryMixin {

    private static final int MAX_HISTORY = 1000;

    @ModifyConstant(method = "addVisibleMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V",
            constant = @Constant(intValue = 100))
    private int solstice$raiseVisibleMessagesCap(int original) {
        return MAX_HISTORY;
    }

    @ModifyConstant(method = "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V",
            constant = @Constant(intValue = 100))
    private int solstice$raiseMessagesCap(int original) {
        return MAX_HISTORY;
    }
}
