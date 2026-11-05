package com.itmecasey.barelysorts.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The drawing calls the UI needs. Minecraft 1.21 draws with GuiGraphics and 1.20.1 with PoseStack, so each version
 * folder provides one {@code DrawImpl} that wraps its own type. The shared UI code only uses this interface.
 */
public interface Draw {
    void fill(int x1, int y1, int x2, int y2, int color);

    void text(Component text, int x, int y, int color);

    void centeredText(Component text, int centerX, int y, int color);

    /** The item's icon with its count. */
    void item(ItemStack stack, int x, int y);

    void tooltip(Component text, int mouseX, int mouseY);

    void push();

    void pop();

    void translate(float x, float y);

    /** Moves drawing in front of item stacks, which the game draws higher than plain fills. */
    void lift(float z);

    void scale(float factor);
}
