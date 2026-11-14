package com.itmecasey.barelysorts.script;

/** What a script sees of an item stack: plain strings and a count. Public fields so scripts read {@code item.id}. */
public final class ItemView {
    /** Registry id, for example {@code minecraft:iron_ingot}. */
    public final String id;
    public final String namespace;
    /** Display name as the player sees it. */
    public final String name;
    public final int count;

    public ItemView(String id, String name, int count) {
        this.id = id;
        int colon = id.indexOf(':');
        this.namespace = colon < 0 ? "minecraft" : id.substring(0, colon);
        this.name = name;
        this.count = count;
    }

    @Override
    public String toString() {
        return id + " x" + count;
    }
}
