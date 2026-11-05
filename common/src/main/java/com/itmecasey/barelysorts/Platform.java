package com.itmecasey.barelysorts;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * The few things the shared code needs from the loader it is running in. Each loader layer sets these at startup,
 * so the shared code never refers to a loader's own classes.
 */
public final class Platform {
    /** Sends a sort request to the server. Set by the client side of each loader layer. */
    public interface SortSender {
        void send(int containerId, String group, String method, String key, String lockedIds);
    }

    /**
     * Where a container screen's GUI starts on the window. NeoForge and Forge expose it as a method; Fabric reads the
     * field through an access widener, so each loader supplies its own way.
     */
    public interface GuiOrigin {
        int left(AbstractContainerScreen<?> screen);

        int top(AbstractContainerScreen<?> screen);
    }

    private static Supplier<Path> configDir = () -> Path.of("config");
    private static GuiOrigin guiOrigin;
    private static SortSender sortSender = (containerId, group, method, key, lockedIds) -> {
    };

    private Platform() {
    }

    public static void init(Supplier<Path> configDirectory, SortSender sender) {
        configDir = configDirectory;
        sortSender = sender;
    }

    /** Sets only the sender, for loaders whose client side starts after the common setup. */
    public static void setSortSender(SortSender sender) {
        sortSender = sender;
    }

    public static void setGuiOrigin(GuiOrigin origin) {
        guiOrigin = origin;
    }

    public static GuiOrigin guiOrigin() {
        return guiOrigin;
    }

    public static Path configDir() {
        return configDir.get();
    }

    public static void sendSort(int containerId, String group, String method, String key, String lockedIds) {
        sortSender.send(containerId, group, method, key, lockedIds);
    }
}
