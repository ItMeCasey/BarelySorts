package com.itmecasey.barelysorts.fabric;

import com.itmecasey.barelysorts.Platform;
import com.itmecasey.barelysorts.client.AutofillSync;
import com.itmecasey.barelysorts.client.KeyBindings;
import com.itmecasey.barelysorts.client.ScreenActions;
import com.itmecasey.barelysorts.compat.DrawImpl;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.FriendlyByteBuf;

/** Client side of the Fabric 1.20.1 layer: the buttons, the popup and the hotkeys on container screens. */
public final class FabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // key and leftPos/topPos are widened by barelysorts.accesswidener.
        Platform.setSortSender((containerId, group, method, key, lockedIds) -> {
            FriendlyByteBuf buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            new FabricSortPayload(containerId, group, method, key, lockedIds).encode(buffer);
            ClientPlayNetworking.send(FabricSortPayload.ID, buffer);
        });
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
                    ScreenActions.render(container, new DrawImpl(graphics), mouseX, mouseY));
            ScreenMouseEvents.allowMouseClick(screen).register((s, mouseX, mouseY, button) ->
                    !ScreenActions.mouseButton(container, mouseX, mouseY, button));
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, key, scancode, modifiers) ->
                    !ScreenActions.key(container, key, scancode));
        });
    }
}
