package com.itmecasey.barelysorts.compat;

import com.itmecasey.barelysorts.Compat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

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
    public ResourceLocation id(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }
}
