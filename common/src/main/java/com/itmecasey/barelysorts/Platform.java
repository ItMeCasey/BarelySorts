package com.itmecasey.barelysorts;

import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * The few things the shared code needs from the loader it is running in. Each loader layer sets these at startup,
 * so the shared code never refers to a loader's own classes.
 */
public final class Platform {
    /** Sends a sort request to the server. Set by the client side of each loader layer. */
    public interface SortSender {
        void send(int containerId, String group, String method, String key);
    }

    private static Supplier<Path> configDir = () -> Path.of("config");
    private static SortSender sortSender = (containerId, group, method, key) -> {
    };

    private Platform() {
    }

    public static void init(Supplier<Path> configDirectory, SortSender sender) {
        configDir = configDirectory;
        sortSender = sender;
    }

    public static Path configDir() {
        return configDir.get();
    }

    public static void sendSort(int containerId, String group, String method, String key) {
        sortSender.send(containerId, group, method, key);
    }
}
