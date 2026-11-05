package com.itmecasey.barelysorts.client;

import com.itmecasey.barelysorts.BarelySorts;
import com.itmecasey.barelysorts.GroupSettings;
import com.itmecasey.barelysorts.net.SortPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Adds the sort and settings buttons to every inventory group, and the middle-click and R sorting, to container
 * screens. The buttons are drawn and hit-tested here rather than added as widgets, so the screen's own widget list
 * is not changed.
 */
@EventBusSubscriber(modid = BarelySorts.MODID, value = Dist.CLIENT)
public final class ScreenHooks {
    private static final int MIDDLE_BUTTON = 2;
    private static final int LEFT_BUTTON = 0;

    private ScreenHooks() {
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        double mx = event.getMouseX();
        double my = event.getMouseY();
        for (Group group : Group.of(screen)) {
            boolean locked = ClientSettings.get(group.settingsKey()).locked();
            boolean sortHover = ButtonWidget.inside(mx, my, group.sortButtonX(), group.sortButtonY());
            boolean menuHover = ButtonWidget.inside(mx, my, group.sortButtonX(), group.settingsButtonY());
            ButtonWidget.drawSort(event.getGuiGraphics(), group.sortButtonX(), group.sortButtonY(), sortHover, locked);
            ButtonWidget.drawMenu(event.getGuiGraphics(), group.sortButtonX(), group.settingsButtonY(), menuHover);
        }
    }

    @SubscribeEvent
    public static void onMouse(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        double mx = event.getMouseX();
        double my = event.getMouseY();
        for (Group group : Group.of(screen)) {
            if (event.getButton() == LEFT_BUTTON
                    && ButtonWidget.inside(mx, my, group.sortButtonX(), group.settingsButtonY())) {
                Minecraft.getInstance().setScreen(new SettingsScreen(screen, group.settingsKey(), group.name()));
                event.setCanceled(true);
                return;
            }
            if (event.getButton() == LEFT_BUTTON && ButtonWidget.inside(mx, my, group.sortButtonX(), group.sortButtonY())) {
                sort(screen, group);
                event.setCanceled(true);
                return;
            }
            if (event.getButton() == MIDDLE_BUTTON && group.contains(mx, my)) {
                sort(screen, group);
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || event.getKeyCode() != GLFW.GLFW_KEY_R) {
            return;
        }
        // R sorts the group under the mouse. Away from a group it does nothing, so the recipe book key keeps working.
        Minecraft minecraft = Minecraft.getInstance();
        var window = minecraft.getWindow();
        double mx = minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth();
        double my = minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight();
        List<Group> groups = Group.of(screen);
        for (Group group : groups) {
            if (group.contains(mx, my)) {
                sort(screen, group);
                event.setCanceled(true);
                return;
            }
        }
    }

    private static void sort(AbstractContainerScreen<?> screen, Group group) {
        GroupSettings settings = ClientSettings.get(group.settingsKey());
        if (settings.locked()) {
            return;
        }
        PacketDistributor.sendToServer(new SortPayload(
                screen.getMenu().containerId,
                group.name(),
                settings.method().name(),
                settings.key().name()));
    }
}
