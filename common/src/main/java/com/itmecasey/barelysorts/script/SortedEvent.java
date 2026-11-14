package com.itmecasey.barelysorts.script;

import java.util.UUID;

/** Fired on the server after a group was sorted. */
public final class SortedEvent {
    public final String player;
    public final String uuid;
    public final String group;
    public final String method;
    public final String key;
    /** How many slots took part (locked slots are left out). */
    public final int slots;

    SortedEvent(String player, UUID uuid, String group, String method, String key, int slots) {
        this.player = player;
        this.uuid = uuid.toString();
        this.group = group;
        this.method = method;
        this.key = key;
        this.slots = slots;
    }
}
