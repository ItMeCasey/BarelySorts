package com.itmecasey.barelysorts.forge;

import com.itmecasey.barelysorts.Autofill;
import com.itmecasey.barelysorts.BarelySorts;
import com.itmecasey.barelysorts.Compat;
import com.itmecasey.barelysorts.Platform;
import com.itmecasey.barelysorts.client.AutofillSync;
import com.itmecasey.barelysorts.compat.CompatImpl;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/**
 * Forge entry point for Minecraft 26.1.2. Everything shared lives in common/; this class only wires Forge into it.
 */
@Mod(BarelySorts.MODID)
public final class ForgeBarelySorts {
    private static final int PROTOCOL = 1;
    private static SimpleChannel channel;

    public ForgeBarelySorts(FMLJavaModLoadingContext context) {
        Compat.init(new CompatImpl());
        channel = ChannelBuilder.named(Identifier.fromNamespaceAndPath(BarelySorts.MODID, "sort"))
                .networkProtocolVersion(PROTOCOL)
                .optional()
                .simpleChannel();
        channel.messageBuilder(ForgeSortMessage.class)
                .codec(ForgeSortMessage.CODEC)
                .consumerMainThread(ForgeSortMessage::handle)
                .add();

        Platform.init(FMLPaths.CONFIGDIR::get,
                (containerId, group, method, key, lockedIds) -> channel.send(
                        new ForgeSortMessage(containerId, group, method, key, lockedIds),
                        PacketDistributor.SERVER.noArg()));

        TickEvent.PlayerTickEvent.Post.BUS.addListener((TickEvent.PlayerTickEvent.Post e) -> {
            if (e.player() instanceof ServerPlayer player) {
                Autofill.tick(player);
            }
        });
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) ->
                Autofill.forget(e.getEntity().getUUID()));

        if (FMLEnvironment.dist.isClient()) {
            ClientSetup.install(context.getModBusGroup());
        }
    }
}
