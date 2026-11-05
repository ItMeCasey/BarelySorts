package com.itmecasey.barelysorts;

/** How sorted stacks fill the group: rows run left to right, columns run top to bottom. */
public enum SortMethod {
    ROWS,
    COLUMNS;

    public SortMethod next() {
        return this == ROWS ? COLUMNS : ROWS;
    }

    public static SortMethod parse(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return ROWS;
        }
    }
}
