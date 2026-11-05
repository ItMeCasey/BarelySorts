package com.itmecasey.barelysorts.compat;

import com.itmecasey.barelysorts.client.Draw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Minecraft 1.21.1: draws through GuiGraphics. */
public final class DrawImpl implements Draw {
    private final GuiGraphics g;

    public DrawImpl(GuiGraphics graphics) {
        this.g = graphics;
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y2, color);
    }

    @Override
    public void text(Component text, int x, int y, int color) {
        g.drawString(Minecraft.getInstance().font, text, x, y, color, false);
    }

    @Override
    public void centeredText(Component text, int centerX, int y, int color) {
        g.drawCenteredString(Minecraft.getInstance().font, text, centerX, y, color);
    }

    @Override
    public void item(ItemStack stack, int x, int y) {
        g.renderItem(stack, x, y);
        g.renderItemDecorations(Minecraft.getInstance().font, stack, x, y);
    }

    @Override
    public void tooltip(Component text, int mouseX, int mouseY) {
        g.renderTooltip(Minecraft.getInstance().font, text, mouseX, mouseY);
    }

    @Override
    public void push() {
        g.pose().pushPose();
    }

    @Override
    public void pop() {
        g.pose().popPose();
    }

    @Override
    public void translate(float x, float y) {
        g.pose().translate(x, y, 0);
    }

    @Override
    public void lift(float z) {
        g.pose().translate(0, 0, z);
    }

    @Override
    public void scale(float factor) {
        g.pose().scale(factor, factor, 1f);
    }
}
