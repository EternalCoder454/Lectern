package net.lectern.screen.cloth;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.lectern.LecternConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Lectern's own settings, built with Cloth Config.
 *
 * <h2>Why this class is on its own, in its own package</h2>
 *
 * <p>Every mention of {@code me.shedaniel.clothconfig2} in Lectern is in this file. That is not
 * tidiness: the fields and signatures here name Cloth types, so merely <em>resolving</em> this
 * class without Cloth installed throws {@link NoClassDefFoundError}. Keeping it to one class, only
 * reached from behind a mod-loaded check, is what makes the dependency genuinely optional rather
 * than optional-until-someone-clicks.
 *
 * <p>Cloth is compile-only in the build for the same reason. Lectern without it is Lectern with no
 * settings button, which is a fair trade for not requiring a library to list mods.
 */
public final class LecternClothScreen {

    private LecternClothScreen() {}

    /** {@return Lectern's settings screen, returning to {@code parent} when closed} */
    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("lectern.title"))
                // The values live in fields, so "save" means write them out; there is nothing to
                // reload and nothing to reconcile.
                .setSavingRunnable(LecternConfig::save);

        ConfigEntryBuilder entries = builder.entryBuilder();
        ConfigCategory list = builder.getOrCreateCategory(Component.translatable("lectern.config.list"));

        list.addEntry(entries
                .startBooleanToggle(Component.translatable("lectern.config.show_libraries"),
                        LecternConfig.showLibraries)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("lectern.config.show_libraries.tip"))
                .setSaveConsumer(v -> LecternConfig.showLibraries = v)
                .build());

        list.addEntry(entries
                .startBooleanToggle(Component.translatable("lectern.config.compact_rows"),
                        LecternConfig.compactRows)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("lectern.config.compact_rows.tip"))
                .setSaveConsumer(v -> LecternConfig.compactRows = v)
                .build());

        list.addEntry(entries
                .startBooleanToggle(Component.translatable("lectern.config.remember_search"),
                        LecternConfig.rememberSearch)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("lectern.config.remember_search.tip"))
                .setSaveConsumer(v -> LecternConfig.rememberSearch = v)
                .build());

        list.addEntry(entries
                .startBooleanToggle(Component.translatable("lectern.config.take_over"),
                        LecternConfig.takeOverModsButton)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("lectern.config.take_over.tip"))
                .setSaveConsumer(v -> LecternConfig.takeOverModsButton = v)
                .build());

        return builder.build();
    }
}
