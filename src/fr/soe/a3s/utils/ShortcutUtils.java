package fr.soe.a3s.utils;

import java.io.File;
import java.io.IOException;

/**
 * Helper for creating Windows shortcuts without external dependencies.
 */
public final class ShortcutUtils {

    private ShortcutUtils() {
    }

    /**
     * Creates a Windows shortcut (.lnk) using PowerShell. Works only on
     * Windows systems that have PowerShell installed.
     *
     * @param target   executable path
     * @param args     command line arguments
     * @param shortcut destination shortcut file
     */
    public static void createShortcut(String target, String args, File shortcut) throws IOException, InterruptedException {
        String script = String.join("",
                "$s=(New-Object -COM WScript.Shell).CreateShortcut('",
                shortcut.getAbsolutePath().replace("\\", "\\\\"), "');",
                "$s.TargetPath='", target.replace("\\", "\\\\"), "';",
                "$s.Arguments='", args.replace("'", "''"), "';",
                "$s.WorkingDirectory='", new File(target).getParent().replace("\\", "\\\\"), "';",
                "$s.Save()"
        );
        String command = "powershell -NoProfile -Command " + script;
        Process p = Runtime.getRuntime().exec(command);
        try {
            p.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }
}
