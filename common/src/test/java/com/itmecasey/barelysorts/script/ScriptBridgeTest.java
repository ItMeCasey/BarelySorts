package com.itmecasey.barelysorts.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.modjs.script.ExtraBindings;
import com.itmecasey.modjs.script.ScriptType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

/** Real JavaScript (Rhino, the engine ModJS uses) against the API through the reflective ModJS bridge. */
class ScriptBridgeTest {
    final UUID alice = UUID.randomUUID();

    @AfterEach
    void tearDown() {
        SortScripts.clearAll();
        ExtraBindings.PROVIDERS.clear();
        ModJsBridge.reset();
    }

    Object run(ScriptType type, String js) {
        assertTrue(ModJsBridge.install());
        Context cx = Context.enter();
        try {
            ScriptableObject scope = cx.initStandardObjects();
            ExtraBindings.PROVIDERS.get(0).install(type, cx, scope);
            return cx.evaluateString(scope, js, "test.js", 1, null);
        } finally {
            Context.exit();
        }
    }

    @Test
    void serverScriptsGetTheGlobalAndClientScriptsDoNot() {
        assertEquals("function", run(ScriptType.SERVER, "typeof BarelySorts.onSort").toString());
        assertEquals("function", run(ScriptType.STARTUP, "typeof BarelySorts.category").toString());
        assertEquals("undefined", run(ScriptType.CLIENT, "typeof BarelySorts").toString());
    }

    @Test
    void aScriptHandlerCancelsAndChangesASort() {
        run(ScriptType.SERVER, "BarelySorts.onSort(function (e) { if (e.group == 'container') e.cancel(); else e.key = 'MOD'; })");
        SortEvent container = SortScripts.fireSort("alice", alice, "container", "ROWS", "QUANTITY", new int[0]);
        assertTrue(container.isCancelled());
        SortEvent player = SortScripts.fireSort("alice", alice, "player", "ROWS", "QUANTITY", new int[0]);
        assertFalse(player.isCancelled());
        assertEquals("MOD", player.key);
    }

    @Test
    void aScriptThatThrowsDoesNotBreakTheSort() {
        run(ScriptType.SERVER, "BarelySorts.onSort(function (e) { throw new Error('bad script'); })");
        SortEvent event = SortScripts.fireSort("alice", alice, "player", "ROWS", "NAME", new int[0]);
        assertFalse(event.isCancelled());
        assertEquals("NAME", event.key);
    }

    @Test
    void categoriesAndOrdersFromJavaScript() {
        run(ScriptType.SERVER, "BarelySorts.category('gems', ['minecraft:diamond', 'minecraft:emerald']);"
                + "BarelySorts.categoryOrder(['gems']);"
                + "BarelySorts.registerSortOrder('byCount', function (a, b) { return b.count - a.count; });"
                + "BarelySorts.useSortOrder('byCount');");
        var order = SortScripts.scriptedOrder();
        ItemView apple = new ItemView("minecraft:apple", "Apple", 64);
        ItemView diamond = new ItemView("minecraft:diamond", "Diamond", 1);
        ItemView bread = new ItemView("minecraft:bread", "Bread", 8);
        List<ItemView> items = new java.util.ArrayList<>(List.of(apple, bread, diamond));
        items.sort(order);
        assertEquals(List.of(diamond, apple, bread), items, "gems first, then by count");
    }

    @Test
    void readingAPlayersModeAndLocks() {
        SortScripts.remember("Alice", alice, "player", "COLUMNS", "NAME", new int[] {12, 13});
        Object mode = run(ScriptType.SERVER, "var m = BarelySorts.sortMode('Alice', 'player'); m.method + ':' + m.key");
        assertEquals("COLUMNS:NAME", mode.toString());
        assertEquals("2", run(ScriptType.SERVER, "String(BarelySorts.lockedSlots('Alice', 'player').length)").toString());
        assertNull(SortScripts.mode(UUID.randomUUID(), "player"));
        assertTrue(run(ScriptType.SERVER, "BarelySorts.sortMode('nobody', 'player') === null") == Boolean.TRUE);
    }

    @Test
    void loadingServerScriptsAgainReplacesTheirHandlers() {
        run(ScriptType.SERVER, "BarelySorts.onSorted(function (e) { java.lang.System.setProperty('bs.test', 'old'); })");
        tearDownProvidersOnly();
        run(ScriptType.SERVER, "BarelySorts.onSorted(function (e) { java.lang.System.setProperty('bs.test', 'new'); })");
        SortScripts.fireSorted("a", alice, "player", "ROWS", "NAME", 3);
        assertEquals("new", System.getProperty("bs.test"));
    }

    private void tearDownProvidersOnly() {
        ExtraBindings.PROVIDERS.clear();
        ModJsBridge.reset();
    }
}
