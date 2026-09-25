package de.fr4ncyilgrande.autotpa.client.gui;

import de.fr4ncyilgrande.autotpa.client.config.ConfigManager;
import de.fr4ncyilgrande.autotpa.client.config.ModConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public final class TpaManagerScreen extends Screen {
    private static final int PREFERRED_WIDTH  = 380;
    private static final int PREFERRED_HEIGHT = 308;
    private static final int SAFE_MARGIN      = 10;

    private static final int MARGIN     = 12;
    private static final int GAP        = 6;
    private static final int ROW_HEIGHT = 20;
    private static final int TITLE_Y    = 10;
    private static final int ACCEPT_ALL_Y = 28;
    private static final int HEADER_Y     = 55;
    private static final int LIST_Y       = 71;

    private static final int SEARCH_FROM_BOTTOM  = 54;
    private static final int BUTTONS_FROM_BOTTOM = 26;

    private static final int COLOR_BACKDROP       = 0xC0000000;
    private static final int COLOR_PANEL          = 0xE6202020;
    private static final int COLOR_PANEL_BORDER   = 0xFF3C3C3C;
    private static final int COLOR_HEADER_AUTO    = 0xFF4CAF50;
    private static final int COLOR_HEADER_BLOCKED = 0xFFF44336;
    private static final int COLOR_ROW_BG         = 0x40FFFFFF;
    private static final int COLOR_ROW_BG_ALT     = 0x20FFFFFF;
    private static final int COLOR_TEXT           = 0xFFE0E0E0;
    private static final int COLOR_TEXT_DIM       = 0xFF9E9E9E;

    private final ModConfig config;
    private final List<Button> rowWidgets = new ArrayList<>();

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int columnWidth;
    private int visibleRows;

    private int autoScroll = 0;
    private int blockedScroll = 0;
    private EditBox searchField;
    private Button addButton;
    private Button blockButton;
    private Button acceptAllButton;

    private List<String> cachedAutoList = Collections.emptyList();
    private List<String> cachedBlockedList = Collections.emptyList();

    public TpaManagerScreen(ModConfig config) {
        super(Component.translatable("autotpa.gui.title"));
        this.config = config;
    }

    @Override
    protected void init() {
        super.init();

        this.rowWidgets.clear();

        int maxWidth  = Math.max(1, this.width  - 2 * SAFE_MARGIN);
        int maxHeight = Math.max(1, this.height - 2 * SAFE_MARGIN);
        this.panelWidth  = Math.min(PREFERRED_WIDTH, maxWidth);
        this.panelHeight = Math.min(PREFERRED_HEIGHT, maxHeight);
        this.panelX = Math.max(0, (this.width  - this.panelWidth)  / 2);
        this.panelY = Math.max(0, (this.height - this.panelHeight) / 2);

        int contentWidth = Math.max(1, this.panelWidth - 2 * MARGIN);
        this.columnWidth = Math.max(1, (contentWidth - GAP) / 2);

        int listSpace = this.panelHeight - LIST_Y - SEARCH_FROM_BOTTOM - 12;
        this.visibleRows = Math.max(1, listSpace / ROW_HEIGHT);

        int contentX = this.panelX + MARGIN;
        int acceptAllY = this.panelY + ACCEPT_ALL_Y;
        int searchY  = this.panelY + this.panelHeight - SEARCH_FROM_BOTTOM;
        int buttonsY = this.panelY + this.panelHeight - BUTTONS_FROM_BOTTOM;

        this.acceptAllButton = Button.builder(
                        this.acceptAllButtonLabel(),
                        button -> this.toggleAcceptAll())
                .bounds(contentX, acceptAllY, contentWidth, 20)
                .build();
        this.addRenderableWidget(this.acceptAllButton);

        this.searchField = new EditBox(
                this.font,
                contentX, searchY, contentWidth, 20,
                Component.translatable("autotpa.gui.username_field"));
        this.searchField.setHint(Component.translatable("autotpa.gui.search_hint"));
        this.searchField.setMaxLength(16);
        this.searchField.setResponder(text -> this.refreshRows());
        this.addRenderableWidget(this.searchField);

        this.addButton = Button.builder(
                        Component.translatable("autotpa.gui.add_auto"),
                        button -> this.handleAdd(true))
                .bounds(contentX, buttonsY, this.columnWidth, 20)
                .build();
        this.addRenderableWidget(this.addButton);

        this.blockButton = Button.builder(
                        Component.translatable("autotpa.gui.block"),
                        button -> this.handleAdd(false))
                .bounds(contentX + this.columnWidth + GAP, buttonsY, this.columnWidth, 20)
                .build();
        this.addRenderableWidget(this.blockButton);

        this.addRenderableWidget(Button.builder(
                        Component.literal("\u00D7"),
                        button -> this.onClose())
                .bounds(this.panelX + this.panelWidth - 22,
                        this.panelY + 6, 16, 16)
                .build());

        this.setInitialFocus(this.searchField);
        this.refreshRows();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // The screen supplies its own translucent backdrop instead of Minecraft's menu background.
    }

    private void handleAdd(boolean autoAccept) {
        if (this.searchField == null) return;

        String name = this.currentInputName();
        if (name.isEmpty()) return;

        boolean changed = autoAccept
                ? this.config.addAutoAccept(name)
                : this.config.addBlocked(name);

        if (changed) {
            ConfigManager.save(this.config);
        }

        if (this.searchField.getValue().isEmpty()) {
            this.refreshRows();
        } else {
            this.searchField.setValue("");
        }
    }

    private void refreshRows() {
        for (Button widget : this.rowWidgets) {
            this.removeWidget(widget);
        }
        this.rowWidgets.clear();

        String query = this.currentQuery();
        this.cachedAutoList    = sortAndFilter(this.config.autoAccept, query);
        this.cachedBlockedList = sortAndFilter(this.config.blocked, query);

        boolean hasValidInput = ModConfig.isValidPlayerName(this.currentInputName());
        this.addButton.active   = hasValidInput;
        this.blockButton.active = hasValidInput;

        this.autoScroll    = clampScroll(this.autoScroll,    this.cachedAutoList.size());
        this.blockedScroll = clampScroll(this.blockedScroll, this.cachedBlockedList.size());

        int leftX   = this.panelX + MARGIN;
        int rightX  = leftX + this.columnWidth + GAP;
        int listTop = this.panelY + LIST_Y;

        addListButtons(leftX,  listTop, this.cachedAutoList,    this.autoScroll,    true);
        addListButtons(rightX, listTop, this.cachedBlockedList, this.blockedScroll, false);
    }

    private void addListButtons(int columnX, int listTop, List<String> names,
                                int scroll, boolean isAutoColumn) {
        int end = Math.min(names.size(), scroll + this.visibleRows);
        for (int i = scroll; i < end; i++) {
            String name = names.get(i);
            int rowY = listTop + (i - scroll) * ROW_HEIGHT;
            Component label = Component.translatable(
                    isAutoColumn ? "autotpa.gui.remove" : "autotpa.gui.unblock");
            Button rowButton = Button.builder(label, button -> {
                if (isAutoColumn) {
                    this.config.removeAutoAccept(name);
                } else {
                    this.config.unblock(name);
                }
                ConfigManager.save(this.config);
                this.refreshRows();
            }).bounds(columnX + this.columnWidth - 56, rowY + 1, 54, 16).build();
            this.rowWidgets.add(rowButton);
            this.addRenderableWidget(rowButton);
        }
    }

    private static List<String> sortAndFilter(Iterable<String> source, String query) {
        List<String> result = new ArrayList<>();
        for (String name : source) {
            if (name == null) continue;
            if (query.isEmpty() || name.toLowerCase(Locale.ROOT).contains(query)) {
                result.add(name);
            }
        }
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    private String currentQuery() {
        return this.searchField == null
                ? ""
                : this.searchField.getValue().trim().toLowerCase(Locale.ROOT);
    }

    private String currentInputName() {
        return this.searchField == null ? "" : this.searchField.getValue().trim();
    }

    private int clampScroll(int scroll, int size) {
        int maxScroll = Math.max(0, size - this.visibleRows);
        return Math.max(0, Math.min(scroll, maxScroll));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        graphics.fill(0, 0, this.width, this.height, COLOR_BACKDROP);
        graphics.fill(this.panelX, this.panelY,
                this.panelX + this.panelWidth, this.panelY + this.panelHeight, COLOR_PANEL);
        drawRectBorder(graphics, this.panelX, this.panelY,
                this.panelWidth, this.panelHeight, COLOR_PANEL_BORDER);

        graphics.centeredText(this.font, this.title,
                this.panelX + this.panelWidth / 2, this.panelY + TITLE_Y, COLOR_TEXT);

        int leftX  = this.panelX + MARGIN;
        int rightX = leftX + this.columnWidth + GAP;
        int headerY = this.panelY + HEADER_Y;
        int listTop = this.panelY + LIST_Y;

        graphics.text(this.font,
                Component.translatable("autotpa.gui.auto_header"),
                leftX, headerY, COLOR_HEADER_AUTO);
        graphics.text(this.font,
                Component.translatable("autotpa.gui.blocked_header"),
                rightX, headerY, COLOR_HEADER_BLOCKED);

        renderList(graphics, leftX,  listTop, this.cachedAutoList,    this.autoScroll);
        renderList(graphics, rightX, listTop, this.cachedBlockedList, this.blockedScroll);

        if (this.cachedAutoList.isEmpty()) {
            graphics.text(this.font,
                    Component.translatable("autotpa.gui.none"),
                    leftX + 2, listTop + 2, COLOR_TEXT_DIM);
        }
        if (this.cachedBlockedList.isEmpty()) {
            graphics.text(this.font,
                    Component.translatable("autotpa.gui.none"),
                    rightX + 2, listTop + 2, COLOR_TEXT_DIM);
        }

        super.extractRenderState(graphics, mouseX, mouseY, deltaTicks);
    }

    private void toggleAcceptAll() {
        this.config.acceptAllTpaRequests = !this.config.acceptAllTpaRequests;
        ConfigManager.save(this.config);
        if (this.acceptAllButton != null) {
            this.acceptAllButton.setMessage(this.acceptAllButtonLabel());
        }
    }

    private Component acceptAllButtonLabel() {
        return Component.translatable(this.config.acceptAllTpaRequests
                ? "autotpa.gui.accept_all.enabled"
                : "autotpa.gui.accept_all.disabled");
    }

    private void renderList(GuiGraphicsExtractor graphics, int columnX, int listTop,
                            List<String> names, int scroll) {
        int end = Math.min(names.size(), scroll + this.visibleRows);
        for (int i = scroll; i < end; i++) {
            String name = names.get(i);
            int rowY = listTop + (i - scroll) * ROW_HEIGHT;
            int rowColor = (i % 2 == 0) ? COLOR_ROW_BG : COLOR_ROW_BG_ALT;
            graphics.fill(columnX, rowY, columnX + this.columnWidth,
                    rowY + ROW_HEIGHT - 2, rowColor);
            graphics.text(this.font,
                    Component.literal(name),
                    columnX + 4, rowY + 5, COLOR_TEXT);
        }

        if (names.size() > this.visibleRows) {
            String scrollInfo = (scroll + 1) + "-" + end + " / " + names.size();
            graphics.text(this.font,
                    Component.literal(scrollInfo),
                    columnX, listTop + this.visibleRows * ROW_HEIGHT + 2, COLOR_TEXT_DIM);
        }
    }

    private static void drawRectBorder(GuiGraphicsExtractor graphics, int x, int y,
                                       int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                 double horizontalAmount, double verticalAmount) {
        int leftX  = this.panelX + MARGIN;
        int rightX = leftX + this.columnWidth + GAP;
        int listTop    = this.panelY + LIST_Y;
        int listBottom = listTop + this.visibleRows * ROW_HEIGHT;

        int delta = (int) Math.signum(verticalAmount);
        if (delta == 0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        if (mouseY < listTop || mouseY >= listBottom) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        if (mouseX >= leftX && mouseX < leftX + this.columnWidth) {
            int newScroll = clampScroll(this.autoScroll - delta, this.cachedAutoList.size());
            if (newScroll != this.autoScroll) {
                this.autoScroll = newScroll;
                this.refreshRows();
            }
            return true;
        }
        if (mouseX >= rightX && mouseX < rightX + this.columnWidth) {
            int newScroll = clampScroll(this.blockedScroll - delta, this.cachedBlockedList.size());
            if (newScroll != this.blockedScroll) {
                this.blockedScroll = newScroll;
                this.refreshRows();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onClose() {
        ConfigManager.save(this.config);
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
