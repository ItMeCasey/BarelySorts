package com.itmecasey.barelysorts.compat;

import com.itmecasey.barelysorts.Compat;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;

/** Minecraft 1.20.1: items carry their data as NBT tags. */
public final class CompatImpl implements Compat.Api {
    @Override
    public boolean sameItem(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameTags(a, b);
    }

    @Override
    public int dataHash(ItemStack stack) {
        return stack.getTag() == null ? 0 : stack.getTag().hashCode();
    }

    @Override
    public Style smallFont() {
        return Style.EMPTY.withFont(new ResourceLocation("barelysorts", "small"));
    }

    @Override
    public Path worldFile(ServerPlayer player, String name) {
        return player.getServer().getWorldPath(LevelResource.ROOT).resolve(name);
    }

    @Override
    public long windowHandle() {
        return Minecraft.getInstance().getWindow().getWindow();
    }

    @Override
    public boolean keyDown(int glfwKey) {
        return InputConstants.isKeyDown(windowHandle(), glfwKey);
    }

    @Override
    public boolean keyMatches(KeyMapping mapping, int keyCode, int scanCode) {
        return mapping.matches(keyCode, scanCode);
    }

    @Override
    public KeyMapping newKey(String name, InputConstants.Type type, int code) {
        return new KeyMapping(name, type, code, "key.categories.barelysorts");
    }
}
