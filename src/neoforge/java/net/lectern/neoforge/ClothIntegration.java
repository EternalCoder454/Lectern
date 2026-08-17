package net.lectern.neoforge;

import net.lectern.Lectern;
import net.lectern.api.LecternApi;
import net.neoforged.fml.ModList;

/**
 * Registers Lectern's own settings screen, when Cloth Config is installed.
 *
 * <p>The guard matters more than it looks. {@code LecternClothScreen} names Cloth types in its
 * signatures, so resolving that class without the mod present throws {@link NoClassDefFoundError}.
 * Keeping the reference inside a lambda that only runs when somebody opens the screen -- and only
 * registering that lambda when the mod is actually loaded -- is what keeps the dependency optional
 * in practice rather than merely on paper.
 */
public final class ClothIntegration {

    private static final String CLOTH_CONFIG = "cloth_config";

    private ClothIntegration() {}

    public static void register() {
        if (!ModList.get().isLoaded(CLOTH_CONFIG)) {
            Lectern.LOGGER.debug("Cloth Config not installed; Lectern has no settings button.");
            return;
        }
        LecternApi.register(Lectern.MOD_ID,
                parent -> net.lectern.screen.cloth.LecternClothScreen.create(parent));
        Lectern.LOGGER.debug("Settings screen registered through Cloth Config.");
    }
}
