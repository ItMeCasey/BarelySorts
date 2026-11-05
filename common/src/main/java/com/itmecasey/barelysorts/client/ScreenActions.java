package com.itmecasey.barelysorts.client;

import com.itmecasey.barelysorts.GroupSettings;
import com.itmecasey.barelysorts.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.List;

/**
 * What the buttons and hotkeys do on a container screen. The loader layers forward their screen events here, so the
 * behavior is written once. The buttons are drawn and hit-tested by hand, not added as widgets.
 */
public final class ScreenActions {
    private static final int LEFT_BUTTON = 0;
    private static final int MIDDLE_BUTTON = 2;

    private ScreenActions() {
    }

    public static void render(AbstractContainerScreen<?> screen, GuiGraphics graphics, double mouseX, double mouseY) {
        for (Group group : Group.of(screen)) {
            boolean locked = ClientSettings.get(group.settingsKey()).locked();
            boolean sortHover = ButtonWidget.inside(mouseX, mouseY, group.sortButtonX(), group.sortButtonY());
            boolean menuHover = ButtonWidget.inside(mouseX, mouseY, group.sortButtonX(), group.settingsButtonY());
            ButtonWidget.drawSort(graphics, group.sortButtonX(), group.sortButtonY(), sortHover, locked);
            ButtonWidget.drawMenu(graphics, group.sortButtonX(), group.settingsButtonY(), menuHover);
        }
    }

    /** Returns true when the click was used, so the loader can cancel it. */
    public static boolean mouseButton(AbstractContainerScreen<?> screen, double mouseX, double mouseY, int button) {
        for (Group group : Group.of(screen)) {
            if (button == LEFT_BUTTON
                    && ButtonWidget.inside(mouseX, mouseY, group.sortButtonX(), group.settingsButtonY())) {
                Minecraft.getInstance().setScreen(new SettingsScreen(screen, group.settingsKey(), group.name()));
                return true;
            }
            if (button == LEFT_BUTTON && ButtonWidget.inside(mouseX, mouseY, group.sortButtonX(), group.sortButtonY())) {
                sort(screen, group);
                return true;
            }
            if (button == MIDDLE_BUTTON && group.contains(mouseX, mouseY)) {
                sort(screen, group);
                return true;
            }
        }
        return false;
    }

    /**
     * R sorts the group under the mouse. Away from a group it does nothing, so the recipe book key keeps working.
     * Returns true when a group was sorted.
     */
    public static boolean keyR(AbstractContainerScreen<?> screen) {
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

}
