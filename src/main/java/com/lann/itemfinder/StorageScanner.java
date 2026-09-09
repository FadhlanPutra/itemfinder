package com.lann.itemfinder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractChestBoat;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class StorageScanner {

    public static class SearchResult {
        public final BlockPos pos;
        public final String containerType;
        public final int count;

        public SearchResult(BlockPos pos, String containerType, int count) {
            this.pos = pos;
            this.containerType = containerType;
            this.count = count;
        }

        public String format() {
            return "§e" + friendlyName(containerType) +
                   " §fat §b" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() +
                   " §f→ §a" + count + " item";
        }

        private String friendlyName(String type) {
            if (type.equals("EnderChestBlockEntity")) return "Ender Chest";
            if (type.equals("MinecartChest")) return "Minecart Chest";
            if (type.equals("ChestBoat")) return "Chest Boat";

            String t = type.toLowerCase();
            if (t.contains("shulker_box")) return "Shulker Box";
            if (t.equals("trapped_chest")) return "Trapped Chest";
            if (t.equals("chest")) return "Chest";
            if (t.equals("barrel")) return "Barrel";
            if (t.equals("blast_furnace")) return "Blast Furnace";
            if (t.equals("smoker")) return "Smoker";
            if (t.equals("furnace")) return "Furnace";
            if (t.equals("hopper")) return "Hopper";
            if (t.equals("dispenser")) return "Dispenser";
            if (t.equals("dropper")) return "Dropper";

            return capitalizeWords(t.replace('_', ' '));
        }

        private String capitalizeWords(String s) {
            String[] parts = s.split(" ");
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                if (part.isEmpty()) continue;
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
            return sb.toString();
        }
    }

    public static List<SearchResult> scanExact(MinecraftServer server, ServerLevel serverLevel,
                                                String playerName, BlockPos center,
                                                int radius, String targetItemId) {
        List<SearchResult> results = new ArrayList<>();

        // Scan block entity (chest, barrel, shulker box, etc.)
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);

                    var blockState = serverLevel.getBlockState(pos);
                    if (blockState.isAir() || !blockState.hasBlockEntity()) continue;

                    BlockEntity blockEntity = serverLevel.getBlockEntity(pos);
                    if (blockEntity == null) continue;

                    if (blockEntity instanceof EnderChestBlockEntity) {
                        var serverPlayer = server.getPlayerList().getPlayerByName(playerName);
                        if (serverPlayer != null) {
                            int found = countMatchingExact(serverPlayer.getEnderChestInventory(), targetItemId);
                            if (found > 0) {
                                results.add(new SearchResult(pos, "EnderChestBlockEntity", found));
                            }
                        }
                        continue;
                    }

                    if (blockEntity instanceof Container container) {
                        int found = countMatchingExact(container, targetItemId);
                        if (found > 0) {
                            String blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).getPath();
                            results.add(new SearchResult(pos, blockId, found));
                        }
                    }
                }
            }
        }

        // Scan entity (minecart chest, chest boat)
        AABB searchBox = new AABB(
            center.getX() - radius, center.getY() - radius, center.getZ() - radius,
            center.getX() + radius, center.getY() + radius, center.getZ() + radius
        );

        List<Entity> entities = serverLevel.getEntities(null, searchBox);
        for (Entity entity : entities) {
            Container container = null;
            String typeName = null;

            if (entity instanceof MinecartChest minecart) {
                container = minecart;
                typeName = "MinecartChest";
            } else if (entity instanceof AbstractChestBoat chestBoat) {
                container = chestBoat;
                typeName = "ChestBoat";
            }

            if (container != null && typeName != null) {
                int found = countMatchingExact(container, targetItemId);
                if (found > 0) {
                    BlockPos entityPos = entity.blockPosition();
                    results.add(new SearchResult(entityPos, typeName, found));
                }
            }
        }

        return results;
    }

    public static List<SearchResult> scanContains(MinecraftServer server, ServerLevel serverLevel,
                                                   String playerName, BlockPos center,
                                                   int radius, String query) {
        List<SearchResult> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    var blockState = serverLevel.getBlockState(pos);
                    if (blockState.isAir() || !blockState.hasBlockEntity()) continue;
                    BlockEntity blockEntity = serverLevel.getBlockEntity(pos);
                    if (blockEntity == null) continue;

                    if (blockEntity instanceof EnderChestBlockEntity) {
                        var serverPlayer = server.getPlayerList().getPlayerByName(playerName);
                        if (serverPlayer != null) {
                            int found = countMatchingContains(serverPlayer.getEnderChestInventory(), lowerQuery);
                            if (found > 0) results.add(new SearchResult(pos, "EnderChestBlockEntity", found));
                        }
                        continue;
                    }

                    if (blockEntity instanceof Container container) {
                        int found = countMatchingContains(container, lowerQuery);
                        if (found > 0) {
                            String blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).getPath();
                            results.add(new SearchResult(pos, blockId, found));
                        }
                    }
                }
            }
        }

        // Entity scan
        AABB searchBox = new AABB(
            center.getX() - radius, center.getY() - radius, center.getZ() - radius,
            center.getX() + radius, center.getY() + radius, center.getZ() + radius
        );
        for (Entity entity : serverLevel.getEntities(null, searchBox)) {
            Container container = null;
            String typeName = null;
            if (entity instanceof MinecartChest m) { container = m; typeName = "MinecartChest"; }
            else if (entity instanceof AbstractChestBoat b) { container = b; typeName = "ChestBoat"; }
            if (container != null) {
                int found = countMatchingContains(container, lowerQuery);
                if (found > 0) results.add(new SearchResult(entity.blockPosition(), typeName, found));
            }
        }

        return results;
    }

    private static int countMatchingExact(Container container, String targetItemId) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) {
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                if (itemId.equals(targetItemId)) total += stack.getCount();
            }
        }
        return total;
    }

    private static int countMatchingContains(Container container, String query) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) {
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase();
                String displayName = stack.getHoverName().getString().toLowerCase();
                if (itemId.contains(query) || displayName.contains(query)) total += stack.getCount();
            }
        }
        return total;
    }
}