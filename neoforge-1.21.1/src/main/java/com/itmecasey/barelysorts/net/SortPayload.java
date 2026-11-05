package com.itmecasey.barelysorts.net;

import com.itmecasey.barelysorts.BarelySorts;
import com.itmecasey.barelysorts.Sorter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client asks the server to sort one group of the open container. */
public record SortPayload(int containerId, String group, String method, String key) implements CustomPacketPayload {
    public static final Type<SortPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(BarelySorts.MODID, "sort"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SortPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SortPayload::containerId,
            ByteBufCodecs.STRING_UTF8, SortPayload::group,
            ByteBufCodecs.STRING_UTF8, SortPayload::method,
            ByteBufCodecs.STRING_UTF8, SortPayload::key,
            SortPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SortPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                Sorter.sortGroup(player, payload);
            }
        });
    }
}
