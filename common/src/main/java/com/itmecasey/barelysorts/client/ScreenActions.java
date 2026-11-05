package com.itmecasey.barelysorts.client;

import com.itmecasey.barelysorts.GroupSettings;
import com.itmecasey.barelysorts.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * What the buttons, the settings popup and the hotkeys do on a container screen. The loader layers forward their
 * screen events here, so the behavior is written once. Buttons and the popup are drawn and hit-tested by hand.
 */
public final class ScreenActions {
    private static final int LEFT_BUTTON = 0;
    private static final int MIDDLE_BUTTON = 2;

    private static final int POPUP_WIDTH = 104;
    private static final int ROW_HEIGHT = 12;
    private static final int ROWS = 3;
    private static final int EDGE = 2;
    private static final float TEXT_SCALE = 0.8f;
    private static final int FRAME = 0xFF2E2A46;
    private static final int FACE = 0xFF1A1830;
    private static final int HOVER_FACE = 0xFF2E2A46;
    private static final int TEXT = 0xFFF4F2FF;

    /** The settings popup is open for one group of one screen, or nowhere. */
    private static AbstractContainerScreen<?> popupScreen;
    private static String popupKey;

    private ScreenActions() {
    }

    public static void render(AbstractContainerScreen<?> screen, GuiGraphics graphics, double mouseX, double mouseY) {
        String tooltip = null;
        for (Group group : Group.of(screen)) {
            boolean locked = ClientSettings.get(group.settingsKey()).locked();
            boolean sortHover = ButtonWidget.inside(mouseX, mouseY, group.sortButtonX(), group.sortButtonY());
            boolean menuHover = ButtonWidget.inside(mouseX, mouseY, group.menuButtonX(), group.menuButtonY());
            ButtonWidget.drawSort(graphics, group.sortButtonX(), group.sortButtonY(), sortHover, locked);
            ButtonWidget.drawMenu(graphics, group.menuButtonX(), group.menuButtonY(), menuHover);
            if (sortHover) {
                tooltip = locked ? "Locked: sorting is off" : "Sort this group";
            } else if (menuHover) {
                tooltip = "Sort settings";
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
                    toggle(group, row);
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
     * Escape closes an open settings popup. R sorts the group under the mouse; away from a group it does nothing, so
     * the recipe book key keeps working. Returns true when the key was used.
     */
    public static boolean key(AbstractContainerScreen<?> screen, int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && popupScreen == screen) {
            closePopup();
            return true;
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
        if (settings.locked()) {
            return;
        }
        Platform.sendSort(screen.getMenu().containerId, group.name(), settings.method().name(), settings.key().name());
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

    private static boolean isPopupOpen(AbstractContainerScreen<?> screen, Group group) {
        return popupScreen == screen && group.settingsKey().equals(popupKey);
    }

    /** The popup hangs below the settings button, lined up with its right edge, and stays on screen. */
    private static int popupLeft(Group group) {
        int left = group.menuButtonX() + ButtonWidget.SIZE - POPUP_WIDTH;
        return Math.max(EDGE, left);
    }

    private static int popupTop(Group group) {
        return group.menuButtonY() + ButtonWidget.SIZE + 2;
    }

    private static int popupHeight() {
        return ROWS * ROW_HEIGHT + 4;
    }

    private static boolean insidePopup(Group group, double mouseX, double mouseY) {
        int left = popupLeft(group);
        int top = popupTop(group);
        return mouseX >= left && mouseX < left + POPUP_WIDTH && mouseY >= top && mouseY < top + popupHeight();
    }

    private static void renderPopup(GuiGraphics g, Group group, double mouseX, double mouseY) {
        int left = popupLeft(group);
        int top = popupTop(group);
        GroupSettings settings = ClientSettings.get(group.settingsKey());
        String[] labels = {methodLabel(settings), keyLabel(settings), lockLabel(settings)};

        g.fill(left - 1, top - 1, left + POPUP_WIDTH + 1, top + popupHeight() + 1, FRAME);
        g.fill(left, top, left + POPUP_WIDTH, top + popupHeight(), FACE);

        var font = Minecraft.getInstance().font;
        for (int row = 0; row < ROWS; row++) {
            int rowTop = top + 2 + row * ROW_HEIGHT;
            boolean hover = mouseX >= left && mouseX < left + POPUP_WIDTH
                    && mouseY >= rowTop && mouseY < rowTop + ROW_HEIGHT;
            if (hover) {
                g.fill(left + 1, rowTop, left + POPUP_WIDTH - 1, rowTop + ROW_HEIGHT, HOVER_FACE);
            }
            g.pose().pushPose();
            g.pose().translate(left + 4, rowTop + 2, 0);
            g.pose().scale(TEXT_SCALE, TEXT_SCALE, 1f);
            g.drawString(font, labels[row], 0, 0, TEXT, false);
            g.pose().popPose();
        }
    }

    private static void toggle(Group group, int row) {
        GroupSettings settings = ClientSettings.get(group.settingsKey());
        GroupSettings updated = switch (row) {
            case 0 -> settings.withMethod(settings.method().next());
            case 1 -> settings.withKey(settings.key().next());
            case 2 -> settings.withLocked(!settings.locked());
            default -> settings;
        };
        ClientSettings.put(group.settingsKey(), updated);
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

    private static String lockLabel(GroupSettings settings) {
        return "Lock: " + (settings.locked() ? "On" : "Off");
    }
}
