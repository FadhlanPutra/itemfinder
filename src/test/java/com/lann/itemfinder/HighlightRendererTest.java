package com.lann.itemfinder;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

public class HighlightRendererTest {

    @BeforeAll
    static void beforeAll() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @AfterEach
    void cleanup() {
        HighlightRenderer.clearHighlights();
    }

    @Test
    void setHighlights_usesConfiguredDurationInSeconds() {
        ConfigManager.get().highlightDurationSeconds = 5;
        List<StorageScanner.SearchResult> results = List.of(
            new StorageScanner.SearchResult(new BlockPos(0, 0, 0), "chest", 1)
        );

        long before = System.currentTimeMillis();
        HighlightRenderer.setHighlights(results);
        long after = System.currentTimeMillis();

        long expectedMin = before + 5000;
        long expectedMax = after + 5000;

        Assertions.assertTrue(
            HighlightRenderer.highlightUntil >= expectedMin && HighlightRenderer.highlightUntil <= expectedMax,
            "highlightUntil should be approximately 'now + highlightDurationSeconds*1000'. " +
            "If Highlight Duration config is changed but highlight duration in-game does not change, check this line in HighlightRenderer.setHighlights()."
        );
    }

    @Test
    void differentDurationsProduceProportionalExpiry() {
        List<StorageScanner.SearchResult> results = List.of(
            new StorageScanner.SearchResult(new BlockPos(0, 0, 0), "chest", 1)
        );

        ConfigManager.get().highlightDurationSeconds = 3;
        HighlightRenderer.setHighlights(results);
        long shortExpiry = HighlightRenderer.highlightUntil;

        ConfigManager.get().highlightDurationSeconds = 30;
        HighlightRenderer.setHighlights(results);
        long longExpiry = HighlightRenderer.highlightUntil;

        Assertions.assertTrue(longExpiry > shortExpiry,
            "Duration 30s should produce a longer expiry time than 3s duration -- if not, Highlight Duration config is not actually taking effect.");
    }

    @Test
    void isActive_reflectsHighlightUntil() {
        HighlightRenderer.highlightUntil = System.currentTimeMillis() + 10_000;
        Assertions.assertTrue(HighlightRenderer.isActive(), "Should still be active because highlightUntil is in the future");

        HighlightRenderer.highlightUntil = System.currentTimeMillis() - 1_000;
        Assertions.assertFalse(HighlightRenderer.isActive(), "Should no longer be active because highlightUntil is in the past");
    }

    @Test
    void clearHighlights_resetsAllState() {
        HighlightRenderer.highlightedPositions.put(new BlockPos(1, 1, 1), "chest (1)");
        HighlightRenderer.highlightUntil = System.currentTimeMillis() + 10_000;

        HighlightRenderer.clearHighlights();

        Assertions.assertTrue(HighlightRenderer.highlightedPositions.isEmpty(),
            "highlightedPositions should be empty after clearHighlights()");
        Assertions.assertEquals(0, HighlightRenderer.highlightUntil,
            "highlightUntil should be reset to 0 after clearHighlights()");
    }
}