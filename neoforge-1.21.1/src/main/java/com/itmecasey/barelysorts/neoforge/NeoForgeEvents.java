package com.itmecasey.barelysorts.neoforge;

import com.itmecasey.barelysorts.Autofill;
import com.itmecasey.barelysorts.BarelySorts;
import com.itmecasey.barelysorts.client.AutofillSync;
import com.itmecasey.barelysorts.client.KeyBindings;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import com.itmecasey.barelysorts.client.ScreenActions;
import com.itmecasey.barelysorts.compat.DrawImpl;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Server-side hooks: autofill runs each player tick, and its state is dropped on logout. */
@EventBusSubscriber(modid = BarelySorts.MODID)
public final class NeoForgeEvents {
    private NeoForgeEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Autofill.tick(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Autofill.forget(event.getEntity().getUUID());
    }
}

/** Client-side hooks: the buttons and the middle-click and R sorting on container screens. */
@EventBusSubscriber(modid = BarelySorts.MODID, value = Dist.CLIENT)
final class NeoForgeScreenEvents {
    private NeoForgeScreenEvents() {
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            ScreenActions.render(screen, new DrawImpl(event.getGuiGraphics()), event.getMouseX(), event.getMouseY());
        }
    }

    @SubscribeEvent
    public static void onMouse(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> screen
                && ScreenActions.mouseButton(screen, event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    /** Tells the server the saved autofill setting on every join. */
    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        AutofillSync.sendSaved();
    }

    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed.Pre event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> screen
                && ScreenActions.key(screen, event.getKeyCode(), m -> m.matches(event.getKeyCode(), event.getScanCode()))) {
            event.setCanceled(true);
        }
    }
}

/** Key mappings are registered on the mod bus, on the client only. */
@EventBusSubscriber(modid = BarelySorts.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
final class NeoForgeKeys {
    private NeoForgeKeys() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        KeyBindings.all().forEach(event::register);
    }
}
