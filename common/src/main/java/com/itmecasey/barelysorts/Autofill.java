package com.itmecasey.barelysorts;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Hotbar autofill. When a hotbar slot's item runs out, the same item moves in from the main inventory.
 *
 * Players who turn on "swap at 1 durability" also get a tool swapped out as soon as it has one use left. The damaged
 * tool goes to the main-inventory slot the replacement came from. Without that setting, a tool is replaced only once
 * it breaks.
 *
 * Moving the last item out of the hotbar by hand empties the slot the same way, so it also refills; that cannot be
 * told apart without hooking the menu.
 *
 * Each loader calls {@link #tick(ServerPlayer)} once per server player tick and {@link #forget(UUID)} on logout.
 */
public final class Autofill {
    private static final int HOTBAR_SIZE = 9;
    private static final int MAIN_START = 9;
    private static final int MAIN_END = 35;

    private static final Map<UUID, ItemStack[]> SNAPSHOTS = new HashMap<>();
    private static final Set<UUID> REPLACE_AT_ONE = new HashSet<>();

    private Autofill() {
    }

    /** Turns the swap-at-1-durability setting on or off for one player. Sent by the player's client. */
    public static void setReplaceAtOne(UUID player, boolean enabled) {
        if (enabled) {
            REPLACE_AT_ONE.add(player);
        } else {
            REPLACE_AT_ONE.remove(player);
        }
    }

    public static void tick(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        ItemStack[] previous = SNAPSHOTS.computeIfAbsent(player.getUUID(), id -> copyHotbar(inventory));
        boolean atOne = REPLACE_AT_ONE.contains(player.getUUID());

        for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
            ItemStack before = previous[slot];
            ItemStack now = inventory.getItem(slot);
            if (!before.isEmpty() && now.isEmpty()) {
                refill(inventory, slot, before);
            } else if (atOne && isOnLastUse(now)) {
                swapDamaged(inventory, slot, now);
            }
        }
        SNAPSHOTS.put(player.getUUID(), copyHotbar(inventory));
    }

    public static void forget(UUID player) {
        SNAPSHOTS.remove(player);
        REPLACE_AT_ONE.remove(player);
    }

    private static boolean isOnLastUse(ItemStack stack) {
        return stack.isDamageableItem() && stack.getMaxDamage() - stack.getDamageValue() <= 1;
    }

    private static void refill(Inventory inventory, int hotbarSlot, ItemStack used) {
        for (int slot = MAIN_START; slot <= MAIN_END; slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (!candidate.isEmpty() && ItemStack.isSameItemSameComponents(candidate, used)) {
                inventory.setItem(hotbarSlot, candidate);
                inventory.setItem(slot, ItemStack.EMPTY);
                return;
            }
        }
    }

    /** Swaps a tool on its last use for another of the same item from the main inventory, if there is one. */
    private static void swapDamaged(Inventory inventory, int hotbarSlot, ItemStack damaged) {
        for (int slot = MAIN_START; slot <= MAIN_END; slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (!candidate.isEmpty() && candidate.is(damaged.getItem())) {
                inventory.setItem(slot, damaged);
                inventory.setItem(hotbarSlot, candidate);
                return;
            }
        }
    }

    private static ItemStack[] copyHotbar(Inventory inventory) {
        ItemStack[] copy = new ItemStack[HOTBAR_SIZE];
        for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
            copy[slot] = inventory.getItem(slot).copy();
        }
        return copy;
    }
}
