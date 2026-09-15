package com.lann.itemfinder;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.HashMap;
import java.util.Map;

public class ItemFinderClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ItemFinderMod.LOGGER.info("ItemFinder client loaded!");
        KeyBindings.register();
        HighlightRenderer.register();
        HudOverlay.register();
        ParticleTrail.register();
        ClientNetworkHandler.register();
        ServerDetector.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (KeyBindings.OPEN_SEARCH.consumeClick()) {
                ItemFinderMod.LOGGER.info("[ItemFinder] OPEN_SEARCH key consumed, opening SearchScreen");
                if (client.player != null) {
                    client.gui.setScreen(new SearchScreen());
                }
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) return;
            if (client.player == null) return;

            BlockPos playerPos = client.player.blockPosition();
            BlockPos nearestPos = null;
            double nearestDistSq = Double.MAX_VALUE;

            for (BlockPos pos : HighlightRenderer.highlightedPositions.keySet()) {
                double distSq = playerPos.distSqr(pos);
                if (distSq <= 25.0 && distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearestPos = pos;
                }
            }

            if (nearestPos != null) {
                HighlightRenderer.highlightedPositions.remove(nearestPos);
                ParticleTrail.removeTarget(nearestPos);
                if (HighlightRenderer.highlightedPositions.isEmpty()) {
                    HighlightRenderer.clearHighlights();
                    ParticleTrail.clearAll();
                }
            }

            cacheOpenedContainer(client, containerScreen);
        });
    }

    private void cacheOpenedContainer(Minecraft client, AbstractContainerScreen<?> screen) {
        if (client.player == null || client.level == null) return;
        if (!CacheManager.isAvailable()) return;

        BlockPos playerPos = client.player.blockPosition();
        AbstractContainerMenu menu = screen.getMenu();

        Map<String, Integer> items = new HashMap<>();
        int containerSlots = menu.slots.size();

        int playerInvStart = containerSlots - 36; // 27 main + 9 hotbar
        if (playerInvStart < 0) playerInvStart = 0;

        for (int i = 0; i < playerInvStart; i++) {
            ItemStack stack = menu.slots.get(i).getItem();
            if (!stack.isEmpty()) {
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                items.merge(id, stack.getCount(), Integer::sum);
            }
        }

        if (items.isEmpty()) return;
        for (int x = -5; x <= 5; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    var blockState = client.level.getBlockState(pos);
                    if (blockState.hasBlockEntity()) {
                        var be = client.level.getBlockEntity(pos);
                        if (be instanceof Container) {
                            String typeName = be.getClass().getSimpleName();
                            CacheManager.cacheContainer(pos, typeName, items);
                            return;
                        }
                    }
                }
            }
        }
    }
}
