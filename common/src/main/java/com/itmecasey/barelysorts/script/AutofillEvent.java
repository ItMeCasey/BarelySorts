package com.itmecasey.barelysorts.script;

import java.util.UUID;

/** Fired on the server before hotbar autofill refills a slot or swaps a worn tool. Cancellable. */
public final class AutofillEvent {
    public final String player;
    public final String uuid;
    /** Hotbar slot 0 to 8. */
    public final int hotbarSlot;
    /** Player inventory slot the replacement comes from. */
    public final int fromSlot;
    public final ItemView item;
    /** {@code refill} (the stack ran out) or {@code swap} (a tool on its last use). */
    public final String reason;
    private boolean cancelled;

    AutofillEvent(String player, UUID uuid, int hotbarSlot, int fromSlot, ItemView item, String reason) {
        this.player = player;
        this.uuid = uuid.toString();
        this.hotbarSlot = hotbarSlot;
        this.fromSlot = fromSlot;
        this.item = item;
        this.reason = reason;
    }

    public void cancel() {
        cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
