package com.itmecasey.barelysorts.compat;

import com.itmecasey.barelysorts.Compat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Minecraft 1.21.1: items carry their data as data components. */
public final class CompatImpl implements Compat.Api {
    @Override
    public boolean sameItem(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameComponents(a, b);
    }

    @Override
    public int dataHash(ItemStack stack) {
        return stack.getComponents().hashCode();
    }

    @Override
    public ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
