package com.itmecasey.barelysorts;


import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
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

    /** Player main inventory is slots 9 to 35 of the player's Inventory. */
    private static final int MAIN_START = 9;
    private static final int MAIN_END = 35;

    private Sorter() {
    }

    public static void sortGroup(ServerPlayer player, int containerId, String group, String methodName, String keyName) {
        AbstractContainerMenu menu = player.containerMenu;
        // The creative item picker is a client-only menu; its slots hold infinite stacks and are never sorted.
        if (menu.containerId != containerId || menu.getClass().getName().endsWith("ItemPickerMenu")) {
            return;
        }
        List<Slot> slots = groupSlots(menu, player, group);
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

    /** The slots that belong to a group, in the menu's own order. */
    static List<Slot> groupSlots(AbstractContainerMenu menu, ServerPlayer player, String group) {
        Inventory inventory = player.getInventory();
        List<Slot> slots = new ArrayList<>();
        for (Slot slot : menu.slots) {
            boolean playerSlot = slot.container == inventory;
            if (GROUP_PLAYER.equals(group)) {
                if (playerSlot && slot.getContainerSlot() >= MAIN_START && slot.getContainerSlot() <= MAIN_END) {
                    slots.add(slot);
                }
            } else if (GROUP_CONTAINER.equals(group)) {
                if (!playerSlot && !(slot instanceof ResultSlot)) {
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
