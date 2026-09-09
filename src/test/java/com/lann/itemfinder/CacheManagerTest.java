package com.lann.itemfinder;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;


public class CacheManagerTest {

    private static final String SERVER_A = "unittest-fake-server-A";
    private static final String SERVER_B = "unittest-fake-server-B";

    @BeforeAll
    static void beforeAll() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @AfterEach
    void cleanup() {
        CacheManager.onLeaveServer();
        deleteCacheFileFor(SERVER_A);
        deleteCacheFileFor(SERVER_B);
    }

    @Test
    void cacheContainer_thenSearchExact_findsItem() {
        CacheManager.onJoinServer(SERVER_A);
        BlockPos pos = new BlockPos(5, 64, 5);

        CacheManager.cacheContainer(pos, "chest", Map.of("stone", 10));

        List<CacheManager.CacheSearchResult> results =
            CacheManager.searchExact(new BlockPos(0, 64, 0), 50, "stone");

        Assertions.assertEquals(1, results.size(), "Should find 1 container");
        Assertions.assertEquals(pos, results.get(0).pos);
        Assertions.assertEquals(10, results.get(0).count);
    }

    @Test
    void searchExact_doesNotMatchPartialItemId() {
        CacheManager.onJoinServer(SERVER_A);
        CacheManager.cacheContainer(new BlockPos(1, 1, 1), "chest", Map.of("stone_bricks", 5));

        List<CacheManager.CacheSearchResult> results =
            CacheManager.searchExact(new BlockPos(0, 0, 0), 50, "stone");

        Assertions.assertTrue(results.isEmpty(),
            "searchExact('stone') must not match 'stone_bricks' -- that's searchContains's job, not searchExact.");
    }

    @Test
    void searchContains_matchesSubstring() {
        CacheManager.onJoinServer(SERVER_A);
        CacheManager.cacheContainer(new BlockPos(1, 1, 1), "chest", Map.of("stone_bricks", 5));

        List<CacheManager.CacheSearchResult> results =
            CacheManager.searchContains(new BlockPos(0, 0, 0), 50, "stone");

        Assertions.assertEquals(1, results.size(), "searchContains('stone') should match 'stone_bricks'");
    }

    @Test
    void radius_excludesContainerOutsideRange() {
        CacheManager.onJoinServer(SERVER_A);
        BlockPos playerPos = new BlockPos(0, 64, 0);
        BlockPos farPos = new BlockPos(100, 64, 100);
        CacheManager.cacheContainer(farPos, "chest", Map.of("stone", 5));

        List<CacheManager.CacheSearchResult> results = CacheManager.searchExact(playerPos, 10, "stone");

        Assertions.assertTrue(results.isEmpty(),
            "Container far outside radius must not be included in scan.");
    }

    @Test
    void radius_boundaryIsInclusive() {
        CacheManager.onJoinServer(SERVER_A);
        BlockPos playerPos = new BlockPos(0, 64, 0);
        BlockPos edgePos = new BlockPos(10, 64, 0);
        CacheManager.cacheContainer(edgePos, "chest", Map.of("stone", 5));

        List<CacheManager.CacheSearchResult> results = CacheManager.searchExact(playerPos, 10, "stone");

        Assertions.assertEquals(1, results.size(),
            "Container exactly at distance == radius should still be scanned (inclusive).");
    }

    @Test
    void removeContainer_removesFromSearchResults() {
        CacheManager.onJoinServer(SERVER_A);
        BlockPos pos = new BlockPos(1, 1, 1);
        CacheManager.cacheContainer(pos, "chest", Map.of("stone", 5));

        CacheManager.removeContainer(pos);

        List<CacheManager.CacheSearchResult> results =
            CacheManager.searchExact(new BlockPos(0, 0, 0), 50, "stone");
        Assertions.assertTrue(results.isEmpty(), "Container that was removed must not appear again in search results.");
    }

    @Test
    void differentServersHaveIsolatedCaches() {
        CacheManager.onJoinServer(SERVER_A);
        CacheManager.cacheContainer(new BlockPos(1, 1, 1), "chest", Map.of("diamond", 3));
        CacheManager.onLeaveServer();

        CacheManager.onJoinServer(SERVER_B);
        List<CacheManager.CacheSearchResult> resultsOnServerB =
            CacheManager.searchExact(new BlockPos(0, 0, 0), 50, "diamond");

        Assertions.assertTrue(resultsOnServerB.isEmpty(),
            "Cache server A must not 'leak' to server B -- each server must have separate cache (see README.md 'Local Cache').");
    }

    @Test
    void dataPersistsAcrossLeaveAndRejoin() {
        CacheManager.onJoinServer(SERVER_A);
        CacheManager.cacheContainer(new BlockPos(2, 2, 2), "barrel", Map.of("iron_ingot", 7));
        CacheManager.onLeaveServer();

        CacheManager.onJoinServer(SERVER_A);
        List<CacheManager.CacheSearchResult> results =
            CacheManager.searchExact(new BlockPos(0, 0, 0), 50, "iron_ingot");

        Assertions.assertEquals(1, results.size(),
            "Cache data must persist after disconnect & reconnect to same server (cache is persistent, per README.md).");
    }

    @Test
    void isAvailable_reflectsSessionState() {
        Assertions.assertFalse(CacheManager.isAvailable(), "Before joining any server, cache not yet available");

        CacheManager.onJoinServer(SERVER_A);
        Assertions.assertTrue(CacheManager.isAvailable(), "After joining server, cache must be available");

        CacheManager.onLeaveServer();
        Assertions.assertFalse(CacheManager.isAvailable(), "After leaving server, cache no longer available");
    }

    // ── Helper cleanup ───────────────────────────────────────────────────

    private void deleteCacheFileFor(String serverAddress) {
        try {
            Field dirField = CacheManager.class.getDeclaredField("CACHE_DIR");
            dirField.setAccessible(true);
            Path cacheDir = (Path) dirField.get(null);
            String sanitized = serverAddress.replaceAll("[^a-zA-Z0-9._-]", "_");
            Files.deleteIfExists(cacheDir.resolve(sanitized + ".json"));
        } catch (Exception ignored) {
        }
    }
}