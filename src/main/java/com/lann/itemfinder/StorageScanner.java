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

    /**
     * Full entity type id (e.g. {@code minecraft:oak_chest_boat}), so the client
     * can resolve the translated name via {@code EntityType.getDescription()}.
     */
    static String entityId(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }

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

        /**
         * Legacy string form, kept for logs only. Player-visible text must go
         * through {@link ResultFormatter} (client side) so it is translated.
         */
        public String format() {
            return ContainerDisplayNames.displayNameForId(containerType) +
                   " at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() +
                   " → " + count + " item";
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
                                results.add(new SearchResult(pos, "minecraft:ender_chest", found, items));
                            }
                        }
                        continue;
                    }

                    if (blockEntity instanceof DecoratedPotBlockEntity) {
                        Map<String, Integer> items = new HashMap<>();
                        int found = countMatchingExact((Container) blockEntity, targetItemId, items);
                        if (found > 0) {
                            results.add(new SearchResult(pos, "minecraft:decorated_pot", found, items));
                        }
                        continue;
                    }

                    if (blockEntity instanceof Container container) {
                        Map<String, Integer> items = new HashMap<>();
                        int found = countMatchingExact(container, targetItemId, items);
                        if (found > 0) {
                            String blockId = ContainerDisplayNames.blockIdFull(blockEntity);
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
                typeName = "minecraft:chest_minecart";
            } else if (entity instanceof AbstractChestBoat chestBoat) {
                container = chestBoat;
                typeName = entityId(chestBoat);
            } else if (entity instanceof MinecartHopper hopper) {
                container = hopper;
                typeName = "minecraft:hopper_minecart";
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
                            if (found > 0) results.add(new SearchResult(pos, "minecraft:ender_chest", found, items));
                        }
                        continue;
                    }

                    if (blockEntity instanceof DecoratedPotBlockEntity) {
                        Map<String, Integer> items = new HashMap<>();
                        int found = countMatchingContains((Container) blockEntity, lowerQuery, items);
                        if (found > 0) results.add(new SearchResult(pos, "minecraft:decorated_pot", found, items));
                        continue;
                    }

                    if (blockEntity instanceof Container container) {
                        Map<String, Integer> items = new HashMap<>();
                        int found = countMatchingContains(container, lowerQuery, items);
                        if (found > 0) {
                            String blockId = ContainerDisplayNames.blockIdFull(blockEntity);
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
            if (entity instanceof MinecartChest m) { container = m; typeName = "minecraft:chest_minecart"; }
            else if (entity instanceof AbstractChestBoat b) { container = b; typeName = entityId(b); }
            else if (entity instanceof MinecartHopper h) { container = h; typeName = "minecraft:hopper_minecart"; }
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
