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
        // Saat join server
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Deteksi address
            var serverData = client.getCurrentServer();
            currentAddress = (serverData != null) ? serverData.ip : "singleplayer";

            // Coba kirim handshake packet ke server
            // Kalau server punya mod, dia akan reply
            serverHasMod = false;

            // Load cache untuk server ini
            CacheManager.onJoinServer(currentAddress);

            // Cek apakah server support mod dengan cek channel
            client.execute(() -> {
                try {
                    // Kirim handshake, kalau server punya mod dia register channel ini
                    serverHasMod = ClientPlayNetworking.canSend(SearchPacket.TYPE);
                } catch (Exception e) {
                    serverHasMod = false;
                }
                ItemFinderMod.LOGGER.info("[ItemFinder] Server has mod: " + serverHasMod);
            });

            // Detect dimensi
            if (client.level != null) {
                CacheManager.onChangeDimension(
                    client.level.dimension().location().toString()
                );
            }
        });

        // Saat disconnect
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            serverHasMod = false;
            CacheManager.onLeaveServer();
        });
    }
}