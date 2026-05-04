package com.lann.itemfinder;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemFinderMod implements ModInitializer {
    public static final String MOD_ID = "itemfinder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ConfigManager.load();

        // Register packets
        SearchPacket.register();
        SearchResultPacket.register();

        // Register server-side handler
        ServerNetworkHandler.register();

        LOGGER.info("ItemFinder loaded!");
    }
}
