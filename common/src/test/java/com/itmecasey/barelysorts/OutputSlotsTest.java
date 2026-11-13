package com.itmecasey.barelysorts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Shift-click on a crafting result must be left to the game, so these slots must be recognized as outputs. */
class OutputSlotsTest {
    @Test
    void vanillaResultSlotIsAnOutputEvenWhenEmpty() {
        assertTrue(OutputSlots.isOutput(true, false, false));
        assertTrue(OutputSlots.isOutput(true, true, false));
    }

    @Test
    void modSlotThatRefusesItsOwnItemIsAnOutput() {
        assertTrue(OutputSlots.isOutput(false, true, false));
    }

    @Test
    void ordinarySlotsAreNotOutputs() {
        assertFalse(OutputSlots.isOutput(false, true, true));
        assertFalse(OutputSlots.isOutput(false, false, true));
        assertFalse(OutputSlots.isOutput(false, false, false));
    }
}
