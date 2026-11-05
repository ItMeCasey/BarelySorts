package com.itmecasey.barelysorts.forge;

import com.itmecasey.barelysorts.BarelySorts;
import com.itmecasey.barelysorts.Platform;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
import net.minecraft.resources.ResourceLocation;

/** Forge entry point. Everything shared lives in common/; this class only wires Forge into it. */
@Mod(BarelySorts.MODID)
public final class ForgeBarelySorts {
    private static SimpleChannel channel;

    public ForgeBarelySorts(FMLJavaModLoadingContext context) {
        channel = ChannelBuilder.named(ResourceLocation.fromNamespaceAndPath(BarelySorts.MODID, "sort"))
                .networkProtocolVersion(1)
                .optional()
                .simpleChannel();
        channel.messageBuilder(ForgeSortMessage.class)
                .codec(ForgeSortMessage.CODEC)
                .consumerMainThread(ForgeSortMessage::handle)
                .add();

        Platform.init(FMLPaths.CONFIGDIR::get,
                (containerId, group, method, key, lockedIds) -> channel.send(
                        new ForgeSortMessage(containerId, group, method, key, lockedIds), PacketDistributor.SERVER.noArg()));
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
}
