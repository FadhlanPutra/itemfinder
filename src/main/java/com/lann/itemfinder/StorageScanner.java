package com.lann.itemfinder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractChestBoat;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StorageScanner {

    public static class SearchResult {
        public final BlockPos pos;
        public final String containerType;
        public final int count;
        public final Map<String, Integer> items;

        public SearchResult(BlockPos pos, String containerType, int count) {
            this.pos = pos;
            this.containerType = containerType;
            this.count = count;
            this.items = new HashMap<>();
        }

        public SearchResult(BlockPos pos, String containerType, int count, Map<String, Integer> items) {
            this.pos = pos;
            this.containerType = containerType;
            this.count = count;
            this.items = items;
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
            if (type.equals("MinecartHopper")) return "Minecart with Hopper";

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
                            Map<String, Integer> items = new HashMap<>();
                            int found = countMatchingExact(serverPlayer.getEnderChestInventory(), targetItemId, items);
                            if (found > 0) {
                                results.add(new SearchResult(pos, "EnderChestBlockEntity", found, items));
                            }
                        }
                        continue;
                    }

                    if (blockEntity instanceof DecoratedPotBlockEntity) {
                        Map<String, Integer> items = new HashMap<>();
                        int found = countMatchingExact((Container) blockEntity, targetItemId, items);
                        if (found > 0) {
                            results.add(new SearchResult(pos, "decorated_pot", found, items));
                        }
                        continue;
                    }

                    if (blockEntity instanceof Container container) {
                        Map<String, Integer> items = new HashMap<>();
                        int found = countMatchingExact(container, targetItemId, items);
                        if (found > 0) {
                            String blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).getPath();
                            results.add(new SearchResult(pos, blockId, found, items));
                        }
                    }
                }
            }
        }

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
            } else if (entity instanceof MinecartHopper hopper) {
                container = hopper;
                typeName = "MinecartHopper";
            }

            if (container != null && typeName != null) {
                Map<String, Integer> items = new HashMap<>();
                int found = countMatchingExact(container, targetItemId, items);
                if (found > 0) {
                    BlockPos entityPos = entity.blockPosition();
                    results.add(new SearchResult(entityPos, typeName, found, items));
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
                            Map<String, Integer> items = new HashMap<>();
                            int found = countMatchingContains(serverPlayer.getEnderChestInventory(), lowerQuery, items);
                            if (found > 0) results.add(new SearchResult(pos, "EnderChestBlockEntity", found, items));
                        }
                        continue;
                    }

                    if (blockEntity instanceof DecoratedPotBlockEntity) {
                        Map<String, Integer> items = new HashMap<>();
                        int found = countMatchingContains((Container) blockEntity, lowerQuery, items);
                        if (found > 0) results.add(new SearchResult(pos, "decorated_pot", found, items));
                        continue;
                    }

                    if (blockEntity instanceof Container container) {
                        Map<String, Integer> items = new HashMap<>();
                        int found = countMatchingContains(container, lowerQuery, items);
                        if (found > 0) {
                            String blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).getPath();
                            results.add(new SearchResult(pos, blockId, found, items));
                        }
                    }
                }
            }
        }

        AABB searchBox = new AABB(
            center.getX() - radius, center.getY() - radius, center.getZ() - radius,
            center.getX() + radius, center.getY() + radius, center.getZ() + radius
        );
        for (Entity entity : serverLevel.getEntities(null, searchBox)) {
            Container container = null;
            String typeName = null;
            if (entity instanceof MinecartChest m) { container = m; typeName = "MinecartChest"; }
            else if (entity instanceof AbstractChestBoat b) { container = b; typeName = "ChestBoat"; }
            else if (entity instanceof MinecartHopper h) { container = h; typeName = "MinecartHopper"; }
            if (container != null) {
                Map<String, Integer> items = new HashMap<>();
                int found = countMatchingContains(container, lowerQuery, items);
                if (found > 0) results.add(new SearchResult(entity.blockPosition(), typeName, found, items));
            }
        }

        return results;
    }

    private static int countMatchingExact(Container container, String targetItemId, Map<String, Integer> items) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) {
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                if (itemId.equals(targetItemId)) {
                    total += stack.getCount();
                    items.merge(itemId, stack.getCount(), Integer::sum);
                }
            }
        }
        return total;
    }

    private static int countMatchingContains(Container container, String query, Map<String, Integer> items) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) {
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase();
                String displayName = stack.getHoverName().getString().toLowerCase();
                if (itemId.contains(query) || displayName.contains(query)) {
                    total += stack.getCount();
                    items.merge(itemId, stack.getCount(), Integer::sum);
                }
            }
        }
        return total;
    }
}
