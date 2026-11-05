package com.itmecasey.barelysorts;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Path;

/**
 * The few calls that changed between Minecraft versions. Each version folder (version-1.21.1, version-1.20.1) provides
 * one {@code CompatImpl}, and each loader layer installs it once at startup, before anything else runs.
 */
public final class Compat {
    public interface Api {
        /** Whether two stacks hold the same item with the same data. Counts are ignored. */
        boolean sameItem(ItemStack a, ItemStack b);

        /** A hash of the stack's data, not counting the item or the count. */
        int dataHash(ItemStack stack);

        /** The style that draws text in the mod's small pixel font. */
        Style smallFont();

        /** A file in the world's folder for this server. */
        Path worldFile(ServerPlayer player, String name);

        /** The window handle GLFW uses, for mouse-button polling. */
        long windowHandle();

        /** Whether a GLFW key is held down right now. */
        boolean keyDown(int glfwKey);

        /** Whether a key mapping matches a key press. */
        boolean keyMatches(KeyMapping mapping, int keyCode, int scanCode);

        /** A key mapping in the mod's Controls category. */
        KeyMapping newKey(String name, InputConstants.Type type, int code);
    }

    private static Api api;

    private Compat() {
    }

    public static void init(Api implementation) {
        api = implementation;
    }

    public static boolean sameItem(ItemStack a, ItemStack b) {
        return api.sameItem(a, b);
    }

    public static int dataHash(ItemStack stack) {
        return api.dataHash(stack);
    }

    public static Style smallFont() {
        return api.smallFont();
    }

    public static Path worldFile(ServerPlayer player, String name) {
        return api.worldFile(player, name);
    }

    public static long windowHandle() {
        return api.windowHandle();
    }

    public static boolean keyDown(int glfwKey) {
        return api.keyDown(glfwKey);
    }

    public static boolean keyMatches(KeyMapping mapping, int keyCode, int scanCode) {
        return api.keyMatches(mapping, keyCode, scanCode);
    }

    public static KeyMapping newKey(String name, InputConstants.Type type, int code) {
        return api.newKey(name, type, code);
    }
}
