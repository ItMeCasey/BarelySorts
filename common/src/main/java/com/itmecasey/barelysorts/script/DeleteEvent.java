package com.itmecasey.barelysorts.script;

import java.util.List;
import java.util.UUID;

/** Fired on the server before the stacks a player marked and confirmed are deleted. Cancellable. */
public final class DeleteEvent {
    public final String player;
    public final String uuid;
    /** The stacks about to be deleted. */
    public final List<ItemView> items;
    private boolean cancelled;

    DeleteEvent(String player, UUID uuid, List<ItemView> items) {
        this.player = player;
        this.uuid = uuid.toString();
        this.items = items;
    }

    public void cancel() {
        cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
