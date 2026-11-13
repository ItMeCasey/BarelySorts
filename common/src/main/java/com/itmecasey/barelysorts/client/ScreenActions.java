package com.itmecasey.barelysorts.client;

import com.itmecasey.barelysorts.GroupSettings;
import com.itmecasey.barelysorts.Compat;
import com.itmecasey.barelysorts.Platform;
import com.itmecasey.barelysorts.Sorter;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * What the buttons, the settings popup, slot locking, delete marking and the hotkeys do on a container screen. The
 * loader layers forward their screen events here, so the behavior is written once. Everything is drawn and hit-tested
 * by hand.
 */
public final class ScreenActions {
    /** The left button is 0 in older versions and 1 in 26.3, so read it from the game. */
    private static final int LEFT_BUTTON = InputConstants.MOUSE_BUTTON_LEFT;

    private static final int ROW_HEIGHT = 12;
    private static final int ROW_PADDING = 6;
    /** Where the popup text's top sits inside its row, in pixels. Tuned by eye against the row highlight. */
    private static final int ROW_TEXT_OFFSET = 4;
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
    /** How far overlays are lifted in front of the item stacks. */
    /**
     * Overlay height on versions that draw with a z offset: above item stacks, their counts and the carried
     * item (counts reach about 350), but below tooltips, which the game draws at 400. 26.x orders overlays by stratum instead and ignores this.
     */
    private static final float OVERLAY_Z = 399f;
    private static final int RED = 0xFFE5484D;
    private static final int RED_TINT = 0x55E5484D;
    private static final int DIALOG_WIDTH = 178;
    private static final int DIALOG_COLUMNS = 9;
    private static final int DIALOG_MAX_SHOWN = 27;
    private static final int BUTTON_WIDTH = 56;
    private static final int BUTTON_HEIGHT = 14;

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

    /** Holding X marks slots for deletion. Releasing X asks for confirmation. */
    private static boolean xWasDown;
    private static AbstractContainerScreen<?> markScreen;
    private static final Set<Integer> marked = new HashSet<>();

    /** Slots already quick-moved during the current hold of Shift, and the screen they belong to. */
    private static final Set<Integer> dragged = new HashSet<>();
    private static AbstractContainerScreen<?> dragScreen;

    /** The delete confirmation is open for one screen, with these menu slots. */
    private static AbstractContainerScreen<?> confirmScreen;
    private static List<Integer> confirmSlots = List.of();

    private ScreenActions() {
    }

    public static void render(AbstractContainerScreen<?> screen, Draw graphics, double mouseX, double mouseY) {
        updateMarking(screen);
        updateShiftDrag(screen, mouseX, mouseY);

        // Everything below is drawn above the item stacks, which the game draws higher than plain fills.
        graphics.push();
        graphics.lift(OVERLAY_Z);
        drawAllLocks(graphics, screen);
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
                drawSelectionHint(graphics, group);
            }
            drawMarked(graphics, screen, group);
            if (isPopupOpen(screen, group)) {
                renderPopup(graphics, group, mouseX, mouseY);
            }
        }
        if (confirmScreen == screen) {
            renderConfirm(graphics, screen, mouseX, mouseY);
        }
        graphics.pop();
        // Tooltips are for the two buttons only. The popup rows have none.
        if (tooltip != null && confirmScreen != screen) {
            graphics.tooltip(Component.literal(tooltip), (int) mouseX, (int) mouseY);
        }
    }

    /** Returns true when the click was used, so the loader can cancel it. */
    public static boolean mouseButton(AbstractContainerScreen<?> screen, double mouseX, double mouseY, int button) {
        noteHotbarClick(screen, mouseX, mouseY);
        if (confirmScreen == screen) {
            if (button == LEFT_BUTTON) {
                int left = confirmLeft(screen);
                int buttonY = confirmButtonY(screen);
                if (inside(mouseX, mouseY, left + 12, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                    deleteConfirmed();
                } else if (inside(mouseX, mouseY, left + DIALOG_WIDTH - 12 - BUTTON_WIDTH, buttonY,
                        BUTTON_WIDTH, BUTTON_HEIGHT)) {
                    closeConfirm();
                }
            }
            // The dialog is modal: clicks outside it do nothing until it is answered.
            return true;
        }
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
        // Ctrl-click moves one item (Ctrl+Shift moves every item of that type). Shift-click moves the stack, as usual.
        // Output slots (crafting results and the like) are left to the game, which crafts the whole stack on
        // Shift-click and has its own rules for Ctrl-click. Our quick move cannot take items out of them.
        Slot hovered = slotUnder(screen, mouseX, mouseY);
        if (button == LEFT_BUTTON && hovered != null && !Sorter.isOutputSlot(hovered)
                && (KeyBindings.ctrlDown() || KeyBindings.shiftDown())) {
            boolean ctrl = KeyBindings.ctrlDown();
            boolean shift = KeyBindings.shiftDown();
            if (ctrl) {
                sendQuickMove(screen, hovered, shift ? "type" : "one");
            } else {
                dragged.add(hovered.index);
                sendQuickMove(screen, hovered, "stack");
            }
            return true;
        }
        if (markScreen == screen && isXDown() && button == LEFT_BUTTON) {
            for (Group group : Group.of(screen)) {
                Slot slot = slotAt(screen, group, mouseX, mouseY);
                if (slot != null) {
                    toggleMark(slot.index);
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
            if (KeyBindings.isSortClick(button) && group.contains(mouseX, mouseY)) {
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
     * Escape closes the delete dialog, the popup, or slot selection. R sorts the group under the mouse; away from a
     * group it does nothing, so the recipe book key keeps working. Returns true when the key was used.
     */
    public static boolean key(AbstractContainerScreen<?> screen, int keyCode, Predicate<KeyMapping> sortKeyMatches) {
        if (keyCode == InputConstants.KEY_ESCAPE) {
            if (confirmScreen == screen) {
                closeConfirm();
                return true;
            }
            if (selectScreen == screen) {
                stopSelecting();
                return true;
            }
            if (popupScreen == screen) {
                closePopup();
                return true;
            }
        }
        if (sortKeyMatches.test(KeyBindings.SORT)) {
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

    // ---- Delete marking: hold X, click slots to mark them red, release X to confirm ----

    private static boolean isXDown() {
        return KeyBindings.isDown(KeyBindings.MARK);
    }

    /** Starts a new mark when X goes down, and opens the confirmation when it comes back up. */
    private static void updateMarking(AbstractContainerScreen<?> screen) {
        boolean xDown = isXDown();
        if (xDown && !xWasDown) {
            marked.clear();
            markScreen = screen;
        } else if (!xDown && xWasDown && markScreen == screen) {
            markScreen = null;
            if (!marked.isEmpty() && confirmScreen == null) {
                openConfirm(screen);
            }
            marked.clear();
        }
        xWasDown = xDown;
    }

    /**
     * Sends a quick move for one slot. Locked slots are not sent at all. Every locked slot on the screen travels with
     * the request as a menu index, on either side, so the server applies the same rule to the source and destinations.
     */
    private static void sendQuickMove(AbstractContainerScreen<?> screen, Slot slot, String mode) {
        Set<Integer> locked = lockedMenuSlots(screen);
        if (locked.contains(slot.index)) {
            return;
        }
        String lockedList = String.join(",", locked.stream().map(String::valueOf).toList());
        Platform.sendSort(screen.getMenu().containerId, Sorter.QUICK_MOVE, mode, lockedList,
                String.valueOf(slot.index));
    }

    /** Menu indexes of every locked slot on the screen, from both the player and the container side. */
    private static Set<Integer> lockedMenuSlots(AbstractContainerScreen<?> screen) {
        var inventory = Minecraft.getInstance().player.getInventory();
        GroupSettings playerSettings = ClientSettings.get(Sorter.GROUP_PLAYER);
        GroupSettings containerSettings = ClientSettings.get(Group.containerKey(screen));
        Set<Integer> out = new HashSet<>();
        for (Slot s : screen.getMenu().slots) {
            GroupSettings settings = s.container == inventory ? playerSettings : containerSettings;
            if (settings.isLocked(Sorter.lockId(s, inventory))) {
                out.add(s.index);
            }
        }
        return out;
    }

    /**
     * Holding Shift and moving over items quick-moves each one, once per hold. Releasing Shift starts over.
     */
    private static void updateShiftDrag(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        // Shift-drag needs the left mouse button held as well, so moving the mouse with Shift alone does nothing.
        boolean leftHeld = Compat.leftMouseDown();
        if (!KeyBindings.shiftDown() || !leftHeld) {
            dragged.clear();
            dragScreen = null;
            return;
        }
        if (dragScreen != screen) {
            dragged.clear();
            dragScreen = screen;
        }
        if (confirmScreen == screen || popupScreen == screen || selectScreen == screen) {
            return;
        }
        Slot slot = slotUnder(screen, mouseX, mouseY);
        if (slot != null && slot.hasItem() && !Sorter.isOutputSlot(slot) && dragged.add(slot.index)) {
            sendQuickMove(screen, slot, "stack");
        }
    }

    private static void toggleMark(int menuSlot) {
        if (!marked.remove(menuSlot)) {
            marked.add(menuSlot);
        }
    }

    /** Outlines marked slots of the group in red. */
    private static void drawMarked(Draw g, AbstractContainerScreen<?> screen, Group group) {
        if (marked.isEmpty()) {
            return;
        }
        int guiLeft = Platform.guiOrigin().left(screen);
        int guiTop = Platform.guiOrigin().top(screen);
        for (Slot slot : group.slots()) {
            if (!marked.contains(slot.index)) {
                continue;
            }
            int x = guiLeft + slot.x;
            int y = guiTop + slot.y;
            g.fill(x, y, x + 16, y + 16, RED_TINT);
            g.fill(x - 1, y - 1, x + 17, y, RED);
            g.fill(x - 1, y + 16, x + 17, y + 17, RED);
            g.fill(x - 1, y, x, y + 16, RED);
            g.fill(x + 16, y, x + 17, y + 16, RED);
        }
    }

    private static void openConfirm(AbstractContainerScreen<?> screen) {
        List<Integer> withItems = new ArrayList<>();
        for (int index : marked) {
            if (index >= 0 && index < screen.getMenu().slots.size() && screen.getMenu().getSlot(index).hasItem()) {
                withItems.add(index);
            }
        }
        if (withItems.isEmpty()) {
            return;
        }
        confirmScreen = screen;
        confirmSlots = List.copyOf(withItems);
    }

    private static void deleteConfirmed() {
        if (confirmScreen != null) {
            Platform.sendSort(confirmScreen.getMenu().containerId, Sorter.DELETE, "",
                    "", confirmSlots.stream().map(String::valueOf).collect(Collectors.joining(",")));
        }
        closeConfirm();
    }

    private static void closeConfirm() {
        confirmScreen = null;
        confirmSlots = List.of();
    }

    /** The dialog shows up to DIALOG_MAX_SHOWN stacks, DIALOG_COLUMNS to a row. */
    private static int shownStacks() {
        return Math.min(confirmSlots.size(), DIALOG_MAX_SHOWN);
    }

    private static int itemRows() {
        return Math.max(1, (shownStacks() + DIALOG_COLUMNS - 1) / DIALOG_COLUMNS);
    }

    private static boolean overflowing() {
        return confirmSlots.size() > DIALOG_MAX_SHOWN;
    }

    /** Title, item rows, an optional "+N more" line, then the warning and the buttons. */
    private static int confirmHeight() {
        return 20 + itemRows() * 18 + (overflowing() ? 10 : 0) + 40;
    }

    private static int confirmLeft(AbstractContainerScreen<?> screen) {
        return screen.width / 2 - DIALOG_WIDTH / 2;
    }

    private static int confirmTop(AbstractContainerScreen<?> screen) {
        return screen.height / 2 - confirmHeight() / 2;
    }

    private static int confirmButtonY(AbstractContainerScreen<?> screen) {
        return confirmTop(screen) + confirmHeight() - 22;
    }

    private static void renderConfirm(Draw g, AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        int left = confirmLeft(screen);
        int top = confirmTop(screen);
        int height = confirmHeight();
        Font font = Minecraft.getInstance().font;
        int count = confirmSlots.size();

        g.fill(left - 1, top - 1, left + DIALOG_WIDTH + 1, top + height + 1, FRAME);
        g.fill(left, top, left + DIALOG_WIDTH, top + height, FACE);
        g.centeredText(small("Delete " + count + (count == 1 ? " stack?" : " stacks?")),
                left + DIALOG_WIDTH / 2, top + 7, TEXT);

        // The actual stacks, with their counts, so you can see exactly what goes.
        int shown = shownStacks();
        for (int i = 0; i < shown; i++) {
            ItemStack stack = screen.getMenu().getSlot(confirmSlots.get(i)).getItem();
            int x = left + 8 + (i % DIALOG_COLUMNS) * 18;
            int y = top + 20 + (i / DIALOG_COLUMNS) * 18;
            g.item(stack, x, y);
        }
        if (overflowing()) {
            g.centeredText(small("+" + (count - shown) + " more"), left + DIALOG_WIDTH / 2,
                    top + 20 + itemRows() * 18 + 2, TEXT);
        }
        g.centeredText(small("This cannot be undone."), left + DIALOG_WIDTH / 2, top + height - 33, TEXT);

        int buttonY = top + height - 22;
        drawButton(g, font, left + 12, buttonY, "Delete", RED, mouseX, mouseY);
        drawButton(g, font, left + DIALOG_WIDTH - 12 - BUTTON_WIDTH, buttonY, "Cancel", GOLD, mouseX, mouseY);
    }

    private static void drawButton(Draw g, Font font, int x, int y, String label, int color,
                                   double mouseX, double mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
        g.fill(x - 1, y - 1, x + BUTTON_WIDTH + 1, y + BUTTON_HEIGHT + 1, color);
        g.fill(x, y, x + BUTTON_WIDTH, y + BUTTON_HEIGHT, hover ? HOVER_FACE : FACE);
        g.centeredText(small(label), x + BUTTON_WIDTH / 2, y + 3, color);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    // ---- Sorting, settings, and slot locking ----

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

    /**
     * Tells the server when the player clicks a hotbar slot, so autofill does not refill the slot they just emptied.
     * The click itself is not consumed.
     */
    private static void noteHotbarClick(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        Slot slot = slotUnder(screen, mouseX, mouseY);
        var inventory = Minecraft.getInstance().player.getInventory();
        if (slot != null && slot.container == inventory && slot.getContainerSlot() >= 0
                && slot.getContainerSlot() <= 8) {
            Platform.sendSort(screen.getMenu().containerId, Sorter.MANUAL, "", "",
                    String.valueOf(slot.getContainerSlot()));
        }
    }

    /**
     * The slot of the screen under the mouse, or null. Computed here rather than read from the screen, because
     * Fabric does not expose the screen's hovered slot.
     */
    private static Slot slotUnder(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        int guiLeft = Platform.guiOrigin().left(screen);
        int guiTop = Platform.guiOrigin().top(screen);
        for (Slot slot : screen.getMenu().slots) {
            int x = guiLeft + slot.x;
            int y = guiTop + slot.y;
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                return slot;
            }
        }
        return null;
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
        GroupSettings updated = settings.toggleSlot(Sorter.lockId(slot, inventory));
        ClientSettings.put(group.settingsKey(), updated);
        // Player slots also limit autofill, so the server needs the new locks right away.
        if (Sorter.GROUP_PLAYER.equals(group.settingsKey())) {
            AutofillSync.send(updated);
        }
    }

    /**
     * Outlines every locked slot on the screen, on any screen: storage, the player inventory, workstations and the
     * creative screen. Player locks apply everywhere. Container locks apply to the container they were set on. The
     * slots of the group being edited are drawn in strong gold; the rest are faint.
     */
    private static void drawAllLocks(Draw g, AbstractContainerScreen<?> screen) {
        var inventory = Minecraft.getInstance().player.getInventory();
        String containerKey = Group.containerKey(screen);
        GroupSettings playerSettings = ClientSettings.get(Sorter.GROUP_PLAYER);
        GroupSettings containerSettings = ClientSettings.get(containerKey);
        int guiLeft = Platform.guiOrigin().left(screen);
        int guiTop = Platform.guiOrigin().top(screen);
        boolean selecting = selectScreen == screen;
        for (Slot slot : screen.getMenu().slots) {
            boolean playerSide = slot.container == inventory;
            GroupSettings settings = playerSide ? playerSettings : containerSettings;
            if (!settings.isLocked(Sorter.lockId(slot, inventory))) {
                continue;
            }
            String slotKey = playerSide ? Sorter.GROUP_PLAYER : containerKey;
            boolean strong = selecting && slotKey.equals(selectKey);
            int tint = strong ? GOLD_TINT : LOCKED_FAINT_TINT;
            int edge = strong ? GOLD : LOCKED_FAINT;
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
    private static void drawSelectionHint(Draw g, Group group) {
        Font font = Minecraft.getInstance().font;
        Component text = small("Click slots to lock. Esc to finish.");
        int x = group.left();
        int y = group.top() - 14;
        int width = font.width(text);
        // A dark plate behind the text keeps it readable over the inventory art.
        g.fill(x - 3, y - 2, x + width + 3, y + font.lineHeight + 2, FACE);
        g.text(text, x, y, GOLD);
    }

    private static Component small(String text) {
        return Component.literal(text).withStyle(Compat.smallFont());
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

    private static void renderPopup(Draw g, Group group, double mouseX, double mouseY) {
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
            // Left-aligned. The pixel font sits high in its line box, so it is nudged down to center on the row.
            g.text(small(labels.get(row)), left + ROW_PADDING, rowTop + ROW_TEXT_OFFSET, TEXT);
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
                AutofillSync.send(updated);
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
