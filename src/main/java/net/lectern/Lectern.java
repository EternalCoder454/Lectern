package net.lectern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lectern's state: the index, built once, and the logger.
 *
 * <p>Deliberately not a god object. It holds the index because the index is genuinely global and
 * genuinely expensive to build, and holds nothing else.
 */
public final class Lectern {

    public static final String MOD_ID = "lectern";
    public static final Logger LOGGER = LoggerFactory.getLogger("Lectern");

    private static volatile ModIndex index = ModIndex.empty();

    private Lectern() {}

    /**
     * Builds the index from {@code source}.
     *
     * <p>Called once, by the loader entry point, after mods are loaded. Building it lazily on first
     * open would move a hundred milliseconds of work to the moment somebody clicks the button,
     * which is the worst possible time for it.
     */
    public static void index(ModSource source) {
        index = ModIndex.build(source);
        LOGGER.info("Indexed {} mods.", index.count());
    }

    /** {@return the index; empty until {@link #index(ModSource)} has run} */
    public static ModIndex index() {
        return index;
    }
}
