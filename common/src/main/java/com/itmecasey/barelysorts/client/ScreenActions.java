package com.itmecasey.barelysorts.client;

import com.itmecasey.barelysorts.GroupSettings;
import com.itmecasey.barelysorts.Platform;
import com.itmecasey.barelysorts.Sorter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * What the buttons, the settings popup, slot locking and the hotkeys do on a container screen. The loader layers
 * forward their screen events here, so the behavior is written once. Everything is drawn and hit-tested by hand.
 */
public final class ScreenActions {
    private static final int LEFT_BUTTON = 0;
    private static final int MIDDLE_BUTTON = 2;

    private static final int POPUP_WIDTH = 104;
    private static final int ROW_HEIGHT = 12;
    private static final int EDGE = 2;
    private static final float TEXT_SCALE = 0.8f;
    private static final int FRAME = 0xFF2E2A46;
    private static final int FACE = 0xFF1A1830;
    private static final int HOVER_FACE = 0xFF2E2A46;
    private static final int TEXT = 0xFFF4F2FF;
    private static final int GOLD = 0xFFF2B33D;
    private static final int GOLD_TINT = 0x55F2B33D;

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
            if (isPopupOpen(screen, group)) {
                renderPopup(graphics, group, mouseX, mouseY);
            }
            if (isSelecting(screen, group)) {
                renderSelection(graphics, screen, group);
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

    /** Outlines the group's locked slots in gold, and says how to finish. */
    private static void renderSelection(GuiGraphics g, AbstractContainerScreen<?> screen, Group group) {
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
            g.fill(x, y, x + 16, y + 16, GOLD_TINT);
            g.fill(x - 1, y - 1, x + 17, y, GOLD);
            g.fill(x - 1, y + 16, x + 17, y + 17, GOLD);
            g.fill(x - 1, y, x, y + 16, GOLD);
            g.fill(x + 16, y, x + 17, y + 16, GOLD);
        }
        g.pose().pushPose();
        g.pose().translate(group.left(), group.top() - 22, 0);
        g.pose().scale(TEXT_SCALE, TEXT_SCALE, 1f);
        g.drawString(Minecraft.getInstance().font, "Click slots to lock them. Esc to finish.", 0, 0, GOLD, false);
        g.pose().popPose();
    }

    /** The popup hangs below the settings button, lined up with its right edge, and stays on screen. */
    private static int popupLeft(Group group) {
        int left = group.menuButtonX() + ButtonWidget.SIZE - POPUP_WIDTH;
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
        return mouseX >= left && mouseX < left + POPUP_WIDTH && mouseY >= top && mouseY < top + popupHeight(group);
    }

    private static void renderPopup(GuiGraphics g, Group group, double mouseX, double mouseY) {
        int left = popupLeft(group);
        int top = popupTop(group);
        GroupSettings settings = ClientSettings.get(group.settingsKey());
        List<String> labels = new java.util.ArrayList<>(List.of(
                methodLabel(settings), keyLabel(settings), slotsLabel(settings)));
        if (rowCount(group) == 4) {
            labels.add(toolsLabel(settings));
        }

        g.fill(left - 1, top - 1, left + POPUP_WIDTH + 1, top + popupHeight(group) + 1, FRAME);
        g.fill(left, top, left + POPUP_WIDTH, top + popupHeight(group), FACE);

        var font = Minecraft.getInstance().font;
        for (int row = 0; row < labels.size(); row++) {
            int rowTop = top + 2 + row * ROW_HEIGHT;
            boolean hover = mouseX >= left && mouseX < left + POPUP_WIDTH
                    && mouseY >= rowTop && mouseY < rowTop + ROW_HEIGHT;
            if (hover) {
                g.fill(left + 1, rowTop, left + POPUP_WIDTH - 1, rowTop + ROW_HEIGHT, HOVER_FACE);
            }
            g.pose().pushPose();
            g.pose().translate(left + 4, rowTop + 2, 0);
            g.pose().scale(TEXT_SCALE, TEXT_SCALE, 1f);
            g.drawString(font, labels.get(row), 0, 0, TEXT, false);
            g.pose().popPose();
        }
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
        return "Sort method: " + (settings.method().name().equals("ROWS") ? "Rows" : "Columns");
    }

    private static String keyLabel(GroupSettings settings) {
        return "Sort by: " + switch (settings.key()) {
            case QUANTITY -> "Quantity";
            case NAME -> "Item name";
            case MOD -> "Mod name";
        };
    }

    private static String slotsLabel(GroupSettings settings) {
        return "Locked slots: " + settings.lockedSlots().size() + " (choose)";
    }

    private static String toolsLabel(GroupSettings settings) {
        return settings.replaceAtOne() ? "Tools: swap at 1 durability" : "Tools: swap when broken";
    }
}
