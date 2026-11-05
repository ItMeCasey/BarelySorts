package com.itmecasey.barelysorts.client;

import com.itmecasey.barelysorts.GroupSettings;
import com.itmecasey.barelysorts.SortKey;
import com.itmecasey.barelysorts.SortMethod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The page the hamburger button opens: sort method, sort by, and lock for one group. */
public final class SettingsScreen extends Screen {
    private final Screen parent;
    private final String settingsKey;
    private final String groupLabel;
    private GroupSettings settings;

    private Button methodButton;
    private Button keyButton;
    private Button lockButton;

    public SettingsScreen(Screen parent, String settingsKey, String groupLabel) {
        super(Component.literal("BarelySorts"));
        this.parent = parent;
        this.settingsKey = settingsKey;
        this.groupLabel = groupLabel;
        this.settings = ClientSettings.get(settingsKey);
    }

    @Override
    protected void init() {
        int buttonWidth = 180;
        int x = this.width / 2 - buttonWidth / 2;
        int y = this.height / 2 - 60;
        methodButton = addRenderableWidget(Button.builder(methodLabel(), b -> {
            settings = settings.withMethod(settings.method().next());
            save();
            b.setMessage(methodLabel());
        }).bounds(x, y, width, 20).build());
        keyButton = addRenderableWidget(Button.builder(keyLabel(), b -> {
            settings = settings.withKey(settings.key().next());
            save();
            b.setMessage(keyLabel());
        }).bounds(x, y + 26, width, 20).build());
        lockButton = addRenderableWidget(Button.builder(lockLabel(), b -> {
            settings = settings.withLocked(!settings.locked());
            save();
            b.setMessage(lockLabel());
        }).bounds(x, y + 52, width, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(x, y + 90, width, 20).build());
    }

    private void save() {
        ClientSettings.put(settingsKey, settings);
    }

    private Component methodLabel() {
        return Component.literal("Sort method: " + (settings.method() == SortMethod.ROWS
                ? "Rows (left to right)" : "Columns (top to bottom)"));
    }

    private Component keyLabel() {
        String key = switch (settings.key()) {
            case QUANTITY -> "Quantity";
            case NAME -> "Item name";
            case MOD -> "Mod name";
        };
        return Component.literal("Sort by: " + key);
    }

    private Component lockLabel() {
        return Component.literal("Lock: " + (settings.locked() ? "On (no sorting)" : "Off"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 84, 0xFFF2B33D);
        graphics.drawCenteredString(font, groupLabel, width / 2, height / 2 - 72, 0xFFA9A4C8);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
