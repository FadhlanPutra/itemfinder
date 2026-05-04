package com.lann.itemfinder;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.List;

public class ServerNetworkHandler {

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(SearchPacket.TYPE, (packet, context) -> {
            ServerPlayer player = context.player();
            ServerLevel level = (ServerLevel) player.level();

            context.server().execute(() -> {
                // List<StorageScanner.SearchResult> results = StorageScanner.scanExact(
                //     context.server(),
                //     level,
                //     player.getName().getString(),
                //     player.blockPosition(),
                //     ConfigManager.get().radius,
                //     packet.itemId
                // );

            List<StorageScanner.SearchResult> results;

            if (packet.itemId.startsWith("*")) {
                // Contains mode
                String query = packet.itemId.substring(1);
                results = StorageScanner.scanContains(
                    context.server(), level,
                    player.getName().getString(),
                    player.blockPosition(),
                    ConfigManager.get().radius,
                    query
                );
            } else {
                // Exact mode
                results = StorageScanner.scanExact(
                    context.server(), level,
                    player.getName().getString(),
                    player.blockPosition(),
                    ConfigManager.get().radius,
                    packet.itemId
                );
            }

                if (ConfigManager.get().sortMode == 0) {
                    results.sort(Comparator.comparingDouble(r ->
                        player.blockPosition().distSqr(r.pos)));
                } else {
                    results.sort(Comparator.comparingInt(
                        (StorageScanner.SearchResult r) -> r.count).reversed());
                }

                ServerPlayNetworking.send(player, new SearchResultPacket(results));
            });
        });
    }
}
