package com.itmecasey.barelysorts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SortChoicesTest {
    @Test
    void methodsAlternate() {
        assertEquals(SortMethod.COLUMNS, SortMethod.ROWS.next());
        assertEquals(SortMethod.ROWS, SortMethod.COLUMNS.next());
    }

    @Test
    void keysCycleThroughAllThree() {
        assertEquals(SortKey.NAME, SortKey.QUANTITY.next());
        assertEquals(SortKey.MOD, SortKey.NAME.next());
        assertEquals(SortKey.QUANTITY, SortKey.MOD.next());
    }

    @Test
    void parsingKnownNamesRoundTrips() {
        for (SortMethod method : SortMethod.values()) {
            assertEquals(method, SortMethod.parse(method.name()));
        }
        for (SortKey key : SortKey.values()) {
            assertEquals(key, SortKey.parse(key.name()));
        }
    }

    @Test
    void parsingUnknownOrMissingNamesFallsBackToTheDefaults() {
        assertEquals(SortMethod.ROWS, SortMethod.parse("diagonal"));
        assertEquals(SortMethod.ROWS, SortMethod.parse(null));
        assertEquals(SortKey.QUANTITY, SortKey.parse("weight"));
        assertEquals(SortKey.QUANTITY, SortKey.parse(null));
    }
}
