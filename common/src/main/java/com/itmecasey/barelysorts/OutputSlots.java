package com.itmecasey.barelysorts;

/**
 * Which slots are output-only (crafting results, furnace and merchant outputs, modded crafters). The decision is kept
 * free of Minecraft types so it can be unit tested; {@link Sorter#isOutputSlot} feeds it from a real slot.
 */
public final class OutputSlots {
    private OutputSlots() {
    }

    /**
     * A slot is output-only when it is a vanilla result slot, or when it holds an item that the slot itself would
     * refuse to accept: items can be taken out but never put in, so only the menu's own click handling can move them.
     */
    public static boolean isOutput(boolean resultSlotClass, boolean hasItem, boolean acceptsItsOwnItem) {
        return resultSlotClass || (hasItem && !acceptsItsOwnItem);
    }
}
