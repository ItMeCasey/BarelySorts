package com.itmecasey.barelysorts.script;

import java.util.UUID;

/** Fired on the server before a Ctrl-click or Shift-click quick move that Barely Sorts handles. Cancellable. */
public final class QuickMoveEvent {
    public final String player;
    public final String uuid;
    /** {@code stack}, {@code one} or {@code type}. */
    public final String mode;
    /** Menu slot index the click was on. */
    public final int slot;
    public final ItemView item;
    private boolean cancelled;

    QuickMoveEvent(String player, UUID uuid, String mode, int slot, ItemView item) {
        this.player = player;
        this.uuid = uuid.toString();
        this.mode = mode;
        this.slot = slot;
        this.item = item;
    }

    public void cancel() {
        cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
