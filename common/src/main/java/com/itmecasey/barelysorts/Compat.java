package com.itmecasey.barelysorts;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The few calls that changed between Minecraft versions. Each version folder (version-1.21.1, version-1.20.1) provides
 * one {@code CompatImpl}, and each loader layer installs it once at startup, before anything else runs.
 */
public final class Compat {
    public interface Api {
        /** Whether two stacks hold the same item with the same data. Counts are ignored. */
        boolean sameItem(ItemStack a, ItemStack b);

        /** A hash of the stack's data, not counting the item or the count. */
        int dataHash(ItemStack stack);

        ResourceLocation id(String namespace, String path);
    }

    private static Api api;

    private Compat() {
    }

    public static void init(Api implementation) {
        api = implementation;
    }

    public static boolean sameItem(ItemStack a, ItemStack b) {
        return api.sameItem(a, b);
    }

    public static int dataHash(ItemStack stack) {
        return api.dataHash(stack);
    }

    public static ResourceLocation id(String namespace, String path) {
        return api.id(namespace, path);
    }
}
