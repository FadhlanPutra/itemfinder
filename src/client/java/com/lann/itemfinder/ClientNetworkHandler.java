package com.lann.itemfinder;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public class ClientNetworkHandler {

    public static java.util.function.Consumer<java.util.List<StorageScanner.SearchResult>> onResultReceived;

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(SearchResultPacket.TYPE, (payload, context) -> {
            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                if (onResultReceived != null) {
                    onResultReceived.accept(payload.results);
                }
            });
        });
    }

    public static void sendSearchAllRequest(String query) {
        ClientPlayNetworking.send(new SearchPacket("*" + query)); // prefix * = contains mode
    }
}
