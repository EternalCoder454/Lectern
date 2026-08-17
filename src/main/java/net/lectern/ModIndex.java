package net.lectern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The mod list, and the search over it.
 *
 * <p>Built once from a {@link ModSource} and then read-only. Everything the screen does goes
 * through here, and the screen is redrawn every frame it is open, so every method below is written
 * on the assumption that it will be called far more often than it needs to be.
 *
 * <h2>How search avoids allocating</h2>
 *
 * <p>Each {@link ModEntry} carries its searchable fields pre-joined and pre-lowercased. A query
 * therefore costs one {@code toLowerCase} for the query itself plus a {@code contains} per mod --
 * no per-mod string building, no regex, no streams. On a pack with three hundred mods that is the
 * difference between a search box that keeps up with typing and one that does not.
 *
 * <p>Results are written into a caller-owned list rather than returned as a new one, so holding the
 * search box open does not allocate a list per keystroke.
 */
public final class ModIndex {

    private final List<ModEntry> all;
    private final List<ModEntry> visible;

    private ModIndex(List<ModEntry> all) {
        this.all = List.copyOf(all);
        List<ModEntry> notLibraries = new ArrayList<>(all.size());
        for (ModEntry entry : all) {
            if (!entry.library()) {
                notLibraries.add(entry);
            }
        }
        this.visible = List.copyOf(notLibraries);
    }

    /**
     * {@return an index over everything {@code source} reports, sorted by display name}
     *
     * <p>Sorted here, once. The screen never sorts: re-sorting a few hundred entries per frame is
     * exactly the kind of thing that makes a menu feel heavy for no reason anyone can point at.
     */
    public static ModIndex build(ModSource source) {
        List<ModEntry> entries = new ArrayList<>(source.mods());
        entries.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return new ModIndex(entries);
    }

    /** {@return every mod, libraries included} */
    public List<ModEntry> all() {
        return all;
    }

    /** {@return every mod that is not a library} */
    public List<ModEntry> visible() {
        return visible;
    }

    /**
     * Fills {@code out} with the entries matching {@code query}.
     *
     * <p>{@code out} is cleared first and never reallocated by this method, which is why it is a
     * parameter rather than a return value.
     *
     * @param query           what the player typed; blank matches everything
     * @param includeLibraries whether to search libraries as well
     * @param out             destination, owned by the caller
     */
    public void search(String query, boolean includeLibraries, List<ModEntry> out) {
        out.clear();
        List<ModEntry> pool = includeLibraries ? all : visible;

        if (query == null || query.isBlank()) {
            out.addAll(pool);
            return;
        }

        String needle = query.trim().toLowerCase(Locale.ROOT);
        for (int i = 0, n = pool.size(); i < n; i++) {
            ModEntry entry = pool.get(i);
            if (entry.searchText().contains(needle)) {
                out.add(entry);
            }
        }
    }

    /** {@return the entry with this id, or null} */
    public ModEntry byId(String id) {
        for (int i = 0, n = all.size(); i < n; i++) {
            ModEntry entry = all.get(i);
            if (entry.id().equals(id)) {
                return entry;
            }
        }
        return null;
    }

    /** {@return how many mods are installed, libraries included} */
    public int count() {
        return all.size();
    }

    /** An empty index, for before the loader has been asked. */
    public static ModIndex empty() {
        return new ModIndex(Collections.emptyList());
    }
}
