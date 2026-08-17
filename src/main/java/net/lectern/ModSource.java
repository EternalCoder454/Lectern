package net.lectern;

import java.util.List;

/**
 * Where the mod list comes from.
 *
 * <p>The one thing Lectern needs a loader for. Everything else in {@code src/main} is plain Java
 * and Minecraft, which is what keeps a second loader to a second implementation of this interface
 * rather than a second copy of the menu.
 */
@FunctionalInterface
public interface ModSource {

    /** {@return every installed mod, in any order; Lectern sorts} */
    List<ModEntry> mods();
}
