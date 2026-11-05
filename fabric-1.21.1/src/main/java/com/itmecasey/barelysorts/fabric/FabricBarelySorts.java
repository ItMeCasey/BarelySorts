package com.itmecasey.barelysorts.fabric;

import com.itmecasey.barelysorts.Autofill;
import com.itmecasey.barelysorts.Platform;
import com.itmecasey.barelysorts.Sorter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

/** Fabric entry point. Everything shared lives in common/; this class only wires Fabric into it. */
public final class FabricBarelySorts implements ModInitializer {
    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(FabricSortPayload.ID, FabricSortPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(FabricSortPayload.ID, (payload, context) ->
                context.server().execute(() -> Sorter.sortGroup(context.player(), payload.containerId(),
                        payload.group(), payload.method(), payload.key())));

        Platform.init(() -> FabricLoader.getInstance().getConfigDir(), (containerId, group, method, key) -> {
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
