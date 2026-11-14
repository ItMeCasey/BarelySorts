package com.itmecasey.barelysorts.script;

import java.util.UUID;

/**
 * Fired on the server before a group is sorted. A script can cancel it, or change {@link #method} and {@link #key}
 * for this one sort ({@code ROWS} or {@code COLUMNS}; {@code QUANTITY}, {@code NAME} or {@code MOD}).
 */
public final class SortEvent {
    public final String player;
    public final String uuid;
    /** {@code player} or {@code container}. */
    public final String group;
    public String method;
    public String key;
    /** Locked slot ids the player's client reported with the request. */
    public final int[] lockedSlots;
    private boolean cancelled;

    SortEvent(String player, UUID uuid, String group, String method, String key, int[] lockedSlots) {
        this.player = player;
        this.uuid = uuid.toString();
        this.group = group;
        this.method = method;
        this.key = key;
        this.lockedSlots = lockedSlots;
    }

    public void cancel() {
        cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
