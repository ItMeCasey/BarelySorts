package com.itmecasey.barelysorts;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-side sorting of one inventory group. The client only asks; this class decides what the slots hold.
 * The hotbar (player slots 0 to 8) is never touched, and the armor, offhand and crafting result slots are skipped.
 */
public final class Sorter {
    public static final String GROUP_PLAYER = "player";
    public static final String GROUP_CONTAINER = "container";
    public static final String AUTOFILL = "autofill";
    public static final String DELETE = "delete";
    public static final String QUICK_MOVE = "quickmove";
    public static final String MANUAL = "manual";

    /** Player main inventory is slots 9 to 35 of the player's Inventory. */
    private static final int MAIN_START = 9;
    private static final int MAIN_END = 35;

    private Sorter() {
    }

    /**
     * Whether a menu may be sorted or emptied. The creative item picker is client-only, and Applied Energistics 2
     * terminals show a virtual network grid, so neither is touched. Anything else, including other mods' storage, is.
     */
    public static boolean isSupported(AbstractContainerMenu menu) {
        for (Class<?> type = menu.getClass(); type != null; type = type.getSuperclass()) {
            String name = type.getName();
            if (name.startsWith("appeng.") || name.endsWith("ItemPickerMenu")) {
                return false;
            }
        }
        return true;
    }

    /**
     * The storage container of a menu: the one with the most slots outside the player inventory, crafting and results.
     * A storage's side panels, such as upgrade slots, are a separate container and are left out.
     */
    public static Container mainContainer(AbstractContainerMenu menu, Inventory inventory) {
        Map<Container, Integer> counts = new HashMap<>();
        for (Slot slot : menu.slots) {
            if (slot.container != inventory && !(slot instanceof ResultSlot)
                    && !(slot.container instanceof CraftingContainer)) {
                counts.merge(slot.container, 1, Integer::sum);
            }
        }
        return counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }

    public static void sortGroup(ServerPlayer player, int containerId, String group, String methodName, String keyName,
                                 String lockedIds) {
        // Not a sort: the client reports a hotbar slot the player clicked, so autofill does not put it back.
        if (MANUAL.equals(group)) {
            Autofill.markManual(player.getUUID(), parseLocked(lockedIds));
            return;
        }
        // Not a sort: a quick move of one slot, with the mode in the method field ("one", "type" or "stack").
        if (QUICK_MOVE.equals(group)) {
            // For a quick move, lockedIds is the one slot, and keyName carries that side's locked slots.
            quickMove(player, containerId, methodName, lockedIds, keyName);
            return;
        }
        // Not a sort: the client sends the slots to delete under this group name, as menu slot indexes.
        if (DELETE.equals(group)) {
            deleteSlots(player, containerId, lockedIds);
            return;
        }
        // Not a sort: the client sends its autofill setting ("true" or "false") under this group name.
        if (AUTOFILL.equals(group)) {
            Autofill.setSettings(player, "true".equals(methodName), parseLocked(lockedIds));
            return;
        }
        AbstractContainerMenu menu = player.containerMenu;
        // The creative item picker is a client-only menu; its slots hold infinite stacks and are never sorted.
        if (menu.containerId != containerId || !isSupported(menu)) {
            return;
        }
        // Locked slots keep their contents and are left out of the sort entirely.
        java.util.Set<Integer> locked = parseLocked(lockedIds);
        Inventory inventory = player.getInventory();
        List<Slot> slots = new ArrayList<>();
        for (Slot slot : groupSlots(menu, player, group)) {
            if (!locked.contains(lockId(slot, inventory))) {
                slots.add(slot);
            }
        }
        if (slots.isEmpty()) {
            return;
        }
        SortMethod method = SortMethod.parse(methodName);
        SortKey key = SortKey.parse(keyName);

        List<ItemStack> stacks = new ArrayList<>();
        for (Slot slot : slots) {
            stacks.add(slot.getItem());
        }
        List<ItemStack> arranged = arrange(stacks, slots, key);

        // Container slots can refuse some items (a furnace's fuel slot, for example). Do not move anything into a slot
        // that would refuse its new contents.
        for (int i = 0; i < arranged.size(); i++) {
            ItemStack stack = arranged.get(i);
            if (!stack.isEmpty() && !slots.get(i).mayPlace(stack)) {
                return;
            }
        }

        List<Slot> ordered = new ArrayList<>(slots);
        ordered.sort(method == SortMethod.ROWS
                ? Comparator.comparingInt((Slot s) -> s.y).thenComparingInt(s -> s.x)
                : Comparator.comparingInt((Slot s) -> s.x).thenComparingInt(s -> s.y));

        // Slots are read in group order, so the arrangement lines up with the slot list above; write it out in
        // the chosen fill order.
        Map<Slot, ItemStack> target = new HashMap<>();
        for (int i = 0; i < ordered.size(); i++) {
            target.put(ordered.get(i), i < arranged.size() ? arranged.get(i) : ItemStack.EMPTY);
        }
        for (Slot slot : slots) {
            slot.set(target.get(slot));
        }
        menu.broadcastChanges();
    }

    /**
     * Quick-moves one slot, the way shift-click does. Mode "stack" moves the whole stack, "one" moves a single item,
     * and "type" moves every item of the clicked type from the same side. Result slots are never moved.
     */
    static void quickMove(ServerPlayer player, int containerId, String mode, String slotIds, String lockedList) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu.containerId != containerId || !isSupported(menu)) {
            return;
        }
        var ids = parseLocked(slotIds);
        if (ids.size() != 1) {
            return;
        }
        int index = ids.iterator().next();
        if (index < 0 || index >= menu.slots.size()) {
            return;
        }
        Slot source = menu.getSlot(index);
        if (source instanceof ResultSlot || !source.hasItem() || !source.mayPickup(player)) {
            return;
        }
        // The locked list holds menu slot indexes for both sides. Locked slots never give up or receive items.
        var locked = parseLocked(lockedList);
        if (locked.contains(index)) {
            return;
        }
        if (source.container == player.getInventory() && source.getContainerSlot() <= 8) {
            Autofill.markManual(player.getUUID(), java.util.Set.of(source.getContainerSlot()));
        }
        switch (mode) {
            case "one" -> moveOne(player, menu, source, locked);
            case "type" -> moveType(player, menu, source, locked);
            default -> guardedQuickMove(player, menu, index, locked);
        }
        menu.broadcastChanges();
    }

    /** Moves one item out of the slot: the rest of the stack stays where it was. */
    private static void moveOne(ServerPlayer player, AbstractContainerMenu menu, Slot source,
                                java.util.Set<Integer> locked) {
        ItemStack original = source.getItem().copy();
        int count = original.getCount();
        source.set(original.copyWithCount(1));
        guardedQuickMove(player, menu, source.index, locked);
        int moved = 1 - source.getItem().getCount();
        source.set(original.copyWithCount(count - moved));
    }

    /** Moves every stack of the clicked item from the same side (player inventory or the container) as it. */
    private static void moveType(ServerPlayer player, AbstractContainerMenu menu, Slot source,
                                 java.util.Set<Integer> locked) {
        ItemStack template = source.getItem().copy();
        Inventory inventory = player.getInventory();
        boolean fromPlayer = source.container == inventory;
        List<Integer> matches = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (!(slot instanceof ResultSlot) && slot.hasItem() && (slot.container == inventory) == fromPlayer
                    && !locked.contains(slot.index)
                    && ItemStack.isSameItemSameComponents(slot.getItem(), template)) {
                matches.add(slot.index);
            }
        }
        for (int index : matches) {
            Slot slot = menu.getSlot(index);
            if (slot.hasItem() && ItemStack.isSameItemSameComponents(slot.getItem(), template)) {
                guardedQuickMove(player, menu, index, locked);
            }
        }
    }

    /**
     * Runs a vanilla quick move, then takes back anything it put into a locked slot. Vanilla picks the destination
     * itself, so the check has to come after the move.
     */
    private static void guardedQuickMove(ServerPlayer player, AbstractContainerMenu menu, int index,
                                         java.util.Set<Integer> locked) {
        Map<Integer, ItemStack> before = new HashMap<>();
        for (int lockedIndex : locked) {
            if (lockedIndex >= 0 && lockedIndex < menu.slots.size()) {
                before.put(lockedIndex, menu.getSlot(lockedIndex).getItem().copy());
            }
        }
        menu.quickMoveStack(player, index);
        for (Map.Entry<Integer, ItemStack> entry : before.entrySet()) {
            Slot slot = menu.getSlot(entry.getKey());
            ItemStack now = slot.getItem();
            ItemStack was = entry.getValue();
            boolean sameItem = ItemStack.isSameItemSameComponents(now, was);
            if (sameItem && now.getCount() == was.getCount()) {
                continue;
            }
            int extra = sameItem ? now.getCount() - was.getCount() : now.getCount();
            slot.set(was.copy());
            if (extra > 0) {
                returnItems(player, menu, index, now.copyWithCount(extra));
            }
        }
    }

    /** Puts items back where a quick move took them from, or into the inventory if that slot is taken. */
    private static void returnItems(ServerPlayer player, AbstractContainerMenu menu, int sourceIndex, ItemStack stack) {
        Slot source = menu.getSlot(sourceIndex);
        if (source.getItem().isEmpty() && source.mayPlace(stack)) {
            source.set(stack);
        } else {
            player.getInventory().placeItemBackInInventory(stack);
        }
    }

    /**
     * Empties the listed menu slots. The client has already asked the player to confirm. Result slots and the creative
     * picker are never touched.
     */
    static void deleteSlots(ServerPlayer player, int containerId, String menuSlotIds) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu.containerId != containerId || !isSupported(menu)) {
            return;
        }
        for (int index : parseLocked(menuSlotIds)) {
            if (index < 0 || index >= menu.slots.size()) {
                continue;
            }
            Slot slot = menu.getSlot(index);
            if (!(slot instanceof ResultSlot) && slot.hasItem()) {
                slot.set(ItemStack.EMPTY);
            }
        }
        menu.broadcastChanges();
    }

    /**
     * The id a slot is locked under. Player slots use their index in the player's inventory, which is the same on
     * every screen. Container slots use their index in the menu.
     */
    public static int lockId(Slot slot, Inventory inventory) {
        return slot.container == inventory ? slot.getContainerSlot() : slot.index;
    }

    /** Parses the comma-separated locked ids sent with a sort request. */
    static java.util.Set<Integer> parseLocked(String ids) {
        java.util.Set<Integer> out = new java.util.HashSet<>();
        if (ids == null || ids.isEmpty()) {
            return out;
        }
        for (String part : ids.split(",")) {
            try {
                out.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException e) {
                // Ignore anything that is not an id.
            }
        }
        return out;
    }

    /** The slots that belong to a group, in the menu's own order. */
    static List<Slot> groupSlots(AbstractContainerMenu menu, ServerPlayer player, String group) {
        Inventory inventory = player.getInventory();
        Container main = mainContainer(menu, inventory);
        List<Slot> slots = new ArrayList<>();
        for (Slot slot : menu.slots) {
            boolean playerSlot = slot.container == inventory;
            if (GROUP_PLAYER.equals(group)) {
                if (playerSlot && slot.getContainerSlot() >= MAIN_START && slot.getContainerSlot() <= MAIN_END) {
                    slots.add(slot);
                }
            } else if (GROUP_CONTAINER.equals(group)) {
                // Crafting grids (the 2x2 personal crafter and the 3x3 crafting table) are never sorted.
                if (!playerSlot && slot.container == main && !(slot instanceof ResultSlot)
                        && !(slot.container instanceof CraftingContainer)) {
                    slots.add(slot);
                }
            }
        }
        return slots;
    }

    /**
     * Merges stackable items, orders the stacks by the key, and returns them in fill order (one entry per slot, empty
     * stacks padding the end). The slot list passed in gives each slot's per-item stack limit.
     */
    static List<ItemStack> arrange(List<ItemStack> contents, List<Slot> slots, SortKey key) {
        List<ItemStack> merged = merge(contents, slots);

        Map<String, Integer> totals = new HashMap<>();
        for (ItemStack stack : merged) {
            totals.merge(identity(stack), stack.getCount(), Integer::sum);
        }

        Comparator<ItemStack> byKey = switch (key) {
            case QUANTITY -> Comparator.comparingInt((ItemStack s) -> -totals.get(identity(s)))
                    .thenComparing(s -> name(s));
            case NAME -> Comparator.comparing((ItemStack s) -> name(s));
            case MOD -> Comparator.comparing((ItemStack s) -> namespace(s))
                    .thenComparing(s -> name(s));
        };
        Comparator<ItemStack> full = byKey
                .thenComparing(s -> identity(s))
                .thenComparingInt(s -> -s.getCount());
        merged.sort(full);

        List<ItemStack> out = new ArrayList<>(merged);
        while (out.size() < slots.size()) {
            out.add(ItemStack.EMPTY);
        }
        return out;
    }

    /** Stacks of the same item and components become full stacks, respecting each slot's own limit. */
    private static List<ItemStack> merge(List<ItemStack> contents, List<Slot> slots) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack source : contents) {
            if (source.isEmpty()) {
                continue;
            }
            ItemStack stack = source.copy();
            int cap = limit(stack, slots);
            for (ItemStack existing : out) {
                if (stack.isEmpty()) {
                    break;
                }
                if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < cap) {
                    int move = Math.min(cap - existing.getCount(), stack.getCount());
                    existing.grow(move);
                    stack.shrink(move);
                }
            }
            while (!stack.isEmpty()) {
                out.add(stack.split(cap));
            }
        }
        return out;
    }

    private static int limit(ItemStack stack, List<Slot> slots) {
        int cap = stack.getMaxStackSize();
        for (Slot slot : slots) {
            cap = Math.min(cap, slot.getMaxStackSize(stack));
        }
        return Math.max(1, cap);
    }

    private static String identity(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id + "#" + stack.getComponents().hashCode();
    }

    private static String name(ItemStack stack) {
        return stack.getHoverName().getString().toLowerCase(java.util.Locale.ROOT);
    }

    private static String namespace(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
    }
}
