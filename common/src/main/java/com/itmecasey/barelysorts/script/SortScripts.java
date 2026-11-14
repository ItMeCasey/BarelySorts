package com.itmecasey.barelysorts.script;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.ToIntBiFunction;
import java.util.logging.Logger;

/**
 * Everything scripts register and everything the mod asks scripts. No Minecraft types in here, so it works the same
 * with or without a script engine and is unit tested directly. A script that throws is logged in one line and skipped.
 *
 * <p>Registrations belong to an owner (the script type that made them, such as {@code server}). Loading that owner's
 * scripts again clears its registrations first, so reloading scripts never stacks handlers.
 */
public final class SortScripts {
    private static final Logger LOG = Logger.getLogger("barelysorts");

    /** One owner's registrations. */
    private static final class Registrations {
        final List<Consumer<SortEvent>> sort = new CopyOnWriteArrayList<>();
        final List<Consumer<SortedEvent>> sorted = new CopyOnWriteArrayList<>();
        final List<Consumer<QuickMoveEvent>> quickMove = new CopyOnWriteArrayList<>();
        final List<Consumer<DeleteEvent>> delete = new CopyOnWriteArrayList<>();
        final List<Consumer<AutofillEvent>> autofill = new CopyOnWriteArrayList<>();
        final Map<String, ToIntBiFunction<ItemView, ItemView>> orders = new ConcurrentHashMap<>();
        final Map<String, List<String>> categories = new LinkedHashMap<>();
        volatile String useOrder;
        volatile List<String> categoryOrder;
    }

    private static final Map<String, Registrations> OWNERS = new ConcurrentSkipListMap<>();

    /** What a player's client last reported for one group. */
    public record Mode(String method, String key, int[] lockedSlots) {
    }

    private static final Map<UUID, Map<String, Mode>> MODES = new ConcurrentHashMap<>();
    private static final Map<String, UUID> NAMES = new ConcurrentHashMap<>();

    private SortScripts() {
    }

    private static Registrations of(String owner) {
        return OWNERS.computeIfAbsent(owner, o -> new Registrations());
    }

    /** Drops everything the owner registered; loading that owner's scripts again calls this first. */
    public static void clearOwner(String owner) {
        OWNERS.remove(owner);
    }

    /** Full reset, for tests. */
    public static void clearAll() {
        OWNERS.clear();
        MODES.clear();
        NAMES.clear();
    }

    // ---- registering ----

    public static void onSort(String owner, Consumer<SortEvent> handler) {
        of(owner).sort.add(handler);
    }

    public static void onSorted(String owner, Consumer<SortedEvent> handler) {
        of(owner).sorted.add(handler);
    }

    public static void onQuickMove(String owner, Consumer<QuickMoveEvent> handler) {
        of(owner).quickMove.add(handler);
    }

    public static void onDelete(String owner, Consumer<DeleteEvent> handler) {
        of(owner).delete.add(handler);
    }

    public static void onAutofill(String owner, Consumer<AutofillEvent> handler) {
        of(owner).autofill.add(handler);
    }

    public static void registerOrder(String owner, String name, ToIntBiFunction<ItemView, ItemView> comparator) {
        of(owner).orders.put(name, comparator);
    }

    /** Makes the named order replace the player's sort key for every sort; null goes back to the player's key. */
    public static void useOrder(String owner, String name) {
        of(owner).useOrder = name;
    }

    public static void category(String owner, String name, List<String> patterns) {
        Registrations r = of(owner);
        synchronized (r.categories) {
            r.categories.put(name, List.copyOf(patterns));
        }
    }

    /** Sets the order categories sort in; categories not listed come after, in the order they were registered. */
    public static void categoryOrder(String owner, List<String> names) {
        of(owner).categoryOrder = names == null ? null : List.copyOf(names);
    }

    // ---- firing ----

    private static <T> void run(List<Consumer<T>> handlers, T event, String what) {
        for (Consumer<T> handler : handlers) {
            try {
                handler.accept(event);
            } catch (Throwable t) {
                LOG.warning("Barely Sorts: a script " + what + " handler failed: " + t);
            }
        }
    }

    /** Runs the sort handlers; the returned event says whether to go on and carries any method or key change. */
    public static SortEvent fireSort(String name, UUID uuid, String group, String method, String key, int[] locked) {
        SortEvent event = new SortEvent(name, uuid, group, method, key, locked);
        for (Registrations r : OWNERS.values()) {
            run(r.sort, event, "sort");
        }
        return event;
    }

    public static void fireSorted(String name, UUID uuid, String group, String method, String key, int slots) {
        SortedEvent event = new SortedEvent(name, uuid, group, method, key, slots);
        for (Registrations r : OWNERS.values()) {
            run(r.sorted, event, "sorted");
        }
    }

    /** Returns true when no script cancelled it. */
    public static boolean fireQuickMove(String name, UUID uuid, String mode, int slot, ItemView item) {
        QuickMoveEvent event = new QuickMoveEvent(name, uuid, mode, slot, item);
        for (Registrations r : OWNERS.values()) {
            run(r.quickMove, event, "quick move");
        }
        return !event.isCancelled();
    }

    /** Returns true when no script cancelled it. */
    public static boolean fireDelete(String name, UUID uuid, List<ItemView> items) {
        DeleteEvent event = new DeleteEvent(name, uuid, items);
        for (Registrations r : OWNERS.values()) {
            run(r.delete, event, "delete");
        }
        return !event.isCancelled();
    }

    /** Returns true when no script cancelled it. */
    public static boolean fireAutofill(String name, UUID uuid, int hotbarSlot, int fromSlot, ItemView item, String reason) {
        AutofillEvent event = new AutofillEvent(name, uuid, hotbarSlot, fromSlot, item, reason);
        for (Registrations r : OWNERS.values()) {
            run(r.autofill, event, "autofill");
        }
        return !event.isCancelled();
    }

    /** True when any script registered an event handler, so the mod can skip building events otherwise. */
    public static boolean anyHandlers() {
        for (Registrations r : OWNERS.values()) {
            if (!r.sort.isEmpty() || !r.sorted.isEmpty() || !r.quickMove.isEmpty() || !r.delete.isEmpty()
                    || !r.autofill.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // ---- reading ----

    /** Remembers what a client asked for, so scripts can read a player's sort mode and locks. */
    public static void remember(String name, UUID uuid, String group, String method, String key, int[] locked) {
        NAMES.put(name.toLowerCase(Locale.ROOT), uuid);
        MODES.computeIfAbsent(uuid, id -> new ConcurrentHashMap<>()).put(group, new Mode(method, key, locked));
    }

    public static void forget(UUID uuid) {
        MODES.remove(uuid);
        NAMES.values().removeIf(uuid::equals);
    }

    /** Finds a player by name or UUID text; null when unknown. */
    public static UUID find(String nameOrUuid) {
        if (nameOrUuid == null) {
            return null;
        }
        UUID byName = NAMES.get(nameOrUuid.toLowerCase(Locale.ROOT));
        if (byName != null) {
            return byName;
        }
        try {
            return UUID.fromString(nameOrUuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static Mode mode(UUID uuid, String group) {
        Map<String, Mode> modes = MODES.get(uuid);
        return modes == null ? null : modes.get(group);
    }

    // ---- sort order for the arrange step ----

    /**
     * The comparator scripts put in front of (categories) or in place of (a named order) the player's sort key, or
     * null when no script changes the order.
     */
    public static Comparator<ItemView> scriptedOrder() {
        Comparator<ItemView> categories = categoryComparator();
        ToIntBiFunction<ItemView, ItemView> order = activeOrder();
        if (categories == null && order == null) {
            return null;
        }
        Comparator<ItemView> named = order == null ? null : (a, b) -> order.applyAsInt(a, b);
        if (categories == null) {
            return named;
        }
        return named == null ? categories : categories.thenComparing(named);
    }

    /** True when a named script order replaces the player's key, so the key comparison is skipped. */
    public static boolean replacesKey() {
        return activeOrder() != null;
    }

    private static ToIntBiFunction<ItemView, ItemView> activeOrder() {
        String name = null;
        for (Registrations r : OWNERS.values()) {
            if (r.useOrder != null) {
                name = r.useOrder;
            }
        }
        if (name == null) {
            return null;
        }
        ToIntBiFunction<ItemView, ItemView> found = null;
        for (Registrations r : OWNERS.values()) {
            ToIntBiFunction<ItemView, ItemView> candidate = r.orders.get(name);
            if (candidate != null) {
                found = candidate;
            }
        }
        return found;
    }

    private static Comparator<ItemView> categoryComparator() {
        Map<String, List<String>> patterns = new LinkedHashMap<>();
        List<String> explicit = null;
        for (Registrations r : OWNERS.values()) {
            synchronized (r.categories) {
                patterns.putAll(r.categories);
            }
            if (r.categoryOrder != null) {
                explicit = r.categoryOrder;
            }
        }
        if (patterns.isEmpty()) {
            return null;
        }
        List<String> names = new ArrayList<>();
        if (explicit != null) {
            for (String n : explicit) {
                if (patterns.containsKey(n) && !names.contains(n)) {
                    names.add(n);
                }
            }
        }
        for (String n : patterns.keySet()) {
            if (!names.contains(n)) {
                names.add(n);
            }
        }
        List<String> order = List.copyOf(names);
        return Comparator.comparingInt(item -> categoryIndex(order, patterns, item.id));
    }

    /** Position of the first category that matches the id; ids that match no category sort last. */
    static int categoryIndex(List<String> order, Map<String, List<String>> patterns, String id) {
        for (int i = 0; i < order.size(); i++) {
            for (String pattern : patterns.get(order.get(i))) {
                if (Globs.matches(pattern, id)) {
                    return i;
                }
            }
        }
        return order.size();
    }
}
