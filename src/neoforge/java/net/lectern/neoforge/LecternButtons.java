package net.lectern.neoforge;

import net.lectern.Lectern;
import net.lectern.screen.LecternScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Takes over the loader's Mods button rather than adding a second one.
 *
 * <p>NeoForge puts its own Mods button on the title screen, opening FML's mod list. Adding another
 * beside it would mean two buttons with the same label doing nearly the same thing, which is a
 * worse menu than either alone. So this finds that button, takes its exact position, and puts
 * Lectern behind it.
 *
 * <h2>Finding it by translation key, not by label</h2>
 *
 * <p>The button is identified by the {@code fml.menu.mods} key on its message rather than by the
 * rendered text. Matching "Mods" as a string would work in English and quietly stop working in
 * every other language, which is the sort of bug that only ever gets reported by the people least
 * able to describe it.
 *
 * <h2>What happens if it is not there</h2>
 *
 * <p>Then Lectern adds its own, and says so in the log. A missing button means either that another
 * mod removed it or that NeoForge renamed the key; in both cases the alternative is a mod list with
 * no way to open it, and the duplication this class exists to avoid is not possible anyway.
 */
public final class LecternButtons {

    /** NeoForge's own Mods button, by the key it is built from. */
    private static final String FML_MODS_KEY = "fml.menu.mods";

    private static boolean warnedAboutMissingButton;

    private LecternButtons() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Init.Post.class, LecternButtons::onScreenInit);
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        AbstractWidget existing = net.lectern.LecternConfig.takeOverModsButton
                ? findModsButton(event) : null;

        if (existing != null) {
            // Same place, same size, so the menu keeps its layout and nothing shifts.
            event.removeListener(existing);
            event.addListener(Button.builder(existing.getMessage(), b -> open(screen))
                    .bounds(existing.getX(), existing.getY(), existing.getWidth(), existing.getHeight())
                    .build());
            return;
        }

        // Only the title screen is expected to have one; anywhere else, silence is correct.
        if (!isTitleScreen(screen)) {
            return;
        }
        // Warn only when the takeover was wanted and failed. With it switched off, a separate
        // button is the whole point and saying "nothing to take over" would be nonsense.
        if (net.lectern.LecternConfig.takeOverModsButton && !warnedAboutMissingButton) {
            warnedAboutMissingButton = true;
            Lectern.LOGGER.warn("No '{}' button on the title screen to take over -- adding one, so "
                    + "the mod list stays reachable. Another mod may have removed it.", FML_MODS_KEY);
        }
        event.addListener(Button.builder(Component.translatable("lectern.title"), b -> open(screen))
                .bounds(6, 6, 60, 20)
                .build());
    }

    /** {@return the loader's Mods button on this screen, or null} */
    private static AbstractWidget findModsButton(ScreenEvent.Init.Post event) {
        for (GuiEventListener listener : event.getListenersList()) {
            if (!(listener instanceof AbstractWidget widget)) {
                continue;
            }
            if (widget.getMessage().getContents() instanceof TranslatableContents contents
                    && FML_MODS_KEY.equals(contents.getKey())) {
                return widget;
            }
        }
        return null;
    }

    private static boolean isTitleScreen(Screen screen) {
        return screen instanceof net.minecraft.client.gui.screens.TitleScreen;
    }

    private static void open(Screen parent) {
        parent.getMinecraft().setScreen(new LecternScreen(parent));
    }
}
