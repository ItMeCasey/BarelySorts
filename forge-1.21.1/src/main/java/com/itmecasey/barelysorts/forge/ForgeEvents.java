package com.itmecasey.barelysorts.forge;

import com.itmecasey.barelysorts.Autofill;
import com.itmecasey.barelysorts.BarelySorts;
import com.itmecasey.barelysorts.client.AutofillSync;
import com.itmecasey.barelysorts.client.KeyBindings;
import com.itmecasey.barelysorts.client.ScreenActions;
import com.itmecasey.barelysorts.compat.DrawImpl;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-side hooks: autofill runs each player tick, and its state is dropped on logout. */
@Mod.EventBusSubscriber(modid = BarelySorts.MODID)
public final class ForgeEvents {
    private ForgeEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            Autofill.tick(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Autofill.forget(event.getEntity().getUUID());
    }

    /** Key mappings are registered on the mod bus, on the client only. */
    @Mod.EventBusSubscriber(modid = BarelySorts.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Keys {
        private Keys() {
        }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            KeyBindings.all().forEach(event::register);
        }
    }

    /** Client-side hooks: the buttons and the middle-click and R sorting on container screens. */
    @Mod.EventBusSubscriber(modid = BarelySorts.MODID, value = Dist.CLIENT)
    public static final class Screens {
        private Screens() {
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
                    && ScreenActions.key(screen, event.getKeyCode(), event.getScanCode())) {
                event.setCanceled(true);
            }
        }
    }
}
