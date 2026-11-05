package com.itmecasey.barelysorts.fabric;

import com.itmecasey.barelysorts.BarelySorts;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client asks the server to sort one group of the open container. */
public record FabricSortPayload(int containerId, String group, String method, String key) implements CustomPacketPayload {
    public static final Type<FabricSortPayload> ID =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BarelySorts.MODID, "sort"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FabricSortPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FabricSortPayload::containerId,
            ByteBufCodecs.STRING_UTF8, FabricSortPayload::group,
            ByteBufCodecs.STRING_UTF8, FabricSortPayload::method,
            ByteBufCodecs.STRING_UTF8, FabricSortPayload::key,
            FabricSortPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
