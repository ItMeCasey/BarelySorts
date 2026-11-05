package com.itmecasey.barelysorts.compat;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.lang.reflect.Field;

/**
 * Where a container screen's GUI starts on the window. Minecraft 26.3 loaders no longer expose getGuiLeft and
 * getGuiTop, and the fields are protected, so they are read by reflection once and cached.
 */
public final class GuiPosition {
    private static final Field LEFT = field("leftPos");
    private static final Field TOP = field("topPos");

    private GuiPosition() {
    }

    public static int left(AbstractContainerScreen<?> screen) {
        return read(LEFT, screen);
    }

    public static int top(AbstractContainerScreen<?> screen) {
        return read(TOP, screen);
    }

    private static Field field(String name) {
        try {
            Field field = AbstractContainerScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("AbstractContainerScreen has no field " + name, e);
        }
    }

    private static int read(Field field, AbstractContainerScreen<?> screen) {
        try {
            return field.getInt(screen);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
