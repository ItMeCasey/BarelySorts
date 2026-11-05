package com.itmecasey.barelysorts.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.itmecasey.barelysorts.GroupSettings;
import com.itmecasey.barelysorts.Platform;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Per-group settings kept in the game's config folder. */
public final class ClientSettings {
    private static final String FILE_NAME = "barelysorts.json";
    private static final Type TYPE = new TypeToken<Map<String, GroupSettings>>() {
    }.getType();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Map<String, GroupSettings> groups;

    private ClientSettings() {
    }

    public static GroupSettings get(String groupKey) {
        return groups().getOrDefault(groupKey, GroupSettings.DEFAULT);
    }

    public static void put(String groupKey, GroupSettings settings) {
        groups().put(groupKey, settings);
        save();
    }

    private static Map<String, GroupSettings> groups() {
        if (groups == null) {
            groups = load();
        }
        return groups;
    }

    private static Path file() {
        return Platform.configDir().resolve(FILE_NAME);
    }

    private static Map<String, GroupSettings> load() {
        if (!Files.exists(file())) {
            return new HashMap<>();
        }
        try (Reader reader = Files.newBufferedReader(file())) {
            Map<String, GroupSettings> loaded = GSON.fromJson(reader, TYPE);
            return loaded == null ? new HashMap<>() : new HashMap<>(loaded);
        } catch (IOException | RuntimeException e) {
            return new HashMap<>();
        }
    }

    private static void save() {
        try {
            Files.createDirectories(file().getParent());
            try (Writer writer = Files.newBufferedWriter(file())) {
                GSON.toJson(groups, TYPE, writer);
            }
        } catch (IOException e) {
            // Settings still work for this session; they just will not survive a restart.
        }
    }
}
