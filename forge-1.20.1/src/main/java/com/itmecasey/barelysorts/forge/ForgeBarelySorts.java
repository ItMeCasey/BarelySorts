package com.itmecasey.barelysorts.forge;

import com.itmecasey.barelysorts.BarelySorts;
import com.itmecasey.barelysorts.Compat;
import com.itmecasey.barelysorts.Platform;
import com.itmecasey.barelysorts.compat.CompatImpl;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Forge and NeoForge entry point for Minecraft 1.20.1. NeoForge 1.20.1 keeps the Forge packages, so this one class
 * serves both loaders. Everything shared lives in common/; this class only wires the loader into it.
 */
@Mod(BarelySorts.MODID)
public final class ForgeBarelySorts {
    private static final String PROTOCOL = "1";
    private static SimpleChannel channel;

    public ForgeBarelySorts() {
        Compat.init(new CompatImpl());
        channel = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(BarelySorts.MODID, "sort"))
                .networkProtocolVersion(() -> PROTOCOL)
                .clientAcceptedVersions(version -> true)
                .serverAcceptedVersions(version -> true)
                .simpleChannel();
        channel.registerMessage(0, ForgeSortMessage.class, ForgeSortMessage::encode, ForgeSortMessage::decode,
                ForgeSortMessage::handle);

        Platform.init(FMLPaths.CONFIGDIR::get,
                (containerId, group, method, key, lockedIds) -> channel.sendToServer(
                        new ForgeSortMessage(containerId, group, method, key, lockedIds)));
        if (FMLEnvironment.dist.isClient()) {
            ClientSetup.install();
        }
    }
}
