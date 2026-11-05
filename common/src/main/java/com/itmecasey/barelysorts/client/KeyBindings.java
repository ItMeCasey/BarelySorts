package com.itmecasey.barelysorts.client;

import com.itmecasey.barelysorts.Platform;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The two keys the mod listens for. They are vanilla key mappings, so players rebind them in Options, Controls, under
 * BarelySorts. Each loader layer registers {@link #all()} with the game.
 */
public final class KeyBindings {
    /** The Controls screen groups the keys under this translation key (see assets/barelysorts/lang). */
    private static final String CATEGORY = "key.categories.barelysorts";

    /** Sorts the group under the mouse. Default R. */
    public static final KeyMapping SORT =
            new KeyMapping("key.barelysorts.sort", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);
    /** Hold to mark slots for deletion. Default X. */
    public static final KeyMapping MARK =
            new KeyMapping("key.barelysorts.mark", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY);

    private KeyBindings() {
    }

    public static List<KeyMapping> all() {
        return List.of(SORT, MARK);
    }

    /** Whether the mapping's key is held right now. Works on container screens, where the game does not track it. */
    public static boolean isDown(KeyMapping mapping) {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), Platform.keyCode(mapping));
    }

    public static boolean ctrlDown() {
        return isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    public static boolean shiftDown() {
        return isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private static boolean isKeyDown(int key) {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key);
    }
}
