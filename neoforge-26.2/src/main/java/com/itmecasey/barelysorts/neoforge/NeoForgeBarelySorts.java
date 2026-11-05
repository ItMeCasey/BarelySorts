package com.itmecasey.barelysorts.neoforge;

import com.itmecasey.barelysorts.BarelySorts;
import com.itmecasey.barelysorts.Compat;
import com.itmecasey.barelysorts.Platform;
import com.itmecasey.barelysorts.compat.CompatImpl;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** NeoForge entry point. Everything shared lives in common/; this class only wires NeoForge into it. */
@Mod(BarelySorts.MODID)
public final class NeoForgeBarelySorts {
    public NeoForgeBarelySorts(IEventBus modBus) {
        Compat.init(new CompatImpl());
        modBus.addListener(this::registerPayloads);
        Platform.init(FMLPaths.CONFIGDIR::get,
                (containerId, group, method, key, lockedIds) -> ClientPacketDistributor.sendToServer(
                        new NeoForgeSortPayload(containerId, group, method, key, lockedIds)));
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            NeoForgeClientSetup.install();
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(NeoForgeSortPayload.TYPE, NeoForgeSortPayload.CODEC,
                NeoForgeSortPayload::handle);
    }
}
