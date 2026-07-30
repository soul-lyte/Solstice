package com.example.solstice.viewdistance;

import com.google.common.net.PercentEscaper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * Ported from Johni0702/bobby (LGPL-3.0-only, see NOTICE.md).
 */
public class FileSystemUtils {
    private static final PercentEscaper FALLBACK_NAME_ENCODER = new PercentEscaper(".-_ ", false);

    public static Path resolveSafeDirectoryName(Path parent, String name) {
        if (!name.contains("%") && isUsable(parent, name)) {
            return parent.resolve(name);
        } else {
            return parent.resolve(FALLBACK_NAME_ENCODER.escape(name));
        }
    }

    private static boolean isUsable(Path parent, String name) {
        if (name.contains(parent.getFileSystem().getSeparator())) {
            return false;
        }

        Path path;
        try {
            path = parent.resolve(name);
        } catch (InvalidPathException e) {
            return false;
        }
        if (Files.exists(path)) {
            return true;
        }

        try {
            Files.createDirectory(path);
        } catch (IOException e) {
            return false;
        }

        try {
            Files.delete(path);
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public static Path resolveChild(Path parent, String child) {
        Path childPath = parent.resolve(child);
        if (!childPath.normalize().startsWith(parent.normalize())) {
            return parent.resolve(FALLBACK_NAME_ENCODER.escape(child));
        }
        return childPath;
    }
}
