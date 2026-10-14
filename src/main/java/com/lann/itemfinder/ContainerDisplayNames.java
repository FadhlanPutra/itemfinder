package com.lann.itemfinder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Single helper for player-visible container names.
 *
 * <p>Name resolution happens on the client via the block/entity registries,
 * so each player sees the name in their own game language. The wire/cache
 * format stores a registry id (e.g. {@code minecraft:chest}), never a
 * Java class name.</p>
 *
 * <p>Direct-object overloads (for live world objects):</p>
 * <ul>
 *   <li>BlockEntity: {@code be.getBlockState().getBlock().getName().getString()}
 *       (Mojmap name for the {@code getCachedState()} pattern from the brief).</li>
 *   <li>Entity: {@code e.getType().getDescription().getString()}
 *       (Mojmap name for the {@code getType().getName()} pattern from the brief;
 *       {@code getName()} would return a custom name when one is set).</li>
 * </ul>
 */
public final class ContainerDisplayNames {
    private ContainerDisplayNames() {}

    public static String displayName(BlockEntity be) {
        if (be == null) return "?";
        return be.getBlockState().getBlock().getName().getString();
    }

    public static String displayName(Entity e) {
        if (e == null) return "?";
        return e.getType().getDescription().getString();
    }

    /** Registry path of the block backing this block entity (e.g. {@code chest}), for cache storage. */
    public static String blockId(BlockEntity be) {
        if (be == null) return "?";
        return BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock()).getPath();
    }

    /** Full registry id ({@code minecraft:chest}) of the block backing this block entity. */
    public static String blockIdFull(BlockEntity be) {
        if (be == null) return "minecraft:unknown";
        return BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock()).toString();
    }

    /**
     * Resolve a stored container id to its translated name in the client's
     * current language. Accepts full ({@code minecraft:chest}) or bare
     * ({@code chest}) ids; unknown ids fall back to a title-cased form
     * (never a raw class name or raw underscore id). Legacy cache entries
     * holding class-like names (e.g. {@code ChestBlockEntity}) are mapped
     * to the matching registry id first so old caches keep working.
     */
    public static String displayNameForId(String containerId) {
        if (containerId == null || containerId.isEmpty()) return "?";
        String name = resolveBlockName(containerId);
        if (name != null) return name;
        name = resolveEntityName(containerId);
        if (name != null) return name;
        name = resolveItemName(containerId);
        if (name != null) return name;
        return titleCase(normalizeLegacy(containerId));
    }

    private static String resolveBlockName(String id) {
        try {
            var block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(normalize(id)));
            if (block == null) return null;
            return block.getName().getString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String resolveEntityName(String id) {
        try {
            var type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(normalize(id)));
            if (type == null) return null;
            return type.getDescription().getString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String resolveItemName(String id) {
        try {
            var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(normalize(id)));
            if (item == null) return null;
            return item.getDefaultInstance().getHoverName().getString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalize(String id) {
        String base = normalizeLegacy(id);
        return base.contains(":") ? base : "minecraft:" + base;
    }

    /**
     * Map legacy class-like cache values to registry paths (no crash on old caches):
     * {@code ChestBlockEntity} / {@code class_2595} -> {@code chest}, etc.
     */
    static String normalizeLegacy(String id) {
        return switch (id) {
            case "EnderChestBlockEntity" -> "ender_chest";
            case "MinecartChest" -> "chest_minecart";
            case "MinecartHopper" -> "hopper_minecart";
            case "ChestBoat" -> "oak_chest_boat";
            case "ChestBlockEntity", "class_2595" -> "chest";
            default -> stripNamespace(id);
        };
    }

    private static String stripNamespace(String id) {
        int i = id.indexOf(':');
        return i >= 0 ? id.substring(i + 1) : id;
    }

    static String titleCase(String s) {
        String[] parts = s.replace('_', ' ').split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
