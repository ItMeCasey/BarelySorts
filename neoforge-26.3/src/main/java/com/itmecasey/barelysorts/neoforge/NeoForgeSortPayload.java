package com.itmecasey.barelysorts.neoforge;

import com.itmecasey.barelysorts.BarelySorts;
import com.itmecasey.barelysorts.Sorter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client asks the server to sort one group of the open container. */
public record NeoForgeSortPayload(int containerId, String group, String method, String key, String lockedIds)
        implements CustomPacketPayload {
    public static final Type<NeoForgeSortPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(BarelySorts.MODID, "sort"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NeoForgeSortPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, NeoForgeSortPayload::containerId,
            ByteBufCodecs.STRING_UTF8, NeoForgeSortPayload::group,
            ByteBufCodecs.STRING_UTF8, NeoForgeSortPayload::method,
            ByteBufCodecs.STRING_UTF8, NeoForgeSortPayload::key,
            ByteBufCodecs.STRING_UTF8, NeoForgeSortPayload::lockedIds,
            NeoForgeSortPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(NeoForgeSortPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                Sorter.sortGroup(player, payload.containerId(), payload.group(), payload.method(), payload.key(),
                        payload.lockedIds());
            }
        });
    }
}
