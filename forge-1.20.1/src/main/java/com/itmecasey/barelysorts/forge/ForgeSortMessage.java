package com.itmecasey.barelysorts.forge;

import com.itmecasey.barelysorts.Sorter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client asks the server to sort, delete, quick-move or change a setting. Same meaning as the 1.21.1 payload. */
public record ForgeSortMessage(int containerId, String group, String method, String key, String lockedIds) {
    static void encode(ForgeSortMessage message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.containerId());
        buffer.writeUtf(message.group());
        buffer.writeUtf(message.method());
        buffer.writeUtf(message.key());
        buffer.writeUtf(message.lockedIds());
    }

    static ForgeSortMessage decode(FriendlyByteBuf buffer) {
        return new ForgeSortMessage(buffer.readVarInt(), buffer.readUtf(), buffer.readUtf(), buffer.readUtf(),
                buffer.readUtf());
    }

    static void handle(ForgeSortMessage message, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ServerPlayer player = ctx.getSender();
        ctx.enqueueWork(() -> {
            if (player != null) {
                Sorter.sortGroup(player, message.containerId(), message.group(), message.method(), message.key(),
                        message.lockedIds());
            }
        });
        ctx.setPacketHandled(true);
    }
}
