package com.lann.itemfinder;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

import java.util.concurrent.atomic.AtomicInteger;

public class ServerDetector {

    private static boolean serverHasMod = false;
    private static String currentAddress = "singleplayer";
    private static AtomicInteger joinTickCounter = null;

    public static boolean serverHasMod() {
        return serverHasMod;
    }

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            var serverData = client.getCurrentServer();
            currentAddress = (serverData != null) ? serverData.ip : "singleplayer";

            serverHasMod = false;
            joinTickCounter = new AtomicInteger(0);

            CacheManager.onJoinServer(currentAddress);

            ClientTickEvents.START_CLIENT_TICK.register((c) -> {
                if (joinTickCounter == null) return;
                if (joinTickCounter.incrementAndGet() < 5) return;
                try {
                    serverHasMod = ClientPlayNetworking.canSend(SearchPacket.TYPE);
                } catch (Exception e) {
                    serverHasMod = false;
                }
                ItemFinderMod.LOGGER.info("[ItemFinder] Server has mod: " + serverHasMod
                    + " (checked after " + joinTickCounter + " ticks)");
                joinTickCounter = null;
            });

            if (client.level != null) {
                CacheManager.onChangeDimension(
                    client.level.dimension().identifier().toString()
                );
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            serverHasMod = false;
            joinTickCounter = null;
            CacheManager.onLeaveServer();
        });
    }
}
