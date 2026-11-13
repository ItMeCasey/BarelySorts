package com.itmecasey.barelysorts.forge;

import com.itmecasey.barelysorts.Platform;
import com.itmecasey.barelysorts.client.AutofillSync;
import com.itmecasey.barelysorts.client.KeyBindings;
import com.itmecasey.barelysorts.client.ScreenActions;
import com.itmecasey.barelysorts.compat.DrawImpl;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;

/**
 * Client-only wiring for Minecraft 26.1.2: key mappings, screen drawing and clicks, and the autofill sync on join.
 * It lives in its own class so a dedicated server never loads the client types it names. Cancellable events are
 * cancelled by returning true from the listener.
 */
final class ClientSetup {
    private ClientSetup() {
    }

    static void install(BusGroup modBus) {
        Platform.setKeyCodeReader(mapping -> mapping.getKey().getValue());
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

        RegisterKeyMappingsEvent.BUS.addListener((RegisterKeyMappingsEvent e) ->
                KeyBindings.all().forEach(e::register));

        ClientPlayerNetworkEvent.LoggingIn.BUS.addListener((ClientPlayerNetworkEvent.LoggingIn e) ->
                AutofillSync.sendSaved());

        // The container Foreground event fires before the game queues its tooltip, so tooltips stay on top. It runs
        // inside the translated container pose, so move back to screen coordinates for the shared code.
        ContainerScreenEvent.Render.Foreground.BUS.addListener((ContainerScreenEvent.Render.Foreground e) -> {
            AbstractContainerScreen<?> screen = e.getContainerScreen();
            DrawImpl draw = new DrawImpl(e.getGuiGraphics());
            draw.push();
            draw.translate(-Platform.guiOrigin().left(screen), -Platform.guiOrigin().top(screen));
            ScreenActions.render(screen, draw, e.getMouseX(), e.getMouseY());
            draw.pop();
        });
        ScreenEvent.MouseButtonPressed.Pre.BUS.addListener((ScreenEvent.MouseButtonPressed.Pre e) ->
                e.getScreen() instanceof AbstractContainerScreen<?> screen
                        && ScreenActions.mouseButton(screen, e.getMouseX(), e.getMouseY(), e.getInfo().button()));
        ScreenEvent.KeyPressed.Pre.BUS.addListener((ScreenEvent.KeyPressed.Pre e) ->
                e.getScreen() instanceof AbstractContainerScreen<?> screen
                        && ScreenActions.key(screen, e.getKeyCode(), m -> m.matches(
                                new net.minecraft.client.input.KeyEvent(e.getKeyCode(), e.getScanCode(), 0))));
    }
}
