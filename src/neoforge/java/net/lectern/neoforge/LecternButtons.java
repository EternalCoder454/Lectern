package net.lectern.neoforge;

import net.lectern.Lectern;
import net.lectern.screen.LecternScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Puts a Mods button on the title and pause screens.
 *
 * <p>Through {@link ScreenEvent.Init.Post} rather than a mixin. There is nothing here that needs
 * bytecode: the event hands over the finished widget list and a way to add to it, which is exactly
 * the amount of access this needs. A mixin would also have to be kept in step with every screen
 * layout change, and would collide with the several other mods that rearrange these two screens.
 *
 * <p>The button is placed relative to the screen's own size and never overlaps vanilla's own
 * buttons, because it sits in the top-left rather than in the middle column everything else
 * competes for.
 */
public final class LecternButtons {

    private LecternButtons() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Init.Post.class, event -> {
            Screen screen = event.getScreen();
            if (!(screen instanceof TitleScreen) && !(screen instanceof PauseScreen)) {
                return;
            }
            event.addListener(Button.builder(
                            Component.translatable("lectern.title"),
                            b -> screen.getMinecraft().setScreen(new LecternScreen(screen)))
                    .bounds(6, 6, 60, 20)
                    .build());
        });
        Lectern.LOGGER.debug("Mods button registered on the title and pause screens.");
    }
}
