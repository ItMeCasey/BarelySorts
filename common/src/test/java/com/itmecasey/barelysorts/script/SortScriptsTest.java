package com.itmecasey.barelysorts.script;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The registry behind the script API: pure Java, so it runs without Minecraft or a script engine. */
class SortScriptsTest {
    final UUID alice = UUID.randomUUID();

    @AfterEach
    void reset() {
        SortScripts.clearAll();
    }

    static ItemView item(String id, int count) {
        return new ItemView(id, id.substring(id.indexOf(':') + 1), count);
    }

    @Test
    void globsMatchExactIdsNamespacesAndSuffixes() {
        assertTrue(Globs.matches("minecraft:diamond", "minecraft:diamond"));
        assertTrue(Globs.matches("diamond", "minecraft:diamond"));
        assertTrue(Globs.matches("mymod:*", "mymod:anything"));
        assertTrue(Globs.matches("*_ingot", "minecraft:iron_ingot"));
        assertFalse(Globs.matches("*_ingot", "minecraft:iron_nugget"));
        assertFalse(Globs.matches("mymod:*", "other:thing"));
        assertFalse(Globs.matches("a.c", "minecraft:abc"), "dots are literal");
    }

    @Test
    void sortHandlerCanCancelAndChangeTheKey() {
        SortScripts.onSort("server", e -> e.key = "NAME");
        SortEvent changed = SortScripts.fireSort("alice", alice, "player", "ROWS", "QUANTITY", new int[0]);
        assertEquals("NAME", changed.key);
        assertFalse(changed.isCancelled());

        SortScripts.onSort("server", SortEvent::cancel);
        assertTrue(SortScripts.fireSort("alice", alice, "player", "ROWS", "QUANTITY", new int[0]).isCancelled());
    }

    @Test
    void aThrowingHandlerIsSkippedAndTheRestStillRun() {
        List<String> seen = new ArrayList<>();
        SortScripts.onDelete("server", e -> {
            throw new IllegalStateException("boom");
        });
        SortScripts.onDelete("server", e -> seen.add("second"));
        assertTrue(SortScripts.fireDelete("alice", alice, List.of(item("minecraft:dirt", 3))));
        assertEquals(List.of("second"), seen);
    }

    @Test
    void cancellingDeleteQuickMoveAndAutofillIsReported() {
        SortScripts.onDelete("server", DeleteEvent::cancel);
        SortScripts.onQuickMove("server", QuickMoveEvent::cancel);
        SortScripts.onAutofill("server", AutofillEvent::cancel);
        assertFalse(SortScripts.fireDelete("a", alice, List.of()));
        assertFalse(SortScripts.fireQuickMove("a", alice, "stack", 3, item("minecraft:dirt", 1)));
        assertFalse(SortScripts.fireAutofill("a", alice, 0, 12, item("minecraft:dirt", 1), "refill"));
    }

    @Test
    void reloadingAnOwnerReplacesItsHandlersOnly() {
        List<String> seen = new ArrayList<>();
        SortScripts.onSorted("startup", e -> seen.add("startup"));
        SortScripts.onSorted("server", e -> seen.add("server-old"));
        SortScripts.clearOwner("server");
        SortScripts.onSorted("server", e -> seen.add("server-new"));
        SortScripts.fireSorted("a", alice, "player", "ROWS", "NAME", 5);
        assertEquals(java.util.Set.of("startup", "server-new"), new java.util.HashSet<>(seen));
        assertEquals(2, seen.size());
    }

    @Test
    void remembersWhatAPlayerAskedFor() {
        assertNull(SortScripts.mode(alice, "player"));
        SortScripts.remember("Alice", alice, "player", "COLUMNS", "MOD", new int[] {9, 10});
        assertEquals(alice, SortScripts.find("alice"));
        assertEquals(alice, SortScripts.find(alice.toString()));
        assertEquals("COLUMNS", SortScripts.mode(alice, "player").method());
        assertArrayEquals(new int[] {9, 10}, SortScripts.mode(alice, "player").lockedSlots());
        SortScripts.forget(alice);
        assertNull(SortScripts.mode(alice, "player"));
        assertNull(SortScripts.find("nobody"));
    }

    @Test
    void noScriptsMeansNoScriptedOrder() {
        assertNull(SortScripts.scriptedOrder());
        assertFalse(SortScripts.replacesKey());
        assertFalse(SortScripts.anyHandlers());
    }

    @Test
    void categoriesSortFirstInTheGivenOrder() {
        SortScripts.category("server", "ores", List.of("*_ingot", "minecraft:diamond"));
        SortScripts.category("server", "blocks", List.of("minecraft:stone", "minecraft:dirt"));
        SortScripts.categoryOrder("server", List.of("blocks", "ores"));
        List<ItemView> items = new ArrayList<>(List.of(item("minecraft:apple", 1), item("minecraft:iron_ingot", 1),
                item("minecraft:dirt", 1), item("minecraft:diamond", 1)));
        items.sort(SortScripts.scriptedOrder());
        assertEquals(List.of("minecraft:dirt", "minecraft:iron_ingot", "minecraft:diamond", "minecraft:apple"),
                items.stream().map(i -> i.id).toList());
        assertFalse(SortScripts.replacesKey(), "categories leave the player's key in charge inside a category");
    }

    @Test
    void aNamedOrderReplacesTheKey() {
        SortScripts.registerOrder("server", "shortest", (a, b) -> Integer.compare(a.name.length(), b.name.length()));
        assertFalse(SortScripts.replacesKey(), "registered but not used yet");
        SortScripts.useOrder("server", "shortest");
        assertTrue(SortScripts.replacesKey());
        List<ItemView> items = new ArrayList<>(List.of(item("minecraft:diamond", 1), item("minecraft:ice", 1)));
        items.sort(SortScripts.scriptedOrder());
        assertEquals("minecraft:ice", items.get(0).id);
        SortScripts.useOrder("server", null);
        assertFalse(SortScripts.replacesKey());
    }

    @Test
    void anUnknownOrderNameChangesNothing() {
        SortScripts.useOrder("server", "missing");
        assertFalse(SortScripts.replacesKey());
        assertNull(SortScripts.scriptedOrder());
    }
}
