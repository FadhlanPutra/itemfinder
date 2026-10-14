package com.lann.itemfinder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.ChatFormatting;

/**
 * Client-side formatting of search results as translatable components.
 * All player-visible result text goes through here so it follows the
 * player's game language. Lives in the client source set (needs the
 * client's language data).
 */
public final class ResultFormatter {
    private ResultFormatter() {}

    /** Container name as a component that re-translates when the game language changes. */
    public static Component containerName(String containerId) {
        String id = normalize(containerId);
        Component resolved = resolveBlockName(id);
        if (resolved != null) return resolved;
        resolved = resolveEntityName(id);
        if (resolved != null) return resolved;
        resolved = resolveItemName(id);
        if (resolved != null) return resolved;
        return Component.literal(ContainerDisplayNames.titleCase(ContainerDisplayNames.normalizeLegacy(id)));
    }

    /** Item name as a component that re-translates when the game language changes. */
    public static Component itemName(String itemId) {
        String id = itemId.contains(":") ? itemId : "minecraft:" + itemId;
        try {
            var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
            if (item != null) return item.getDefaultInstance().getHoverName();
        } catch (Exception ignored) {
        }
        return Component.literal(ContainerDisplayNames.titleCase(itemId.replace('_', ' ')));
    }

    /**
     * One result line: container → location → matched item(s) → distance.
     * Single matching kind: {@code Chest at 752, 101, 86 → 1× Oak Planks (3m)}.
     * Multiple kinds: {@code Shulker Box at 748, 101, 86 → 3 jenis item (5m)}.
     */
    public static Component resultLine(StorageScanner.SearchResult r, BlockPos playerPos) {
        int dist = (int) Math.sqrt(playerPos.distSqr(r.pos));
        String coords = r.pos.getX() + ", " + r.pos.getY() + ", " + r.pos.getZ();
        Component itemsPart;
        if (r.items == null || r.items.size() <= 1) {
            String itemId = firstItemId(r);
            Component itemComp = itemId != null ? itemName(itemId) : itemName("unknown");
            long total = itemId != null ? totalCount(r) : r.count;
            itemsPart = Component.translatable(itemKey(total), total, itemComp);
        } else {
            int kinds = r.items.size();
            itemsPart = Component.translatable(kinds == 1
                ? "gui.itemfinder.result.kinds.one" : "gui.itemfinder.result.kinds.other", kinds);
        }
        return Component.translatable("gui.itemfinder.result.line",
            containerName(r.containerType).copy().withStyle(ChatFormatting.YELLOW),
            Component.translatable("gui.itemfinder.result.at").copy().withStyle(ChatFormatting.WHITE),
            Component.literal(coords).withStyle(ChatFormatting.AQUA),
            itemsPart.copy().withStyle(ChatFormatting.GREEN),
            Component.translatable("gui.itemfinder.result.distance", dist).copy().withStyle(ChatFormatting.GRAY));
    }

    /** HUD line for one highlighted container (name + distance). */
    public static Component hudLine(String containerId, int dist) {
        return Component.translatable("gui.itemfinder.hud.line",
            containerName(containerId).copy().withStyle(ChatFormatting.YELLOW),
            Component.translatable("gui.itemfinder.result.distance", dist).copy().withStyle(ChatFormatting.GRAY));
    }

    /** Bottom summary: Found "oak" in 6 containers (query = text the player typed). */
    public static Component summary(String query, int containers) {
        return Component.translatable("gui.itemfinder.summary",
            Component.literal(query).withStyle(ChatFormatting.WHITE),
            Component.literal(String.valueOf(containers)).withStyle(ChatFormatting.WHITE),
            Component.translatable(containers == 1
                ? "gui.itemfinder.container.one" : "gui.itemfinder.container.other").copy().withStyle(ChatFormatting.GREEN));
    }

    /** Chat header above the result lines. */
    public static Component chatHeader(String label, boolean fromCache) {
        if (fromCache) {
            return Component.translatable("gui.itemfinder.chat.header.cache",
                Component.literal(label).withStyle(ChatFormatting.WHITE));
        }
        return Component.translatable("gui.itemfinder.chat.header",
            Component.literal(label).withStyle(ChatFormatting.WHITE));
    }

    /** "not found" status line for GUI. */
    public static Component notFound(String label, int radius, boolean fromCache) {
        if (fromCache) {
            return Component.translatable("gui.itemfinder.not_found_cache",
                Component.literal(label).withStyle(ChatFormatting.WHITE),
                Component.literal(String.valueOf(radius)).withStyle(ChatFormatting.WHITE));
        }
        return Component.translatable("gui.itemfinder.not_found_full",
            Component.literal(label).withStyle(ChatFormatting.WHITE),
            Component.literal(String.valueOf(radius)).withStyle(ChatFormatting.WHITE));
    }

    private static String firstItemId(StorageScanner.SearchResult r) {
        if (r.items == null || r.items.isEmpty()) return null;
        return r.items.keySet().iterator().next();
    }

    private static long totalCount(StorageScanner.SearchResult r) {
        long total = 0;
        for (int c : r.items.values()) total += c;
        return total;
    }

    private static String itemKey(long total) {
        return total == 1 ? "gui.itemfinder.result.item.one" : "gui.itemfinder.result.item.other";
    }

    private static String normalize(String id) {
        if (id == null || id.isEmpty()) return "minecraft:unknown";
        String base = ContainerDisplayNames.normalizeLegacy(id);
        return base.contains(":") ? base : "minecraft:" + base;
    }

    private static Component resolveBlockName(String id) {
        try {
            var block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
            if (block == null) return null;
            return block.getName();
        } catch (Exception e) {
            return null;
        }
    }

    private static Component resolveEntityName(String id) {
        try {
            var type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(id));
            if (type == null) return null;
            return type.getDescription();
        } catch (Exception e) {
            return null;
        }
    }

    private static Component resolveItemName(String id) {
        try {
            var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
            if (item == null) return null;
            return item.getDefaultInstance().getHoverName();
        } catch (Exception e) {
            return null;
        }
    }
}