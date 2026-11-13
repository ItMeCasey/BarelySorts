package com.itmecasey.barelysorts.fabric;

import com.itmecasey.barelysorts.Platform;
import com.itmecasey.barelysorts.client.AutofillSync;
import com.itmecasey.barelysorts.client.KeyBindings;
import com.itmecasey.barelysorts.client.ScreenActions;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Client side of the Fabric 26.1.2 layer: the buttons, the popup and the hotkeys on container screens. */
public final class FabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Platform.setSortSender((containerId, group, method, key, lockedIds) ->
                ClientPlayNetworking.send(new FabricSortPayload(containerId, group, method, key, lockedIds)));
        Platform.setKeyCodeReader(mapping -> KeyMappingHelper.getBoundKeyOf(mapping).getValue());
        // leftPos and topPos are widened by barelysorts.accesswidener.
        Platform.setGuiOrigin(new Platform.GuiOrigin() {
            @Override
            public int left(AbstractContainerScreen<?> screen) {
                return screen.leftPos;
            }

            @Override
            public int top(AbstractContainerScreen<?> screen) {
                return screen.topPos;
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> AutofillSync.sendSaved());
        KeyBindings.all().forEach(KeyMappingHelper::registerKeyMapping);

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof AbstractContainerScreen<?> container)) {
                return;
            }
            // The overlay is drawn by ContainerScreenMixin, before the game queues its tooltip.
            ScreenMouseEvents.allowMouseClick(screen).register((s, event) ->
                    !ScreenActions.mouseButton(container, event.x(), event.y(), event.button()));
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) ->
                    !ScreenActions.key(container, event.key(), m -> m.matches(event)));
        });
    }
}
