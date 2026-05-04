package com.lann.itemfinder;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class CacheManager {

    // ── Data structures ────────────────────────────────────────────────────

    public static class CachedContainer {
        public String type;
        public long lastOpened; // epoch millis
        public Map<String, Integer> items = new HashMap<>(); // itemId -> count

        public CachedContainer(String type) {
            this.type = type;
            this.lastOpened = System.currentTimeMillis();
        }

        public String getAgeString() {
            long now = System.currentTimeMillis();
            long diffMs = now - lastOpened;
            long mins  = diffMs / 60000;
            long hours = mins / 60;
            long days  = hours / 24;

            if (days > 0)  return days  + "d ago";
            if (hours > 0) return hours + "h ago";
            if (mins > 0)  return mins  + "m ago";
            return "just now";
        }
    }

    public static class ServerCache {
        public String serverAddress;
        public Map<String, Map<String, CachedContainer>> dimensions = new HashMap<>();
        // dimensions -> koordinat "x,y,z" -> container

        public ServerCache(String serverAddress) {
            this.serverAddress = serverAddress;
        }

        public void putContainer(String dimension, BlockPos pos, CachedContainer container) {
            dimensions
                .computeIfAbsent(dimension, k -> new HashMap<>())
                .put(posKey(pos), container);
        }

        public CachedContainer getContainer(String dimension, BlockPos pos) {
            var dim = dimensions.get(dimension);
            if (dim == null) return null;
            return dim.get(posKey(pos));
        }

        public Map<String, CachedContainer> getDimension(String dimension) {
            return dimensions.getOrDefault(dimension, new HashMap<>());
        }

        private String posKey(BlockPos pos) {
            return pos.getX() + "," + pos.getY() + "," + pos.getZ();
        }
    }

    // ── State ───────────────────────────────────────────────────────────────

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CACHE_DIR = FabricLoader.getInstance()
        .getGameDir().resolve("itemfinder_cache");

    private static ServerCache currentCache = null;
    private static String currentServerKey  = null;
    private static String currentDimension  = "minecraft:overworld";

    // ── Session management ─────────────────────────────────────────────────

    public static void onJoinServer(String serverAddress) {
        currentServerKey = sanitizeKey(serverAddress);
        currentDimension = "minecraft:overworld";
        currentCache = load(currentServerKey);
        ItemFinderMod.LOGGER.info("[ItemFinder] Cache loaded for: " + serverAddress
            + " (" + getTotalContainers() + " containers)");
    }

    public static void onChangeDimension(String dimension) {
        currentDimension = dimension;
    }

    public static void onLeaveServer() {
        save();
        currentCache = null;
        currentServerKey = null;
    }

    // ── Cache a container ──────────────────────────────────────────────────

    public static void cacheContainer(BlockPos pos, String containerType,
                                       Map<String, Integer> items) {
        if (currentCache == null) return;
        CachedContainer cc = new CachedContainer(containerType);
        cc.items = new HashMap<>(items);
        currentCache.putContainer(currentDimension, pos, cc);
        save(); // auto-save setiap update
    }

    // ── Remove container dari cache (kalau dihancurkan) ────────────────────

    public static void removeContainer(BlockPos pos) {
        if (currentCache == null) return;
        var dim = currentCache.dimensions.get(currentDimension);
        if (dim != null) {
            dim.remove(posKey(pos));
            save();
        }
    }

    // ── Search dari cache ──────────────────────────────────────────────────

    public static List<CacheSearchResult> searchExact(BlockPos playerPos, int radius, String targetItemId) {
        return search(playerPos, radius, (itemId, displayId) -> itemId.equals(targetItemId));
    }

    public static List<CacheSearchResult> searchContains(BlockPos playerPos, int radius, String query) {
        String lq = query.toLowerCase();
        return search(playerPos, radius, (itemId, displayId) -> itemId.contains(lq));
    }

    private static List<CacheSearchResult> search(BlockPos playerPos, int radius,
                                                    java.util.function.BiPredicate<String, String> matcher) {
        List<CacheSearchResult> results = new ArrayList<>();
        if (currentCache == null) return results;

        var dim = currentCache.getDimension(currentDimension);
        for (Map.Entry<String, CachedContainer> entry : dim.entrySet()) {
            BlockPos pos = parsePos(entry.getKey());
            if (pos == null) continue;

            // Cek dalam radius
            double dist = Math.sqrt(playerPos.distSqr(pos));
            if (dist > radius) continue;

            CachedContainer cc = entry.getValue();
            int totalFound = 0;
            for (Map.Entry<String, Integer> item : cc.items.entrySet()) {
                if (matcher.test(item.getKey(), item.getKey())) {
                    totalFound += item.getValue();
                }
            }
            if (totalFound > 0) {
                results.add(new CacheSearchResult(pos, cc.type, totalFound, cc.getAgeString()));
            }
        }

        return results;
    }

    // ── Result type (includes age info) ───────────────────────────────────

    public static class CacheSearchResult {
        public final BlockPos pos;
        public final String containerType;
        public final int count;
        public final String ageString;

        public CacheSearchResult(BlockPos pos, String containerType, int count, String ageString) {
            this.pos = pos;
            this.containerType = containerType;
            this.count = count;
            this.ageString = ageString;
        }

        public StorageScanner.SearchResult toSearchResult() {
            return new StorageScanner.SearchResult(pos, containerType, count);
        }

        public String format() {
            return toSearchResult().format() + " §8(" + ageString + ")";
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    public static boolean isAvailable() {
        return currentCache != null;
    }

    public static int getTotalContainers() {
        if (currentCache == null) return 0;
        return currentCache.dimensions.values().stream()
            .mapToInt(Map::size).sum();
    }

    private static String sanitizeKey(String address) {
        return address.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String posKey(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static BlockPos parsePos(String key) {
        try {
            String[] parts = key.split(",");
            return new BlockPos(Integer.parseInt(parts[0]),
                                Integer.parseInt(parts[1]),
                                Integer.parseInt(parts[2]));
        } catch (Exception e) {
            return null;
        }
    }

    // ── Disk I/O ───────────────────────────────────────────────────────────

    private static ServerCache load(String key) {
        try {
            Files.createDirectories(CACHE_DIR);
            Path file = CACHE_DIR.resolve(key + ".json");
            if (!Files.exists(file)) return new ServerCache(key);
            try (Reader r = new FileReader(file.toFile())) {
                ServerCache cache = GSON.fromJson(r, ServerCache.class);
                return cache != null ? cache : new ServerCache(key);
            }
        } catch (Exception e) {
            ItemFinderMod.LOGGER.warn("[ItemFinder] Failed to load cache: " + e.getMessage());
            return new ServerCache(key);
        }
    }

    private static void save() {
        if (currentCache == null || currentServerKey == null) return;
        try {
            Files.createDirectories(CACHE_DIR);
            Path file = CACHE_DIR.resolve(currentServerKey + ".json");
            try (Writer w = new FileWriter(file.toFile())) {
                GSON.toJson(currentCache, w);
            }
        } catch (Exception e) {
            ItemFinderMod.LOGGER.warn("[ItemFinder] Failed to save cache: " + e.getMessage());
        }
    }
}