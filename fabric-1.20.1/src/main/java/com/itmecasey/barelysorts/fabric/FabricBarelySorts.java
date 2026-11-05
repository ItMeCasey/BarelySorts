package com.itmecasey.barelysorts.fabric;

import com.itmecasey.barelysorts.Autofill;
import com.itmecasey.barelysorts.Compat;
import com.itmecasey.barelysorts.Platform;
import com.itmecasey.barelysorts.Sorter;
import com.itmecasey.barelysorts.compat.CompatImpl;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

/** Fabric entry point for Minecraft 1.20.1. Everything shared lives in common/; this class only wires Fabric into it. */
public final class FabricBarelySorts implements ModInitializer {
    @Override
    public void onInitialize() {
        Compat.init(new CompatImpl());
        ServerPlayNetworking.registerGlobalReceiver(FabricSortPayload.ID, (server, player, handler, buffer, sender) -> {
            FabricSortPayload payload = FabricSortPayload.decode(buffer);
            server.execute(() -> Sorter.sortGroup(player, payload.containerId(), payload.group(), payload.method(),
                    payload.key(), payload.lockedIds()));
        });

        Platform.init(() -> FabricLoader.getInstance().getConfigDir(), (containerId, group, method, key, lockedIds) -> {
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                Autofill.tick(player);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                Autofill.forget(handler.getPlayer().getUUID()));
    }
}
