package com.itmecasey.barelysorts.fabric;

import com.itmecasey.barelysorts.BarelySorts;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Client asks the server to sort, delete, quick-move or change a setting. Minecraft 1.20.1 has no payload registry, so the
 * fields are written into a plain buffer on the sort channel.
 */
public record FabricSortPayload(int containerId, String group, String method, String key, String lockedIds) {
    public static final ResourceLocation ID = new ResourceLocation(BarelySorts.MODID, "sort");

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
        buffer.writeUtf(group);
        buffer.writeUtf(method);
        buffer.writeUtf(key);
        buffer.writeUtf(lockedIds);
    }

    public static FabricSortPayload decode(FriendlyByteBuf buffer) {
        return new FabricSortPayload(buffer.readVarInt(), buffer.readUtf(), buffer.readUtf(), buffer.readUtf(),
                buffer.readUtf());
    }
}
