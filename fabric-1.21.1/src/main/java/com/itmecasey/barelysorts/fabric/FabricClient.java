package com.itmecasey.barelysorts.fabric;

import com.itmecasey.barelysorts.Platform;
import com.itmecasey.barelysorts.client.AutofillSync;
import com.itmecasey.barelysorts.client.KeyBindings;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import com.itmecasey.barelysorts.client.ScreenActions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Client side of the Fabric layer: the buttons and the middle-click and R sorting on container screens. */
public final class FabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Platform.setSortSender((containerId, group, method, key, lockedIds) ->
                ClientPlayNetworking.send(new FabricSortPayload(containerId, group, method, key, lockedIds)));
        // key is widened by barelysorts.accesswidener, as are leftPos and topPos below.
        Platform.setKeyCodeReader(mapping -> mapping.key.getValue());
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
        KeyBindings.all().forEach(KeyBindingHelper::registerKeyBinding);

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof AbstractContainerScreen<?> container)) {
                return;
            }
            ScreenEvents.afterRender(screen).register((s, graphics, mouseX, mouseY, delta) ->
                    ScreenActions.render(container, graphics, mouseX, mouseY));
            ScreenMouseEvents.allowMouseClick(screen).register((s, mouseX, mouseY, button) ->
                    !ScreenActions.mouseButton(container, mouseX, mouseY, button));
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, key, scancode, modifiers) ->
                    !ScreenActions.key(container, key, scancode));
        });
    }
}
