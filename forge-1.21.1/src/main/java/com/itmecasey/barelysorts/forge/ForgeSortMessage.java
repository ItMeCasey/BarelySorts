package com.itmecasey.barelysorts.forge;

import com.itmecasey.barelysorts.Sorter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Client asks the server to sort one group of the open container. */
public record ForgeSortMessage(int containerId, String group, String method, String key) {
    static final StreamCodec<FriendlyByteBuf, ForgeSortMessage> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ForgeSortMessage::containerId,
            ByteBufCodecs.STRING_UTF8, ForgeSortMessage::group,
            ByteBufCodecs.STRING_UTF8, ForgeSortMessage::method,
            ByteBufCodecs.STRING_UTF8, ForgeSortMessage::key,
            ForgeSortMessage::new);

    /** Runs on the main thread, because the channel is set to consume messages there. */
    static void handle(ForgeSortMessage message, CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player != null) {
            Sorter.sortGroup(player, message.containerId(), message.group(), message.method(), message.key());
        }
    }
}
