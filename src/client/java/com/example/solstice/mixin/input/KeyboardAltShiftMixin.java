package com.example.solstice.mixin.input;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops Shift/Alt key events entirely while any Solstice-package screen is
 * open (detected by package name, same technique as {@code GuiScaleOverride}
 * - covers every current and future Solstice screen automatically), per
 * explicit request to stop Windows' own Alt+Shift keyboard-layout-switch
 * shortcut from firing while using Solstice's menus.
 *
 * <p><b>Real limitation, disclosed up front</b>: that Windows shortcut is
 * recognized by a system-wide hook below the game's own input handling -
 * cancelling the event here only stops Minecraft's own reaction to it
 * (keybindings, text input, etc.), it cannot reach into the OS and block
 * the shortcut itself. That would need a native low-level keyboard hook
 * (a new platform-specific dependency), which was deliberately not added.
 * This is the best-effort, Minecraft-only mitigation, chosen explicitly
 * over that heavier option.</p>
 *
 * <p>Vanilla's {@code ChatScreen} lives outside {@code com.example.solstice.ui}
 * entirely, so it's naturally exempt with no special-case needed - matches
 * the explicit "every GUI except chat" request. Doesn't affect the Shulker/
 * Chest Tooltip preview's own hold-keys either, since those are polled
 * directly off the window's raw key state ({@code InputUtil.isKeyPressed}),
 * bypassing this event path entirely, and that preview only ever shows
 * while a vanilla container screen (not a Solstice one) is open anyway.</p>
 */
@Mixin(Keyboard.class)
public abstract class KeyboardAltShiftMixin {

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void solstice$dropAltShiftInSolsticeScreens(long window, int action, KeyInput input, CallbackInfo ci) {
        Screen current = MinecraftClient.getInstance().currentScreen;
        if (current == null) {
            return;
        }
        if (!current.getClass().getPackageName().startsWith("com.example.solstice.ui")) {
            return;
        }

        int key = input.key();
        if (key == GLFW.GLFW_KEY_LEFT_SHIFT || key == GLFW.GLFW_KEY_RIGHT_SHIFT
                || key == GLFW.GLFW_KEY_LEFT_ALT || key == GLFW.GLFW_KEY_RIGHT_ALT) {
            ci.cancel();
        }
    }
}
