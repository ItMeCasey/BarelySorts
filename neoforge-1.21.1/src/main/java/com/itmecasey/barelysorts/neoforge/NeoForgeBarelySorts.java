package com.itmecasey.barelysorts.neoforge;

import com.itmecasey.barelysorts.BarelySorts;
import com.itmecasey.barelysorts.Platform;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** NeoForge entry point. Everything shared lives in common/; this class only wires NeoForge into it. */
@Mod(BarelySorts.MODID)
public final class NeoForgeBarelySorts {
    public NeoForgeBarelySorts(IEventBus modBus) {
        modBus.addListener(this::registerPayloads);
        Platform.init(FMLPaths.CONFIGDIR::get,
                (containerId, group, method, key, lockedIds) -> PacketDistributor.sendToServer(
                        new NeoForgeSortPayload(containerId, group, method, key, lockedIds)));
        Platform.setGuiOrigin(new Platform.GuiOrigin() {
            @Override
            public int left(AbstractContainerScreen<?> screen) {
                return screen.getGuiLeft();
            }

            @Override
            public int top(AbstractContainerScreen<?> screen) {
                return screen.getGuiTop();
            }
        });
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(NeoForgeSortPayload.TYPE, NeoForgeSortPayload.CODEC,
                NeoForgeSortPayload::handle);
    }
}
