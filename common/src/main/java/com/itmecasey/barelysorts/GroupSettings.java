package com.itmecasey.barelysorts;

import java.util.ArrayList;
import java.util.List;

/**
 * The per-group choices: how to sort, what to sort by, which slots are locked, and (player group only) whether
 * autofill swaps a tool in at 1 durability instead of waiting for it to break. A locked slot keeps its contents when
 * the group is sorted. Slots are identified by {@link Sorter#lockId}.
 */
public record GroupSettings(SortMethod method, SortKey key, List<Integer> lockedSlots, boolean replaceAtOne) {
    public static final GroupSettings DEFAULT = new GroupSettings(SortMethod.ROWS, SortKey.QUANTITY, List.of(), false);

    public GroupSettings {
        lockedSlots = lockedSlots == null ? List.of() : List.copyOf(lockedSlots);
    }

    public boolean isLocked(int slotId) {
        return lockedSlots.contains(slotId);
    }

    public GroupSettings toggleSlot(int slotId) {
        List<Integer> next = new ArrayList<>(lockedSlots);
        if (!next.remove((Integer) slotId)) {
            next.add(slotId);
        }
        return new GroupSettings(method, key, next, replaceAtOne);
    }

    public GroupSettings withMethod(SortMethod newMethod) {
        return new GroupSettings(newMethod, key, lockedSlots, replaceAtOne);
    }

    public GroupSettings withKey(SortKey newKey) {
        return new GroupSettings(method, newKey, lockedSlots, replaceAtOne);
    }

    public GroupSettings withReplaceAtOne(boolean newValue) {
        return new GroupSettings(method, key, lockedSlots, newValue);
    }
}
