package com.lann.itemfinder;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class ItemFinderClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ItemFinderMod.LOGGER.info("ItemFinder client loaded!");
        KeyBindings.register();
        HighlightRenderer.register();
        HudOverlay.register();
        ParticleTrail.register();
        ClientNetworkHandler.register();
        ServerDetector.register(); // tambahkan ini

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (KeyBindings.OPEN_SEARCH.consumeClick()) {
                if (client.player != null) {
                    client.setScreen(new SearchScreen());
                }
            }
        });
    }
}
