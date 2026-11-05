package com.itmecasey.barelysorts.client;

import com.itmecasey.barelysorts.Compat;
import com.itmecasey.barelysorts.Platform;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The two keys the mod listens for. They are vanilla key mappings, so players rebind them in Options, Controls, under
 * BarelySorts. Each loader layer registers {@link #all()} with the game.
 */
public final class KeyBindings {
    /** Sorts the group under the mouse. Default R. */
    public static final KeyMapping SORT =
            Compat.newKey("key.barelysorts.sort", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R);
    /** Hold to mark slots for deletion. Default X. */
    public static final KeyMapping MARK =
            Compat.newKey("key.barelysorts.mark", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X);

    private KeyBindings() {
    }

    /** Sorts the group when clicked. Default middle mouse button. Can be rebound to another mouse button. */
    public static final KeyMapping SORT_CLICK =
            Compat.newKey("key.barelysorts.sort_click", InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_MIDDLE);

    public static List<KeyMapping> all() {
        return List.of(SORT, MARK, SORT_CLICK);
    }

    /** Whether a mouse button press is the sort-click button. */
    public static boolean isSortClick(int button) {
        return Platform.keyCode(SORT_CLICK) == button;
    }

    /** Whether the mapping's key is held right now. Works on container screens, where the game does not track it. */
    public static boolean isDown(KeyMapping mapping) {
        return Compat.keyDown(Platform.keyCode(mapping));
    }

    public static boolean ctrlDown() {
        return isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    public static boolean shiftDown() {
        return isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private static boolean isKeyDown(int key) {
        return Compat.keyDown(key);
    }
}
