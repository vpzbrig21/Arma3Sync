package fr.soe.a3s.utils;

import javax.swing.filechooser.FileSystemView;
import java.io.File;

/**
 * Utility methods for retrieving common system paths without relying on
 * external libraries.
 */
public final class SystemPaths {

    private SystemPaths() {
    }

    /**
     * Returns the absolute path to the current user's desktop directory.
     */
    public static String getDesktopPath() {
        FileSystemView fsv = FileSystemView.getFileSystemView();
        File home = fsv.getHomeDirectory();
        return home.getAbsolutePath();
    }

    /**
     * Returns the absolute path to the current user's documents directory.
     */
    public static String getDocumentsPath() {
        FileSystemView fsv = FileSystemView.getFileSystemView();
        File docs = fsv.getDefaultDirectory();
        return docs.getAbsolutePath();
    }
}
