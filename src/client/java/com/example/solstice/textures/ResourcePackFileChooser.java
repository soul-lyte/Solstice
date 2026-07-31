package com.example.solstice.textures;

import com.example.solstice.SolsticeMod;
import net.minecraft.client.MinecraftClient;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Native OS file/folder picker for the "Add your own" Presets card - a
 * resource pack is either a {@code .zip} file or an extracted folder, so
 * this allows picking either in one dialog. Runs the picker on its own
 * thread (Swing's own event loop, kept off Minecraft's render thread) and
 * marshals the result back onto the client thread via {@link
 * MinecraftClient#execute}, since only that thread should touch game state.
 *
 * <p>The dialog is parented to a throwaway, invisible, always-on-top
 * {@link JFrame} rather than {@code null} - real bug found and fixed: with a
 * {@code null} parent the chooser opened behind Minecraft's own window with
 * no way to bring it forward, so clicking "Add your own" looked like it
 * silently did nothing. That alone isn't enough on a real exclusive-fullscreen
 * window though - Windows gives an exclusive-fullscreen app special
 * always-frontmost compositor treatment that an ordinary "always on top" AWT
 * window can't override, so the dialog could still open completely hidden
 * behind the game (this project already hit the same class of bug earlier
 * this session, for GUI window automation, and worked around it the same
 * way: drop out of exclusive fullscreen first). This now temporarily toggles
 * the game out of fullscreen (if it's on) before showing the dialog and back
 * on afterward - windowed/borderless users are unaffected either way. Also
 * wrapped in a try/catch with real logging - previously any exception here
 * (AWT/Swing init racing the game's own GLFW window) would have been
 * swallowed on this background thread with no trace at all.</p>
 */
public final class ResourcePackFileChooser {

    private ResourcePackFileChooser() {}

    public static void pick(Consumer<Path> onPicked) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean wasFullscreen = client.getWindow().isFullscreen();
        if (wasFullscreen) {
            client.getWindow().toggleFullscreen();
        }

        Thread thread = new Thread(() -> {
            JFrame owner = new JFrame();
            try {
                owner.setUndecorated(true);
                owner.setAlwaysOnTop(true);
                owner.setSize(1, 1);
                owner.setLocationRelativeTo(null);
                owner.setVisible(true);
                owner.toFront();

                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Select a Resource Pack (folder or .zip)");
                chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                int result = chooser.showOpenDialog(owner);
                if (result == JFileChooser.APPROVE_OPTION) {
                    Path selected = chooser.getSelectedFile().toPath();
                    client.execute(() -> onPicked.accept(selected));
                }
            } catch (Exception e) {
                SolsticeMod.LOGGER.error("[Solstice] Resource pack file picker failed", e);
            } finally {
                owner.dispose();
                if (wasFullscreen) {
                    client.execute(() -> client.getWindow().toggleFullscreen());
                }
            }
        }, "Solstice-ResourcePackFileChooser");
        thread.setDaemon(true);
        thread.start();
    }
}
