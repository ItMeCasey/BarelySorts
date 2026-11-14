package com.itmecasey.barelysorts.script;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.ToIntBiFunction;

/**
 * The {@code BarelySorts} global that ModJS scripts see. Everything runs on the server, so it is added to startup and
 * server scripts only. Players are named by name or UUID text; unknown players give {@code null}.
 *
 * <p>Events: {@code onSort} (before, cancellable, can change {@code method} and {@code key}), {@code onSorted},
 * {@code onQuickMove}, {@code onDelete} and {@code onAutofill} (before, cancellable). Reads: {@code sortMode},
 * {@code lockedSlots}, {@code toolSwap}. Content: {@code registerSortOrder}, {@code useSortOrder}, {@code category},
 * {@code categoryOrder}.
 */
public final class ScriptApi {
    private final String owner;

    /** Created once per script scope; {@code owner} is the script type, so a reload replaces only its own registrations. */
    public ScriptApi(String owner) {
        this.owner = owner;
    }

    // ---- events ----

    public void onSort(Consumer<SortEvent> handler) {
        SortScripts.onSort(owner, handler);
    }

    public void onSorted(Consumer<SortedEvent> handler) {
        SortScripts.onSorted(owner, handler);
    }

    public void onQuickMove(Consumer<QuickMoveEvent> handler) {
        SortScripts.onQuickMove(owner, handler);
    }

    public void onDelete(Consumer<DeleteEvent> handler) {
        SortScripts.onDelete(owner, handler);
    }

    public void onAutofill(Consumer<AutofillEvent> handler) {
        SortScripts.onAutofill(owner, handler);
    }

    // ---- reading a player's state ----

    /**
     * The sort mode a player's client last asked for in this group ({@code player} or {@code container}), as
     * {@code {method, key}}, or {@code null} if that player has not sorted it since joining.
     */
    public Object sortMode(String player, String group) {
        UUID id = SortScripts.find(player);
        SortScripts.Mode mode = id == null ? null : SortScripts.mode(id, group);
        return mode == null ? null : new ModeView(mode.method(), mode.key());
    }

    /**
     * Slot ids the player had locked when they last sorted this group, or an empty list. Player slots use their
     * inventory index 0 to 35; container slots use their menu index.
     */
    public int[] lockedSlots(String player, String group) {
        UUID id = SortScripts.find(player);
        SortScripts.Mode mode = id == null ? null : SortScripts.mode(id, group);
        return mode == null ? new int[0] : mode.lockedSlots().clone();
    }

    /** Whether the player has "swap tools at 1 durability" on. Needs the player to be online. */
    public boolean toolSwap(String player) {
        UUID id = SortScripts.find(player);
        return id != null && com.itmecasey.barelysorts.Autofill.toolSwap(id);
    }

    // ---- content ----

    /**
     * Adds a named sort order: {@code (a, b) => number}, negative when {@code a} comes first. Items have
     * {@code id}, {@code namespace}, {@code name} and {@code count}. Take effect with {@link #useSortOrder}.
     */
    public void registerSortOrder(String name, ToIntBiFunction<ItemView, ItemView> comparator) {
        SortScripts.registerOrder(owner, name, comparator);
    }

    /** Uses the named order instead of the player's sort key for every sort; pass {@code null} to stop. */
    public void useSortOrder(String name) {
        SortScripts.useOrder(owner, name);
    }

    /**
     * Defines a category by item id patterns: an exact id or a glob ({@code "minecraft:*_ingot"}, {@code "mymod:*"}).
     * Stacks sort by category first, then by the player's key (or the named order).
     */
    public void category(String name, List<?> patterns) {
        SortScripts.category(owner, name, strings(patterns));
    }

    /** Orders the categories; unlisted ones follow in the order they were defined. */
    public void categoryOrder(List<?> names) {
        SortScripts.categoryOrder(owner, names == null ? null : strings(names));
    }

    private static List<String> strings(List<?> values) {
        List<String> out = new ArrayList<>();
        for (Object value : values) {
            out.add(String.valueOf(value));
        }
        return out;
    }

    /** Result of {@link #sortMode}. */
    public static final class ModeView {
        public final String method;
        public final String key;

        ModeView(String method, String key) {
            this.method = method;
            this.key = key;
        }
    }
}
