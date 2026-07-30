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
 * {@code null} parent the chooser opened behind Minecraft's own window (which
 * defaults to always-on-top-capable borderless fullscreen) with no way to
 * bring it forward, so clicking "Add your own" looked like it silently did
 * nothing. Also wrapped in a try/catch with real logging - previously any
 * exception here (AWT/Swing init racing the game's own GLFW window) would
 * have been swallowed on this background thread with no trace at all.</p>
 */
public final class ResourcePackFileChooser {

    private ResourcePackFileChooser() {}

    public static void pick(Consumer<Path> onPicked) {
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
                    MinecraftClient.getInstance().execute(() -> onPicked.accept(selected));
                }
            } catch (Exception e) {
                SolsticeMod.LOGGER.error("[Solstice] Resource pack file picker failed", e);
            } finally {
                owner.dispose();
            }
        }, "Solstice-ResourcePackFileChooser");
        thread.setDaemon(true);
        thread.start();
    }
}
