package net.lectern;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Lectern's own settings, in {@code config/lectern.properties}.
 *
 * <p>Plain properties rather than a config library. Lectern has four settings; a dependency to
 * store four booleans would cost more than it saves, and Cloth Config -- which does provide the
 * screen -- is optional, so the values have to be readable without it.
 *
 * <p>Read into fields at startup and written on save. Nothing here is on a draw path, but the
 * screen does consult {@link #showLibraries} when it opens, and a file read per open would be a
 * file read per open for no reason.
 */
public final class LecternConfig {

    private static final Path PATH = Paths.get("config", "lectern.properties");

    /** Show the loader and library mods in the list by default. */
    public static boolean showLibraries = false;

    /**
     * Take over the loader's Mods button rather than leaving it alone.
     *
     * <p>An escape hatch. Off means NeoForge's own mod list stays reachable and Lectern adds its
     * own button instead -- which is what somebody debugging a mod list problem will want.
     */
    public static boolean takeOverModsButton = true;

    /** Draw list rows tighter, fitting more mods on screen. */
    public static boolean compactRows = false;

    /** Keep the search text when the screen is reopened. */
    public static boolean rememberSearch = false;

    private LecternConfig() {}

    public static void load() {
        Properties props = new Properties();
        if (Files.isRegularFile(PATH)) {
            try (InputStream in = Files.newInputStream(PATH)) {
                props.load(in);
            } catch (IOException e) {
                Lectern.LOGGER.warn("Could not read {}; using defaults: {}", PATH, e.toString());
                return;
            }
        }
        showLibraries = bool(props, "show_libraries", showLibraries);
        takeOverModsButton = bool(props, "take_over_mods_button", takeOverModsButton);
        compactRows = bool(props, "compact_rows", compactRows);
        rememberSearch = bool(props, "remember_search", rememberSearch);
    }

    public static void save() {
        Properties props = new Properties();
        props.setProperty("show_libraries", Boolean.toString(showLibraries));
        props.setProperty("take_over_mods_button", Boolean.toString(takeOverModsButton));
        props.setProperty("compact_rows", Boolean.toString(compactRows));
        props.setProperty("remember_search", Boolean.toString(rememberSearch));
        try {
            Path parent = PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream out = Files.newOutputStream(PATH)) {
                props.store(out, "Lectern");
            }
        } catch (IOException e) {
            // Never fatal: the values are already in memory and apply to this session regardless.
            Lectern.LOGGER.warn("Could not write {}: {}", PATH, e.toString());
        }
    }

    private static boolean bool(Properties props, String key, boolean fallback) {
        String raw = props.getProperty(key);
        return raw == null ? fallback : Boolean.parseBoolean(raw.trim());
    }
}
