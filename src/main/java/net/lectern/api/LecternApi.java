package net.lectern.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * How a mod gets a settings button in the list.
 *
 * <h2>Three ways in, in order of preference</h2>
 *
 * <ol>
 *   <li><b>Do nothing.</b> If a mod registers NeoForge's own {@code IConfigScreenFactory}, Lectern
 *       finds it. Most mods with settings already do, so most mods work with no integration at all.
 *       This is the case Catalogue and ModMenu both handle and it is the one that matters most.
 *   <li><b>{@link #register}</b>, for a mod that wants a button without adopting the loader's
 *       screen registry, or that wants to show something other than its config.
 *   <li><b>{@link #provide}</b>, for a config library registering screens on behalf of the mods
 *       that use it -- Cloth Config does this for dozens of mods at once.
 * </ol>
 *
 * <h2>Why there is no entrypoint to implement</h2>
 *
 * <p>ModMenu asks mods to declare a {@code ModMenuApi} entrypoint in their metadata. That works,
 * and it means a mod cannot register a screen without also editing its manifest, and that Lectern
 * has to be present at compile time for the interface to exist. A static call from a mod's own
 * client initialiser needs neither: guard it with a mod-loaded check and Lectern stays a soft
 * dependency.
 *
 * <p>Nothing here touches Minecraft classes, so a mod can call it without pulling the client in.
 */
public final class LecternApi {

    /** modId -> the screen to open. Concurrent because mods register from their own init threads. */
    private static final Map<String, ConfigScreenFactory> FACTORIES = new ConcurrentHashMap<>();

    private LecternApi() {}

    /**
     * Registers the screen for one mod.
     *
     * <p>Last registration wins, so a pack can override a mod's own screen by registering after it.
     *
     * @param modId   the mod this screen belongs to
     * @param factory given the screen to return to, produces the screen to open
     */
    public static void register(String modId, ConfigScreenFactory factory) {
        FACTORIES.put(modId, factory);
    }

    /**
     * Registers screens for many mods at once.
     *
     * <p>For config libraries. The function is asked per mod id and may return null for mods it
     * does not handle, so a library can offer a screen for everything it knows about without
     * enumerating them.
     *
     * @param modIds  the mods to offer screens for
     * @param factory asked once per id; null means "not mine"
     */
    public static void provide(Iterable<String> modIds, Function<String, ConfigScreenFactory> factory) {
        for (String modId : modIds) {
            ConfigScreenFactory made = factory.apply(modId);
            if (made != null) {
                FACTORIES.putIfAbsent(modId, made);
            }
        }
    }

    /** {@return the registered factory for {@code modId}, or null} */
    public static ConfigScreenFactory factory(String modId) {
        return FACTORIES.get(modId);
    }

    /** {@return whether anything has registered a screen for {@code modId}} */
    public static boolean hasScreen(String modId) {
        return FACTORIES.containsKey(modId);
    }
}
