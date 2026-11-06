package com.itmecasey.barelysorts.compat;

import com.itmecasey.barelysorts.client.Draw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Minecraft 26.1.2: draws through GuiGraphicsExtractor. The extractor has no z offset, so overlays move to the next
 * stratum, which the game draws above the item stacks.
 */
public final class DrawImpl implements Draw {
    private final GuiGraphicsExtractor g;

    public DrawImpl(GuiGraphicsExtractor graphics) {
        this.g = graphics;
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y2, color);
    }

    @Override
    public void text(Component text, int x, int y, int color) {
        g.text(Minecraft.getInstance().font, text, x, y, color, false);
    }

    @Override
    public void centeredText(Component text, int centerX, int y, int color) {
        g.centeredText(Minecraft.getInstance().font, text, centerX, y, color);
    }

    @Override
    public void item(ItemStack stack, int x, int y) {
        g.item(stack, x, y);
        g.itemDecorations(Minecraft.getInstance().font, stack, x, y);
    }

    @Override
    public void tooltip(Component text, int mouseX, int mouseY) {
        g.setTooltipForNextFrame(Minecraft.getInstance().font, text, mouseX, mouseY);
    }

    @Override
    public void push() {
        g.pose().pushMatrix();
    }

    @Override
    public void pop() {
        g.pose().popMatrix();
    }

    @Override
    public void translate(float x, float y) {
        g.pose().translate(x, y);
    }

    @Override
    public void lift(float z) {
        g.nextStratum();
    }

    @Override
    public void scale(float factor) {
        g.pose().scale(factor, factor);
    }
}
