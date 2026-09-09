package com.lann.itemfinder;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.List;

public class ServerNetworkHandler {

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(SearchPacket.TYPE, (packet, context) -> {
            ServerPlayer player = context.player();
            ServerLevel level = (ServerLevel) player.level();
            ItemFinderMod.LOGGER.info("[ItemFinder] C2S received on server: itemId='" + packet.itemId + "' from " + player.getName().getString());

            context.server().execute(() -> {
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

                sortResults(results, player.blockPosition(), ConfigManager.get().sortMode);

                ItemFinderMod.LOGGER.info("[ItemFinder] server scan done, " + results.size() + " results, sending S2C to " + player.getName().getString());
                ServerPlayNetworking.send(player, new SearchResultPacket(results));
            });
        });
    }

    static void sortResults(List<StorageScanner.SearchResult> results, BlockPos playerPos, int sortMode) {
        if (sortMode == 0) {
            results.sort(Comparator.comparingDouble(r -> playerPos.distSqr(r.pos)));
        } else {
            results.sort(Comparator.comparingInt(
                (StorageScanner.SearchResult r) -> r.count).reversed());
        }
    }
}