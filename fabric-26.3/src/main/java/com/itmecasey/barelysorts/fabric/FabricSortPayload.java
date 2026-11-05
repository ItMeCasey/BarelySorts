package com.itmecasey.barelysorts.fabric;

import com.itmecasey.barelysorts.BarelySorts;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client asks the server to sort, delete, quick-move or change a setting. */
public record FabricSortPayload(int containerId, String group, String method, String key, String lockedIds)
        implements CustomPacketPayload {
    public static final Type<FabricSortPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(BarelySorts.MODID, "sort"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FabricSortPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FabricSortPayload::containerId,
            ByteBufCodecs.STRING_UTF8, FabricSortPayload::group,
            ByteBufCodecs.STRING_UTF8, FabricSortPayload::method,
            ByteBufCodecs.STRING_UTF8, FabricSortPayload::key,
            ByteBufCodecs.STRING_UTF8, FabricSortPayload::lockedIds,
            FabricSortPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
