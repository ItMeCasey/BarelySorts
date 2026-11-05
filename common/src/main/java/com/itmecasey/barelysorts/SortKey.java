package com.itmecasey.barelysorts;

/** What the stacks are ordered by. */
public enum SortKey {
    QUANTITY,
    NAME,
    MOD;

    public SortKey next() {
        return switch (this) {
            case QUANTITY -> NAME;
            case NAME -> MOD;
            case MOD -> QUANTITY;
        };
    }

    public static SortKey parse(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return QUANTITY;
        }
    }
}
