package com.itmecasey.barelysorts;

import com.itmecasey.barelysorts.net.SortPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(BarelySorts.MODID)
public final class BarelySorts {
    public static final String MODID = "barelysorts";

    public BarelySorts(IEventBus modBus) {
        modBus.addListener(this::registerPayloads);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(SortPayload.TYPE, SortPayload.CODEC, SortPayload::handle);
    }
}
