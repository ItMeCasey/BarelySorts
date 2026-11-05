package com.itmecasey.barelysorts;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupSettingsTest {
    @Test
    void defaultsSortByRowsAndQuantityWithNothingLocked() {
        GroupSettings defaults = GroupSettings.DEFAULT;
        assertEquals(SortMethod.ROWS, defaults.method());
        assertEquals(SortKey.QUANTITY, defaults.key());
        assertTrue(defaults.lockedSlots().isEmpty());
        assertFalse(defaults.replaceAtOne());
    }

    @Test
    void togglingASlotLocksThenUnlocksIt() {
        GroupSettings locked = GroupSettings.DEFAULT.toggleSlot(12);
        assertTrue(locked.isLocked(12));
        GroupSettings unlocked = locked.toggleSlot(12);
        assertFalse(unlocked.isLocked(12));
    }

    @Test
    void lockingOneSlotLeavesOthersAlone() {
        GroupSettings settings = GroupSettings.DEFAULT.toggleSlot(3).toggleSlot(9);
        assertTrue(settings.isLocked(3));
        assertTrue(settings.isLocked(9));
        assertFalse(settings.isLocked(4));
        assertEquals(2, settings.lockedSlots().size());
    }

    @Test
    void changingMethodOrKeyKeepsLocksAndTheToolSetting() {
        GroupSettings base = new GroupSettings(SortMethod.ROWS, SortKey.QUANTITY, List.of(5), true);
        GroupSettings changed = base.withMethod(SortMethod.COLUMNS).withKey(SortKey.MOD);
        assertEquals(SortMethod.COLUMNS, changed.method());
        assertEquals(SortKey.MOD, changed.key());
        assertTrue(changed.isLocked(5));
        assertTrue(changed.replaceAtOne());
    }

    @Test
    void aMissingLockListBecomesEmpty() {
        GroupSettings settings = new GroupSettings(SortMethod.ROWS, SortKey.NAME, null, false);
        assertTrue(settings.lockedSlots().isEmpty());
    }
}
