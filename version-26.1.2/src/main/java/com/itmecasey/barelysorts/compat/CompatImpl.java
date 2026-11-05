package com.itmecasey.barelysorts.compat;

import com.itmecasey.barelysorts.Compat;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;

/** Minecraft 26.1.2: items carry their data as data components, and identifiers are called Identifier. */
public final class CompatImpl implements Compat.Api {
    /** Holds the key category so the client-only class loads only when a key is created, never on a server. */
    private static final class Category {
        static final KeyMapping.Category VALUE =
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath("barelysorts", "keys"));
    }

    @Override
    public boolean sameItem(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameComponents(a, b);
    }

    @Override
    public int dataHash(ItemStack stack) {
        return stack.getComponents().hashCode();
    }

    @Override
    public Style smallFont() {
        return Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("barelysorts", "small")));
    }

    @Override
    public Path worldFile(ServerPlayer player, String name) {
        return player.level().getServer().getWorldPath(LevelResource.ROOT).resolve(name);
    }

    @Override
    public long windowHandle() {
        return Minecraft.getInstance().getWindow().handle();
    }

    @Override
    public boolean keyDown(int glfwKey) {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), glfwKey);
    }

    @Override
    public boolean keyMatches(KeyMapping mapping, int keyCode, int scanCode) {
        return mapping.matches(new KeyEvent(keyCode, scanCode, 0));
    }

    @Override
    public KeyMapping newKey(String name, InputConstants.Type type, int code) {
        return new KeyMapping(name, type, code, Category.VALUE);
    }
}
