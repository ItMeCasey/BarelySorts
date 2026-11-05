package com.itmecasey.barelysorts;

/** The per-group choices: how to sort, what to sort by, and whether the group is locked. */
public record GroupSettings(SortMethod method, SortKey key, boolean locked) {
    public static final GroupSettings DEFAULT = new GroupSettings(SortMethod.ROWS, SortKey.QUANTITY, false);

    public GroupSettings withMethod(SortMethod newMethod) {
        return new GroupSettings(newMethod, key, locked);
    }

    public GroupSettings withKey(SortKey newKey) {
        return new GroupSettings(method, newKey, locked);
    }

    public GroupSettings withLocked(boolean newLocked) {
        return new GroupSettings(method, key, newLocked);
    }
}
