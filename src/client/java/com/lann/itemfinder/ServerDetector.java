package com.lann.itemfinder;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public class ServerDetector {

    private static boolean serverHasMod = false;
    private static String currentAddress = "singleplayer";

    public static boolean serverHasMod() {
        return serverHasMod;
    }

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            var serverData = client.getCurrentServer();
            currentAddress = (serverData != null) ? serverData.ip : "singleplayer";

            serverHasMod = false;

            CacheManager.onJoinServer(currentAddress);

            client.execute(() -> {
                try {
                    serverHasMod = ClientPlayNetworking.canSend(SearchPacket.TYPE);
                } catch (Exception e) {
                    serverHasMod = false;
                }
                ItemFinderMod.LOGGER.info("[ItemFinder] Server has mod: " + serverHasMod);
            });

            if (client.level != null) {
                CacheManager.onChangeDimension(
                    client.level.dimension().location().toString()
                );
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            serverHasMod = false;
            CacheManager.onLeaveServer();
        });
    }
}