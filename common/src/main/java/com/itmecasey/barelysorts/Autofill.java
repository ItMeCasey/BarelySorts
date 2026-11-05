package com.itmecasey.barelysorts;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
 * Locked slots are never used: a locked hotbar slot is not refilled or swapped, and a locked inventory slot is not
 * taken from. Locked ids use {@link Sorter#lockId}, which for player slots is the slot index 0 to 35.
 *
 * The settings are saved in the world folder, so they survive a server restart. Each player's settings are read from
 * that file the first time the server sees them.
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
    private static final String FILE_NAME = "barelysorts-autofill.json";
    private static final Type FILE_TYPE = new TypeToken<Map<String, Saved>>() {
    }.getType();

    /** One player's saved autofill settings. */
    private record Saved(boolean replaceAtOne, List<Integer> locked) {
    }

    private static final Map<UUID, ItemStack[]> SNAPSHOTS = new HashMap<>();
    private static final Map<UUID, Saved> SETTINGS = new HashMap<>();
    /** Hotbar slots the player moved or removed by hand since the last tick. Those are never refilled. */
    private static final Map<UUID, Set<Integer>> MANUAL = new HashMap<>();

    /** Records hotbar slots the player just moved or removed by hand, so autofill leaves them empty. */
    public static void markManual(UUID player, Set<Integer> hotbarSlots) {
        MANUAL.computeIfAbsent(player, id -> new HashSet<>()).addAll(hotbarSlots);
    }

    private Autofill() {
    }

    /** The player's autofill settings, sent by their client on change and on join. Saved to the world folder. */
    public static void setSettings(ServerPlayer player, boolean replaceAtOne, Set<Integer> lockedSlots) {
        Saved saved = new Saved(replaceAtOne, new ArrayList<>(lockedSlots));
        SETTINGS.put(player.getUUID(), saved);
        Map<String, Saved> all = readFile(player);
        all.put(player.getUUID().toString(), saved);
        writeFile(player, all);
    }

    public static void tick(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        ItemStack[] previous = SNAPSHOTS.computeIfAbsent(player.getUUID(), id -> copyHotbar(inventory));
        Saved settings = settingsFor(player);
        boolean atOne = settings.replaceAtOne();
        Set<Integer> locked = Set.copyOf(settings.locked());
        Set<Integer> manual = MANUAL.getOrDefault(player.getUUID(), Set.of());

        for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
            if (locked.contains(slot)) {
                continue;
            }
            ItemStack before = previous[slot];
            ItemStack now = inventory.getItem(slot);
            if (!before.isEmpty() && now.isEmpty()) {
                // Emptied by hand (a shift-click, a drag or a delete), not by use: leave it empty.
                if (!manual.contains(slot)) {
                    refill(inventory, slot, before, locked);
                }
            } else if (atOne && isOnLastUse(now)) {
                swapDamaged(inventory, slot, now, locked);
            }
        }
        MANUAL.remove(player.getUUID());
        SNAPSHOTS.put(player.getUUID(), copyHotbar(inventory));
    }

    public static void forget(UUID player) {
        SNAPSHOTS.remove(player);
        SETTINGS.remove(player);
        MANUAL.remove(player);
    }

    /** The player's settings in memory, or read from the world file the first time. */
    private static Saved settingsFor(ServerPlayer player) {
        return SETTINGS.computeIfAbsent(player.getUUID(), id -> {
            Saved saved = readFile(player).get(id.toString());
            return saved != null ? saved : new Saved(false, List.of());
        });
    }

    private static Path file(ServerPlayer player) {
        return player.getServer().getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
    }

    private static Map<String, Saved> readFile(ServerPlayer player) {
        Path file = file(player);
        if (!Files.exists(file)) {
            return new HashMap<>();
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            Map<String, Saved> loaded = new Gson().fromJson(reader, FILE_TYPE);
            return loaded == null ? new HashMap<>() : new HashMap<>(loaded);
        } catch (IOException | RuntimeException e) {
            return new HashMap<>();
        }
    }

    private static void writeFile(ServerPlayer player, Map<String, Saved> all) {
        try (Writer writer = Files.newBufferedWriter(file(player))) {
            new Gson().toJson(all, FILE_TYPE, writer);
        } catch (IOException e) {
            // The settings still work for this session; they just will not survive a restart.
        }
    }

    private static boolean isOnLastUse(ItemStack stack) {
        return stack.isDamageableItem() && stack.getMaxDamage() - stack.getDamageValue() <= 1;
    }

    private static void refill(Inventory inventory, int hotbarSlot, ItemStack used, Set<Integer> locked) {
        for (int slot = MAIN_START; slot <= MAIN_END; slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (!locked.contains(slot) && !candidate.isEmpty() && ItemStack.isSameItemSameComponents(candidate, used)) {
                inventory.setItem(hotbarSlot, candidate);
                inventory.setItem(slot, ItemStack.EMPTY);
                return;
            }
        }
    }

    /** Swaps a tool on its last use for another of the same item from the main inventory, if there is one. */
    private static void swapDamaged(Inventory inventory, int hotbarSlot, ItemStack damaged, Set<Integer> locked) {
        for (int slot = MAIN_START; slot <= MAIN_END; slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (!locked.contains(slot) && !candidate.isEmpty() && candidate.is(damaged.getItem())) {
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
