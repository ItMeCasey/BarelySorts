package com.itmecasey.barelysorts;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * When the last item in a hotbar slot is used up, the same item is moved in from the main inventory.
 *
 * Each tick the hotbar is compared with the previous tick. A slot that held a stack and is now empty is refilled
 * from the first matching stack in the main inventory. Moving the last item out of the hotbar by hand empties the
 * slot the same way, so it also refills; that cannot be told apart without hooking the menu.
 */
@EventBusSubscriber(modid = BarelySorts.MODID)
public final class Autofill {
    private static final int HOTBAR_SIZE = 9;
    private static final int MAIN_START = 9;
    private static final int MAIN_END = 35;

    private static final Map<UUID, ItemStack[]> SNAPSHOTS = new HashMap<>();

    private Autofill() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Inventory inventory = player.getInventory();
        ItemStack[] previous = SNAPSHOTS.computeIfAbsent(player.getUUID(), id -> copyHotbar(inventory));

        for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
            ItemStack before = previous[slot];
            if (!before.isEmpty() && inventory.getItem(slot).isEmpty()) {
                refill(inventory, slot, before);
            }
        }
        SNAPSHOTS.put(player.getUUID(), copyHotbar(inventory));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SNAPSHOTS.remove(event.getEntity().getUUID());
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

    private static ItemStack[] copyHotbar(Inventory inventory) {
        ItemStack[] copy = new ItemStack[HOTBAR_SIZE];
        for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
            copy[slot] = inventory.getItem(slot).copy();
        }
        return copy;
    }
}
