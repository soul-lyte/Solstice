package com.example.solstice.textures;

import com.example.solstice.SolsticeMod;
import net.minecraft.client.MinecraftClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Native OS file picker for the "Add your own" Presets card - a real Windows
 * file-open dialog, not a Java Swing one.
 *
 * <p><b>Real history, why this isn't a Swing {@code JFileChooser} anymore</b>:
 * it was, twice - first with a real, fixed bug (the dialog opening hidden
 * behind an exclusive-fullscreen window), then with a second real, fixed bug
 * ({@code new JFrame()} sitting outside the try/catch, silently swallowing
 * any AWT/Swing init exception). Both fixes were real and correct, but the
 * button still didn't work afterward - confirmed by the drag-and-drop path
 * (a completely different, GLFW-native mechanism, see {@code
 * SolsticeScreen#onFilesDropped}) working fine the whole time. That points at
 * AWT/Swing itself being unreliable to initialize inside this specific
 * LWJGL/GLFW-hosted game process, for a reason neither fix could reach since
 * it's in the JVM's own AWT toolkit bootstrap, not this project's code.
 * Rather than keep patching the same broken approach, this spawns a real
 * native Windows file dialog ({@code System.Windows.Forms.OpenFileDialog})
 * in a completely separate {@code powershell.exe} process - its own clean
 * environment, no shared AWT state with the game's JVM at all.</p>
 *
 * <p><b>Scope note</b>: {@code OpenFileDialog} is a file picker, not a
 * combined file-or-folder picker the way Swing's {@code JFileChooser} could
 * be configured - Windows' native dialogs don't have an equivalent unified
 * mode. Scoped to {@code .zip} files here (the common case for a downloaded
 * pack); an extracted folder pack is still fully supported via drag-and-drop
 * onto the Textures tab, which already handles folders correctly.</p>
 */
public final class ResourcePackFileChooser {

    private static final String SCRIPT = """
            Add-Type -AssemblyName System.Windows.Forms
            $dialog = New-Object System.Windows.Forms.OpenFileDialog
            $dialog.Title = 'Select a Resource Pack (.zip) - or drag a folder pack onto the Textures tab instead'
            $dialog.Filter = 'Resource Pack (*.zip)|*.zip|All files (*.*)|*.*'
            $dialog.Multiselect = $false
            $result = $dialog.ShowDialog()
            if ($result -eq [System.Windows.Forms.DialogResult]::OK) {
                Write-Output $dialog.FileName
            }
            """;

    private ResourcePackFileChooser() {}

    public static void pick(Consumer<Path> onPicked) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean wasFullscreen = client.getWindow().isFullscreen();
        if (wasFullscreen) {
            client.getWindow().toggleFullscreen();
        }

        Thread thread = new Thread(() -> {
            SolsticeMod.LOGGER.info("[Solstice] Resource pack picker: launching native dialog process");
            try {
                Process process = new ProcessBuilder(
                        "powershell.exe", "-NoProfile", "-STA", "-WindowStyle", "Hidden", "-Command", SCRIPT)
                        .redirectErrorStream(true)
                        .start();

                String output;
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    output = reader.lines().reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
                }
                int exitCode = process.waitFor();
                SolsticeMod.LOGGER.info("[Solstice] Resource pack picker: process exited with code {}", exitCode);

                String selectedPath = output.strip();
                if (!selectedPath.isEmpty()) {
                    Path selected = Path.of(selectedPath);
                    client.execute(() -> onPicked.accept(selected));
                } else {
                    SolsticeMod.LOGGER.info("[Solstice] Resource pack picker: no file selected (cancelled or empty output)");
                }
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                SolsticeMod.LOGGER.error("[Solstice] Resource pack file picker failed", e);
            } finally {
                if (wasFullscreen) {
                    client.execute(() -> client.getWindow().toggleFullscreen());
                }
            }
        }, "Solstice-ResourcePackFileChooser");
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler((t, e) ->
                SolsticeMod.LOGGER.error("[Solstice] Resource pack picker thread died uncaught", e));
        thread.start();
    }
}
