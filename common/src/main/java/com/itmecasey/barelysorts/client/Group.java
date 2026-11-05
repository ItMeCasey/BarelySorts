package com.itmecasey.barelysorts.client;

import com.itmecasey.barelysorts.Platform;
import com.itmecasey.barelysorts.Sorter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

/**
 * One inventory group on a container screen: either the player's main inventory or the container's own slots.
 * Coordinates are screen pixels, so they can be compared with the mouse position directly.
 */
public record Group(String name, String settingsKey, List<Slot> slots, int left, int top, int right, int bottom) {

    /** Every group the open screen shows, or none for screens that cannot be sorted (creative, for one). */
    public static List<Group> of(AbstractContainerScreen<?> screen) {
        List<Group> groups = new ArrayList<>();
        if (screen instanceof CreativeModeInventoryScreen || Minecraft.getInstance().player == null) {
            return groups;
        }
        var inventory = Minecraft.getInstance().player.getInventory();
        List<Slot> playerMain = new ArrayList<>();
        List<Slot> container = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container == inventory) {
                if (slot.getContainerSlot() >= 9 && slot.getContainerSlot() <= 35) {
                    playerMain.add(slot);
                }
            } else if (!(slot instanceof ResultSlot)) {
                container.add(slot);
            }
        }
        if (!playerMain.isEmpty()) {
            groups.add(bounds(Sorter.GROUP_PLAYER, "player", playerMain, screen));
        }
        if (!container.isEmpty()) {
            groups.add(bounds(Sorter.GROUP_CONTAINER, screen.getClass().getName(), container, screen));
        }
        return groups;
    }

    private static Group bounds(String name, String settingsKey, List<Slot> slots, AbstractContainerScreen<?> screen) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Slot slot : slots) {
            minX = Math.min(minX, slot.x);
            minY = Math.min(minY, slot.y);
            maxX = Math.max(maxX, slot.x + 16);
            maxY = Math.max(maxY, slot.y + 16);
        }
        int guiLeft = Platform.guiOrigin().left(screen);
        int guiTop = Platform.guiOrigin().top(screen);
        int left = guiLeft + minX;
        int top = guiTop + minY;
        int right = guiLeft + maxX;
        int bottom = guiTop + maxY;
        return new Group(name, settingsKey, slots, left, top, right, bottom);
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= left - 2 && mouseX < right + 2 && mouseY >= top - 2 && mouseY < bottom + 2;
    }

    /** The sort button sits just right of the group, at its top. */
    public int sortButtonX() {
        return right + 3;
    }

    public int sortButtonY() {
        return top;
    }

    /** The settings button sits under the sort button. */
    public int settingsButtonY() {
        return top + ButtonWidget.SIZE + 2;
    }
}
