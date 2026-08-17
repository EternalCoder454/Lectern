package net.lectern.api;

import net.minecraft.client.gui.screens.Screen;

/**
 * Makes a mod's settings screen.
 *
 * <p>The parent is the screen to return to when the player backs out; hand it to whatever screen
 * you construct so Escape lands them back in the mod list rather than at the main menu.
 */
@FunctionalInterface
public interface ConfigScreenFactory {

    /**
     * {@return the screen to open, or null to leave the button disabled}
     *
     * <p>Returning null is a legitimate answer -- a mod whose settings only make sense in a world
     * can decline while in the main menu, rather than opening a screen that cannot do anything.
     *
     * @param parent the screen to return to
     */
    Screen create(Screen parent);
}
