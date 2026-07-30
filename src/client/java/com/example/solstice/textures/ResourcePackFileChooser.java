package com.example.solstice.textures;

import net.minecraft.client.MinecraftClient;

import javax.swing.JFileChooser;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Native OS file/folder picker for the "Add your own" Presets card - a
 * resource pack is either a {@code .zip} file or an extracted folder, so
 * this allows picking either in one dialog. Runs the picker on its own
 * thread (Swing's own event loop, kept off Minecraft's render thread) and
 * marshals the result back onto the client thread via {@link
 * MinecraftClient#execute}, since only that thread should touch game state.
 */
public final class ResourcePackFileChooser {

    private ResourcePackFileChooser() {}

    public static void pick(Consumer<Path> onPicked) {
        Thread thread = new Thread(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select a Resource Pack (folder or .zip)");
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            int result = chooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                Path selected = chooser.getSelectedFile().toPath();
                MinecraftClient.getInstance().execute(() -> onPicked.accept(selected));
            }
        }, "Solstice-ResourcePackFileChooser");
        thread.setDaemon(true);
        thread.start();
    }
}
