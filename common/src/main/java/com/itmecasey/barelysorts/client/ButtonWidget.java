package com.itmecasey.barelysorts.client;

import net.minecraft.client.gui.GuiGraphics;

/** Two small icon buttons drawn by hand: a sort glyph (bars of falling length) and a hamburger (three bars). */
public final class ButtonWidget {
    public static final int SIZE = 8;

    private static final int FRAME = 0xFF2E2A46;
    private static final int FACE = 0xFF1A1830;
    private static final int HOVER_FACE = 0xFF2E2A46;
    private static final int ICON = 0xFFF2B33D;

    private ButtonWidget() {
    }

    public static void drawSort(GuiGraphics g, int x, int y, boolean hovered) {
        frame(g, x, y, hovered);
        int[] widths = {6, 4, 2};
        for (int i = 0; i < widths.length; i++) {
            g.fill(x + 1, y + 1 + i * 2, x + 1 + widths[i], y + 2 + i * 2, ICON);
        }
    }

    public static void drawMenu(GuiGraphics g, int x, int y, boolean hovered) {
        frame(g, x, y, hovered);
        for (int i = 0; i < 3; i++) {
            g.fill(x + 1, y + 1 + i * 2, x + SIZE - 1, y + 2 + i * 2, ICON);
        }
    }

    private static void frame(GuiGraphics g, int x, int y, boolean hovered) {
        g.fill(x - 1, y - 1, x + SIZE + 1, y + SIZE + 1, FRAME);
        g.fill(x, y, x + SIZE, y + SIZE, hovered ? HOVER_FACE : FACE);
    }

    public static boolean inside(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + SIZE && mouseY >= y && mouseY < y + SIZE;
    }
}
