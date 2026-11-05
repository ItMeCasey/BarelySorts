package com.itmecasey.barelysorts;

import java.util.HashSet;
import java.util.Set;

/**
 * The lock list travels in a request as comma-separated text. This reads it without touching any Minecraft class, so
 * it can be tested on its own, and a bad value from a client can never throw.
 */
final class LockedIds {
    private LockedIds() {
    }

    /** The ids in the list. Spaces, blanks, duplicates and anything that is not a number are ignored. */
    static Set<Integer> parse(String ids) {
        Set<Integer> out = new HashSet<>();
        if (ids == null || ids.isEmpty()) {
            return out;
        }
        for (String part : ids.split(",")) {
            try {
                out.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException e) {
                // Not an id; skip it.
            }
        }
        return out;
    }
}
