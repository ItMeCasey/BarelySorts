package com.itmecasey.barelysorts;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The lock list travels as comma-separated text, so a bad value from a client must never break the server. */
class LockedIdsTest {
    @Test
    void emptyAndMissingListsGiveNoLocks() {
        assertTrue(LockedIds.parse("").isEmpty());
        assertTrue(LockedIds.parse(null).isEmpty());
    }

    @Test
    void parsesAList() {
        assertEquals(Set.of(1, 5, 12), LockedIds.parse("1,5,12"));
    }

    @Test
    void ignoresSpacesDuplicatesAndGarbage() {
        assertEquals(Set.of(3, 7), LockedIds.parse(" 3 ,7,7,abc,,-,"));
    }

    @Test
    void keepsNegativeNumbersAsTheyAreSoTheCallerCanRejectThem() {
        assertEquals(Set.of(-2, 4), LockedIds.parse("-2,4"));
    }
}
