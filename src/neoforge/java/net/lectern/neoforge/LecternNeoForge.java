package net.lectern.neoforge;

import net.lectern.Lectern;
import net.lectern.api.ConfigScreenFactory;
import net.lectern.api.LecternApi;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforgespi.language.IModInfo;

/**
 * Lectern's NeoForge entry point.
 *
 * <p>Client only. A mod list is a menu; there is nothing for it to do on a dedicated server, and
 * declaring the dist here means the jar can sit in a server pack without loading anything.
 */
@Mod(value = Lectern.MOD_ID, dist = Dist.CLIENT)
public final class LecternNeoForge {

    public LecternNeoForge(IEventBus modBus, ModContainer container) {
        // Client setup rather than the constructor: the mod list is not complete until every mod
        // has been constructed, and indexing half of it would be worse than indexing none.
        modBus.addListener(FMLClientSetupEvent.class, event -> event.enqueueWork(() -> {
            Lectern.index(new NeoForgeModSource());
            adoptLoaderScreens();
            LecternButtons.register();
        }));
    }

    /**
     * Registers Lectern buttons for every mod that already told NeoForge how to open its settings.
     *
     * <p>This is the reason most mods need no integration. {@code IConfigScreenFactory} is the
     * loader's own registry and mods with settings generally already populate it -- for those,
     * Lectern is a different way of reaching a screen that was always there.
     *
     * <p>{@code putIfAbsent} semantics: anything registered through {@link LecternApi} first wins,
     * so a mod that wants Lectern to show something other than its loader screen can say so.
     */
    private static void adoptLoaderScreens() {
        int adopted = 0;
        for (IModInfo info : ModList.get().getMods()) {
            String modId = info.getModId();
            if (LecternApi.hasScreen(modId)) {
                continue;
            }
            var factory = IConfigScreenFactory.getForMod(info);
            if (factory.isEmpty()) {
                continue;
            }
            ModContainer container = ModList.get().getModContainerById(modId).orElse(null);
            if (container == null) {
                continue;
            }
            IConfigScreenFactory found = factory.get();
            LecternApi.register(modId, wrap(found, container));
            adopted++;
        }
        Lectern.LOGGER.info("Adopted {} loader config screen(s).", adopted);
    }

    /**
     * {@return a Lectern factory that defers to the loader's}
     *
     * <p>Separated out so the lambda captures only what it needs, and so a screen that throws is
     * one broken button rather than a mod list that will not open.
     */
    private static ConfigScreenFactory wrap(IConfigScreenFactory factory, ModContainer container) {
        return (Screen parent) -> {
            try {
                return factory.createScreen(container, parent);
            } catch (Throwable t) {
                Lectern.LOGGER.error("{} failed to build its config screen", container.getModId(), t);
                return null;
            }
        };
    }
}
