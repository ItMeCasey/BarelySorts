package com.itmecasey.barelysorts.script;

import java.util.regex.Pattern;

/** Item id patterns for categories: an exact id, or a glob where {@code *} matches any text ({@code mod:*}, {@code *_ingot}). */
final class Globs {
    private Globs() {
    }

    static boolean matches(String pattern, String id) {
        if (pattern == null || id == null) {
            return false;
        }
        String p = pattern.contains(":") ? pattern : "minecraft:" + pattern;
        StringBuilder regex = new StringBuilder();
        for (String part : p.split("[*]", -1)) {
            if (regex.length() > 0) {
                regex.append(".*");
            }
            regex.append(Pattern.quote(part));
        }
        return Pattern.matches(regex.toString(), id);
    }
}
