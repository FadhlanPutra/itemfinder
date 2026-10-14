package com.lann.itemfinder;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public class ClientNetworkHandler {

    public static java.util.function.Consumer<java.util.List<StorageScanner.SearchResult>> onResultReceived;

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(SearchResultPacket.TYPE, (payload, context) -> {
            ItemFinderMod.LOGGER.info("[ItemFinder] S2C SearchResultPacket received with " + payload.results.size() + " results");
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                if (onResultReceived != null) {
                    onResultReceived.accept(payload.results);
                } else {
                    ItemFinderMod.LOGGER.warn("[ItemFinder] S2C arrived but onResultReceived is null (no active search?)");
                }
            });
        });
    }

    public static void sendSearchAllRequest(String query) {
        ItemFinderMod.LOGGER.info("[ItemFinder] C2S sendSearchAllRequest query='" + query + "', canSend=" + ClientPlayNetworking.canSend(SearchPacket.TYPE));
        ClientPlayNetworking.send(new SearchPacket("*" + query)); // prefix * = contains mode
    }
}
