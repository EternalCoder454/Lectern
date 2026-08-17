package net.lectern.screen;

import net.lectern.Lectern;
import net.lectern.ModEntry;
import net.lectern.api.ConfigScreenFactory;
import net.lectern.api.LecternApi;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * The mod list.
 *
 * <h2>Why the list is drawn rather than delegated to a widget</h2>
 *
 * <p>Vanilla's {@code ObjectSelectionList} allocates an entry object per row and keeps them for the
 * life of the screen. That is fine for a settings menu with nine rows and wasteful for a mod list
 * with three hundred, most of which are off-screen at any moment. Drawing the visible rows straight
 * from the filtered list means the screen holds no per-row state at all: scrolling changes an int.
 *
 * <p>Everything on the draw path here is a field read or an int comparison. The filtered list is
 * rebuilt only when the query changes, into a list this screen owns -- see
 * {@link net.lectern.ModIndex#search}.
 */
public final class LecternScreen extends Screen {

    private static final int ROW_HEIGHT = 26;
    private static final int LIST_WIDTH = 168;
    private static final int PADDING = 8;
    private static final int HEADER = 32;

    private static final int COLOUR_TEXT = 0xFFFFFFFF;
    private static final int COLOUR_DIM = 0xFFA0A0A0;
    private static final int COLOUR_ROW = 0x40000000;
    private static final int COLOUR_ROW_SELECTED = 0x80FFFFFF;
    private static final int COLOUR_PANEL = 0x40000000;

    private final Screen parent;

    /** Owned by this screen and reused; see the class docs. */
    private final List<ModEntry> filtered = new ArrayList<>();

    private EditBox search;
    private Button settings;
    private ModEntry selected;
    private int scroll;
    private boolean includeLibraries;

    public LecternScreen(Screen parent) {
        super(Component.translatable("lectern.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        search = new EditBox(this.font, PADDING, PADDING, LIST_WIDTH, 18,
                Component.translatable("lectern.search"));
        search.setMaxLength(64);
        // Only refilter when the text actually changes, which is what the responder is for.
        search.setResponder(query -> refilter());
        addRenderableWidget(search);

        settings = Button.builder(Component.translatable("lectern.settings"), b -> openSettings())
                .bounds(LIST_WIDTH + PADDING * 2, this.height - 28, 100, 20)
                .build();
        addRenderableWidget(settings);

        addRenderableWidget(Button.builder(
                        Component.translatable(includeLibraries
                                ? "lectern.libraries.show" : "lectern.libraries.hide"),
                        b -> {
                            includeLibraries = !includeLibraries;
                            refilter();
                            rebuildWidgets();
                        })
                .bounds(PADDING, this.height - 28, LIST_WIDTH, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                        b -> this.minecraft.setScreen(parent))
                .bounds(this.width - 108, this.height - 28, 100, 20)
                .build());

        refilter();
    }

    private void refilter() {
        Lectern.index().search(search == null ? "" : search.getValue(), includeLibraries, filtered);
        scroll = 0;
        if (selected != null && !filtered.contains(selected)) {
            selected = null;
        }
        if (selected == null && !filtered.isEmpty()) {
            selected = filtered.get(0);
        }
        updateSettingsButton();
    }

    private void updateSettingsButton() {
        if (settings != null) {
            settings.active = selected != null && LecternApi.hasScreen(selected.id());
        }
    }

    private void openSettings() {
        if (selected == null) {
            return;
        }
        ConfigScreenFactory factory = LecternApi.factory(selected.id());
        if (factory == null) {
            return;
        }
        Screen screen = factory.create(this);
        if (screen != null) {
            this.minecraft.setScreen(screen);
        }
    }

    /** {@return how many rows fit} */
    private int visibleRows() {
        return Math.max(1, (this.height - HEADER - 36) / ROW_HEIGHT);
    }

    private int maxScroll() {
        return Math.max(0, filtered.size() - visibleRows());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int listTop = HEADER;
        int rows = visibleRows();

        // Only the rows on screen are touched. Scrolling past three hundred mods costs the same as
        // scrolling past ten.
        for (int i = 0; i < rows; i++) {
            int index = scroll + i;
            if (index >= filtered.size()) {
                break;
            }
            ModEntry entry = filtered.get(index);
            int y = listTop + i * ROW_HEIGHT;
            boolean isSelected = entry == selected;

            graphics.fill(PADDING, y, PADDING + LIST_WIDTH, y + ROW_HEIGHT - 2,
                    isSelected ? COLOUR_ROW_SELECTED : COLOUR_ROW);
            graphics.text(this.font, trim(entry.name(), LIST_WIDTH - 12),
                    PADDING + 5, y + 4, COLOUR_TEXT);
            graphics.text(this.font, trim(entry.version(), LIST_WIDTH - 12),
                    PADDING + 5, y + 14, COLOUR_DIM);
        }

        if (filtered.isEmpty()) {
            graphics.text(this.font, Component.translatable("lectern.none"),
                    PADDING + 5, listTop + 6, COLOUR_DIM);
        }

        extractDetail(graphics);
        extractCount(graphics);
    }

    private void extractDetail(GuiGraphicsExtractor graphics) {
        int x = LIST_WIDTH + PADDING * 2;
        int width = this.width - x - PADDING;
        if (width < 60) {
            return;
        }
        graphics.fill(x, HEADER, x + width, this.height - 36, COLOUR_PANEL);

        if (selected == null) {
            return;
        }
        int y = HEADER + 6;
        graphics.text(this.font, selected.name(), x + 6, y, COLOUR_TEXT);
        y += 12;
        graphics.text(this.font, selected.id() + "  " + selected.version(), x + 6, y, COLOUR_DIM);
        y += 12;

        if (!selected.authors().isEmpty()) {
            graphics.text(this.font, String.join(", ", selected.authors()), x + 6, y, COLOUR_DIM);
            y += 12;
        }
        if (!selected.description().isEmpty()) {
            y += 4;
            graphics.textWithWordWrap(this.font, Component.literal(selected.description()),
                    x + 6, y, width - 12, COLOUR_TEXT);
        }
    }

    private void extractCount(GuiGraphicsExtractor graphics) {
        int total = Lectern.index().count();
        Component label = filtered.size() == total
                ? Component.translatable("lectern.count", total)
                : Component.translatable("lectern.count.filtered", filtered.size(), total);
        graphics.text(this.font, label, PADDING, HEADER - 12, COLOUR_DIM);
    }

    /** Cuts a string to fit, since the font cannot wrap a single-line row. */
    private String trim(String text, int width) {
        return this.font.width(text) <= width ? text : this.font.plainSubstrByWidth(text, width - 6) + "...";
    }

    // 26.1.2 folded the coordinates and button into MouseButtonEvent; the old
    // mouseClicked(double, double, int) is gone rather than deprecated.
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (event.button() == 0 && mouseX >= PADDING && mouseX <= PADDING + LIST_WIDTH
                && mouseY >= HEADER) {
            int index = scroll + (int) ((mouseY - HEADER) / ROW_HEIGHT);
            if (index >= 0 && index < filtered.size()) {
                selected = filtered.get(index);
                updateSettingsButton();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX <= PADDING + LIST_WIDTH) {
            scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
