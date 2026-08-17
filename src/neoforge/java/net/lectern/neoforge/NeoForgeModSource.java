package net.lectern.neoforge;

import net.lectern.ModEntry;
import net.lectern.ModSource;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads the mod list out of FML.
 *
 * <p>Runs once. Everything it produces is a plain record, so nothing downstream holds an FML type
 * and nothing downstream has to ask FML anything again.
 */
public final class NeoForgeModSource implements ModSource {

    /**
     * Mods that are the platform rather than something installed on it.
     *
     * <p>Shown only when "include libraries" is on. A player scanning the list for the mod that is
     * misbehaving does not want the loader in the way, and these three are always present so they
     * carry no information.
     */
    private static final Set<String> PLATFORM = Set.of("minecraft", "neoforge", "fml", "javafml");

    /** Lets a mod declare itself a library, without Lectern having to guess. */
    private static final String LIBRARY_PROPERTY = "lectern:library";

    @Override
    public List<ModEntry> mods() {
        List<IModInfo> infos = ModList.get().getMods();
        List<ModEntry> out = new ArrayList<>(infos.size());

        for (IModInfo info : infos) {
            String id = info.getModId();
            Map<String, Object> props = info.getModProperties();

            out.add(ModEntry.of(
                    id,
                    info.getDisplayName(),
                    info.getVersion().toString(),
                    info.getDescription() == null ? "" : info.getDescription().trim(),
                    authorsOf(props),
                    info.getLogoFile().orElse(null),
                    info.getModURL().map(java.net.URL::toString).orElse(null),
                    issueUrlOf(info),
                    PLATFORM.contains(id) || Boolean.parseBoolean(string(props, LIBRARY_PROPERTY))
            ));
        }
        return out;
    }

    /**
     * {@return the issue tracker for the file this mod came from, or null}
     *
     * <p>{@code IModInfo} has {@code getModURL} but no issue URL: in mods.toml the tracker is
     * declared once per <em>file</em>, above the {@code [[mods]]} blocks, so a file holding several
     * mods gives all of them the same one. Read through the file's config rather than a typed
     * getter, because there is not one.
     */
    private static String issueUrlOf(IModInfo info) {
        var file = info.getOwningFile();
        if (file == null || file.getConfig() == null) {
            return null;
        }
        return file.getConfig().<String>getConfigElement("issueTrackerURL").orElse(null);
    }

    /**
     * {@return the authors, split on commas}
     *
     * <p>mods.toml has no list type for this -- everyone writes "Alice, Bob" into one string -- so
     * splitting is the only way to show them separately, and joining them back is the caller's
     * problem rather than something to guess at here.
     */
    private static List<String> authorsOf(Map<String, Object> props) {
        String authors = string(props, "authors");
        if (authors == null || authors.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>(2);
        for (String part : authors.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }

    private static String string(Map<String, Object> props, String key) {
        Object value = props.get(key);
        return value == null ? null : value.toString();
    }
}
