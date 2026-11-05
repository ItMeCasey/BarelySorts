package com.itmecasey.barelysorts.client;

import com.itmecasey.barelysorts.GroupSettings;
import com.itmecasey.barelysorts.Platform;
import com.itmecasey.barelysorts.Sorter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * What the buttons, the settings popup, slot locking and the hotkeys do on a container screen. The loader layers
 * forward their screen events here, so the behavior is written once. Everything is drawn and hit-tested by hand.
 */
public final class ScreenActions {
    private static final int LEFT_BUTTON = 0;
    private static final int MIDDLE_BUTTON = 2;

    /** Our own pixel font (common/src/main/resources/assets/barelysorts/font), used for popup and hint text. */
    private static final ResourceLocation SMALL_FONT =
            ResourceLocation.fromNamespaceAndPath("barelysorts", "small");

    private static final int ROW_HEIGHT = 12;
    private static final int ROW_PADDING = 6;
    private static final int MIN_POPUP_WIDTH = 80;
    private static final int EDGE = 2;
    private static final int FRAME = 0xFF2E2A46;
    private static final int FACE = 0xFF1A1830;
    private static final int HOVER_FACE = 0xFF2E2A46;
    private static final int TEXT = 0xFFF4F2FF;
    private static final int GOLD = 0xFFF2B33D;
    private static final int GOLD_TINT = 0x55F2B33D;
    /** Locked slots keep this faint outline when the group is not being edited. */
    private static final int LOCKED_FAINT = 0x66F2B33D;
    private static final int LOCKED_FAINT_TINT = 0x18F2B33D;

    /** Row indexes in the popup. The player group has one more row, for the tool swap setting. */
    private static final int ROW_METHOD = 0;
    private static final int ROW_KEY = 1;
    private static final int ROW_SLOTS = 2;
    private static final int ROW_TOOLS = 3;

    /** The settings popup is open for one group of one screen, or nowhere. */
    private static AbstractContainerScreen<?> popupScreen;
    private static String popupKey;

    /** Slot-lock selection is active for one group of one screen, or nowhere. */
    private static AbstractContainerScreen<?> selectScreen;
    private static String selectKey;

    private ScreenActions() {
    }

    public static void render(AbstractContainerScreen<?> screen, GuiGraphics graphics, double mouseX, double mouseY) {
        String tooltip = null;
        for (Group group : Group.of(screen)) {
            boolean sortHover = ButtonWidget.inside(mouseX, mouseY, group.sortButtonX(), group.sortButtonY());
            boolean menuHover = ButtonWidget.inside(mouseX, mouseY, group.menuButtonX(), group.menuButtonY());
            ButtonWidget.drawSort(graphics, group.sortButtonX(), group.sortButtonY(), sortHover);
            ButtonWidget.drawMenu(graphics, group.menuButtonX(), group.menuButtonY(), menuHover);
            if (sortHover) {
                tooltip = "Sort this group";
            } else if (menuHover) {
                tooltip = "Sort settings";
            }
            if (isSelecting(screen, group)) {
                drawLocked(graphics, screen, group, GOLD_TINT, GOLD);
                drawSelectionHint(graphics, group);
            } else {
                drawLocked(graphics, screen, group, LOCKED_FAINT_TINT, LOCKED_FAINT);
            }
            if (isPopupOpen(screen, group)) {
                renderPopup(graphics, group, mouseX, mouseY);
            }
        }
        // Tooltips are for the two buttons only. The popup rows have none.
        if (tooltip != null) {
            graphics.renderTooltip(Minecraft.getInstance().font, Component.literal(tooltip),
                    (int) mouseX, (int) mouseY);
        }
    }

    /** Returns true when the click was used, so the loader can cancel it. */
    public static boolean mouseButton(AbstractContainerScreen<?> screen, double mouseX, double mouseY, int button) {
        if (popupScreen == screen && button == LEFT_BUTTON) {
            for (Group group : Group.of(screen)) {
                if (isPopupOpen(screen, group) && insidePopup(group, mouseX, mouseY)) {
                    int row = (int) ((mouseY - popupTop(group)) / ROW_HEIGHT);
                    if (row == ROW_SLOTS) {
                        startSelecting(screen, group);
                    } else {
                        toggle(group, row);
                    }
                    return true;
                }
            }
        }
        if (selectScreen == screen && button == LEFT_BUTTON) {
            for (Group group : Group.of(screen)) {
                if (!isSelecting(screen, group)) {
                    continue;
                }
                if (ButtonWidget.inside(mouseX, mouseY, group.menuButtonX(), group.menuButtonY())) {
                    stopSelecting();
                    return true;
                }
                Slot slot = slotAt(screen, group, mouseX, mouseY);
                if (slot != null) {
                    toggleSlot(group, slot);
                    return true;
                }
            }
        }
        for (Group group : Group.of(screen)) {
            if (button == LEFT_BUTTON
                    && ButtonWidget.inside(mouseX, mouseY, group.menuButtonX(), group.menuButtonY())) {
                togglePopup(screen, group);
                return true;
            }
            if (button == LEFT_BUTTON && ButtonWidget.inside(mouseX, mouseY, group.sortButtonX(), group.sortButtonY())) {
                closePopup();
                sort(screen, group);
                return true;
            }
            if (button == MIDDLE_BUTTON && group.contains(mouseX, mouseY)) {
                closePopup();
                sort(screen, group);
                return true;
            }
        }
        // A click anywhere else closes the popup, and the click still goes through to the screen.
        closePopup();
        return false;
    }

    /**
     * Escape closes the popup or ends slot selection. R sorts the group under the mouse; away from a group it does
     * nothing, so the recipe book key keeps working. Returns true when the key was used.
     */
    public static boolean key(AbstractContainerScreen<?> screen, int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (selectScreen == screen) {
                stopSelecting();
                return true;
            }
            if (popupScreen == screen) {
                closePopup();
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_R) {
            return keyR(screen);
        }
        return false;
    }

    private static boolean keyR(AbstractContainerScreen<?> screen) {
        Minecraft minecraft = Minecraft.getInstance();
        var window = minecraft.getWindow();
        double mouseX = minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight();
        List<Group> groups = Group.of(screen);
        for (Group group : groups) {
            if (group.contains(mouseX, mouseY)) {
                sort(screen, group);
                return true;
            }
        }
        return false;
    }

    private static void sort(AbstractContainerScreen<?> screen, Group group) {
        GroupSettings settings = ClientSettings.get(group.settingsKey());
        Platform.sendSort(screen.getMenu().containerId, group.name(), settings.method().name(),
                settings.key().name(), lockedIds(settings));
    }

    private static String lockedIds(GroupSettings settings) {
        return String.join(",", settings.lockedSlots().stream().map(String::valueOf).toList());
    }

    private static void togglePopup(AbstractContainerScreen<?> screen, Group group) {
        if (isPopupOpen(screen, group)) {
            closePopup();
        } else {
            popupScreen = screen;
            popupKey = group.settingsKey();
        }
    }

    private static void closePopup() {
        popupScreen = null;
        popupKey = null;
    }

    private static void startSelecting(AbstractContainerScreen<?> screen, Group group) {
        closePopup();
        selectScreen = screen;
        selectKey = group.settingsKey();
    }

    private static void stopSelecting() {
        selectScreen = null;
        selectKey = null;
    }

    private static boolean isPopupOpen(AbstractContainerScreen<?> screen, Group group) {
        return popupScreen == screen && group.settingsKey().equals(popupKey);
    }

    private static boolean isSelecting(AbstractContainerScreen<?> screen, Group group) {
        return selectScreen == screen && group.settingsKey().equals(selectKey);
    }

    /** Which slot of the group is under the mouse, or null. */
    private static Slot slotAt(AbstractContainerScreen<?> screen, Group group, double mouseX, double mouseY) {
        int guiLeft = Platform.guiOrigin().left(screen);
        int guiTop = Platform.guiOrigin().top(screen);
        for (Slot slot : group.slots()) {
            int x = guiLeft + slot.x;
            int y = guiTop + slot.y;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                return slot;
            }
        }
        return null;
    }

    private static void toggleSlot(Group group, Slot slot) {
        GroupSettings settings = ClientSettings.get(group.settingsKey());
        var inventory = Minecraft.getInstance().player.getInventory();
        ClientSettings.put(group.settingsKey(), settings.toggleSlot(Sorter.lockId(slot, inventory)));
    }

    /** Outlines every locked slot of the group with the given colors. */
    private static void drawLocked(GuiGraphics g, AbstractContainerScreen<?> screen, Group group, int tint, int edge) {
        GroupSettings settings = ClientSettings.get(group.settingsKey());
        var inventory = Minecraft.getInstance().player.getInventory();
        int guiLeft = Platform.guiOrigin().left(screen);
        int guiTop = Platform.guiOrigin().top(screen);
        for (Slot slot : group.slots()) {
            if (!settings.isLocked(Sorter.lockId(slot, inventory))) {
                continue;
            }
            int x = guiLeft + slot.x;
            int y = guiTop + slot.y;
            g.fill(x, y, x + 16, y + 16, tint);
            g.fill(x - 1, y - 1, x + 17, y, edge);
            g.fill(x - 1, y + 16, x + 17, y + 17, edge);
            g.fill(x - 1, y, x, y + 16, edge);
            g.fill(x + 16, y, x + 17, y + 16, edge);
        }
    }

    /** Says how to finish slot selection, just above the group. */
    private static void drawSelectionHint(GuiGraphics g, Group group) {
        Font font = Minecraft.getInstance().font;
        g.drawString(font, small("Click slots to lock. Esc to finish."), group.left(), group.top() - 12, GOLD, false);
    }

    private static Component small(String text) {
        return Component.literal(text).withStyle(Style.EMPTY.withFont(SMALL_FONT));
    }

    /** The popup hangs below the settings button, lined up with its right edge, and stays on screen. */
    private static int popupWidth(Group group) {
        Font font = Minecraft.getInstance().font;
        int widest = 0;
        for (String label : labels(group)) {
            widest = Math.max(widest, font.width(small(label)));
        }
        return Math.max(MIN_POPUP_WIDTH, widest + ROW_PADDING * 2);
    }

    private static int popupLeft(Group group) {
        int left = group.menuButtonX() + ButtonWidget.SIZE - popupWidth(group);
        return Math.max(EDGE, left);
    }

    private static int popupTop(Group group) {
        return group.menuButtonY() + ButtonWidget.SIZE + 2;
    }

    /** The player group has four rows (it has the tool swap setting); the others have three. */
    private static int rowCount(Group group) {
        return Sorter.GROUP_PLAYER.equals(group.settingsKey()) ? 4 : 3;
    }

    private static int popupHeight(Group group) {
        return rowCount(group) * ROW_HEIGHT + 4;
    }

    private static boolean insidePopup(Group group, double mouseX, double mouseY) {
        int left = popupLeft(group);
        int top = popupTop(group);
        return mouseX >= left && mouseX < left + popupWidth(group)
                && mouseY >= top && mouseY < top + popupHeight(group);
    }

    private static void renderPopup(GuiGraphics g, Group group, double mouseX, double mouseY) {
        int left = popupLeft(group);
        int top = popupTop(group);
        int width = popupWidth(group);
        List<String> labels = labels(group);
        Font font = Minecraft.getInstance().font;

        g.fill(left - 1, top - 1, left + width + 1, top + popupHeight(group) + 1, FRAME);
        g.fill(left, top, left + width, top + popupHeight(group), FACE);

        for (int row = 0; row < labels.size(); row++) {
            int rowTop = top + 2 + row * ROW_HEIGHT;
            boolean hover = mouseX >= left && mouseX < left + width
                    && mouseY >= rowTop && mouseY < rowTop + ROW_HEIGHT;
            if (hover) {
                g.fill(left + 1, rowTop, left + width - 1, rowTop + ROW_HEIGHT, HOVER_FACE);
            }
            g.drawString(font, small(labels.get(row)), left + ROW_PADDING, rowTop + 2, TEXT, false);
        }
    }

    private static List<String> labels(Group group) {
        GroupSettings settings = ClientSettings.get(group.settingsKey());
        List<String> labels = new ArrayList<>(List.of(
                methodLabel(settings), keyLabel(settings), slotsLabel(settings)));
        if (rowCount(group) == 4) {
            labels.add(toolsLabel(settings));
        }
        return labels;
    }

    private static void toggle(Group group, int row) {
        GroupSettings settings = ClientSettings.get(group.settingsKey());
        switch (row) {
            case ROW_METHOD -> ClientSettings.put(group.settingsKey(), settings.withMethod(settings.method().next()));
            case ROW_KEY -> ClientSettings.put(group.settingsKey(), settings.withKey(settings.key().next()));
            case ROW_TOOLS -> {
                // The tool setting is per player, so it is saved and also sent to the server right away.
                GroupSettings updated = settings.withReplaceAtOne(!settings.replaceAtOne());
                ClientSettings.put(group.settingsKey(), updated);
                AutofillSync.send(updated.replaceAtOne());
            }
            default -> {
            }
        }
    }

    private static String methodLabel(GroupSettings settings) {
        return "Sort: " + (settings.method().name().equals("ROWS") ? "Rows" : "Columns");
    }

    private static String keyLabel(GroupSettings settings) {
        return "By: " + switch (settings.key()) {
            case QUANTITY -> "Quantity";
            case NAME -> "Item name";
            case MOD -> "Mod name";
        };
    }

    private static String slotsLabel(GroupSettings settings) {
        return "Locked: " + settings.lockedSlots().size() + " (choose)";
    }

    private static String toolsLabel(GroupSettings settings) {
        return settings.replaceAtOne() ? "Tools: at 1 durability" : "Tools: when broken";
    }
}
