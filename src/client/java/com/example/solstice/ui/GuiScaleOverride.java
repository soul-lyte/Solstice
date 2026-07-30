package com.example.solstice.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;

/**
 * Forces every Solstice screen to render as if the real GUI Scale option
 * were fixed at {@link #TARGET_SCALE}, regardless of what the user's own
 * setting actually is (vanilla default is 4) - purely cosmetic, more
 * breathing room for the card grid, never touches the persisted {@code
 * options.txt} value.
 *
 * <p>{@code Window.setScaleFactor(int)} (confirmed via decompile) is a
 * pure, self-contained state update - it only recomputes {@code
 * scaledWidth}/{@code scaledHeight} from the existing framebuffer size, no
 * other side effects (no GL resource reallocation, no texture atlas
 * rebuild) - so temporarily overriding it while a Solstice screen is open
 * is safe. Detection is by package name ({@code com.example.solstice.ui})
 * rather than a marker interface, so every current and future Solstice
 * screen is covered automatically with no per-screen changes needed.</p>
 *
 * <p><b>Restoring on exit does NOT re-apply a snapshot taken at entry</b> -
 * confirmed via decompile that {@code SolsticeVideoSettingsScreen} (itself
 * a Solstice-package screen, so this override is active while it's open)
 * lets the user change the real {@code GameOptions.guiScale} option live,
 * whose change-callback is {@code value -> this.client.onResolutionChanged()}
 * - which immediately recomputes and applies the real window scale factor
 * from the new option value. A snapshot captured at entry would go stale
 * the moment that happens, and restoring it on exit would silently discard
 * the user's own change (this was a real, reported bug - changing GUI
 * Scale from inside Solstice's Video Settings appeared to do nothing until
 * toggled twice). Calling {@code client.onResolutionChanged()} on exit
 * instead re-derives the correct value fresh from whatever the option
 * currently says, exactly like vanilla does after any real option change
 * or window resize - so it's always correct, never stale.</p>
 *
 * <p>Driven from {@code SolsticeClient}'s existing client-tick handler via
 * {@link #tick(MinecraftClient)} - edge-triggered (only acts on the actual
 * transition into/out of a Solstice screen), not re-applied every tick,
 * so an open text field or scroll position never gets reset by this.</p>
 */
public final class GuiScaleOverride {

    private static final int TARGET_SCALE = 3;

    private static boolean active = false;

    private GuiScaleOverride() {}

    public static void tick(MinecraftClient client) {
        Screen current = client.currentScreen;
        boolean shouldOverride = current != null
                && current.getClass().getPackageName().startsWith("com.example.solstice.ui");

        if (shouldOverride && !active) {
            activate(client);
        } else if (!shouldOverride && active) {
            deactivate(client);
        }
    }

    private static void activate(MinecraftClient client) {
        active = true;
        Window window = client.getWindow();
        window.setScaleFactor(TARGET_SCALE);
        if (client.currentScreen != null) {
            client.currentScreen.resize(window.getScaledWidth(), window.getScaledHeight());
        }
    }

    private static void deactivate(MinecraftClient client) {
        active = false;
        client.onResolutionChanged();
    }
}
