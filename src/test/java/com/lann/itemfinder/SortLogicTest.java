package com.lann.itemfinder;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;


public class SortLogicTest {

    @BeforeAll
    static void beforeAll() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    private static final BlockPos PLAYER_POS = new BlockPos(0, 64, 0);

    @Test
    void sortMode0_ordersByNearestFirst() {
        List<StorageScanner.SearchResult> results = new ArrayList<>(List.of(
            new StorageScanner.SearchResult(new BlockPos(50, 64, 0), "chest", 1),
            new StorageScanner.SearchResult(new BlockPos(5, 64, 0),  "chest", 99),
            new StorageScanner.SearchResult(new BlockPos(20, 64, 0), "chest", 50)
        ));

        ServerNetworkHandler.sortResults(results, PLAYER_POS, 0);

        Assertions.assertEquals(new BlockPos(5, 64, 0), results.get(0).pos, "First entry should be the CLOSEST");
        Assertions.assertEquals(new BlockPos(20, 64, 0), results.get(1).pos, "Second entry should be at medium distance");
        Assertions.assertEquals(new BlockPos(50, 64, 0), results.get(2).pos, "Last entry should be the FARTHEST");
    }

    @Test
    void sortMode1_ordersByMostItemsFirst() {
        List<StorageScanner.SearchResult> results = new ArrayList<>(List.of(
            new StorageScanner.SearchResult(new BlockPos(5, 64, 0),  "chest", 1),
            new StorageScanner.SearchResult(new BlockPos(50, 64, 0), "chest", 99),
            new StorageScanner.SearchResult(new BlockPos(20, 64, 0), "chest", 50)
        ));

        ServerNetworkHandler.sortResults(results, PLAYER_POS, 1);

        Assertions.assertEquals(99, results.get(0).count, "First entry should have the MOST items");
        Assertions.assertEquals(50, results.get(1).count, "Second entry should have medium count");
        Assertions.assertEquals(1, results.get(2).count, "Last entry should have the FEWEST items");
    }

    @Test
    void emptyResultsDoesNotCrash() {
        List<StorageScanner.SearchResult> results = new ArrayList<>();
        Assertions.assertDoesNotThrow(() -> ServerNetworkHandler.sortResults(results, PLAYER_POS, 0));
        Assertions.assertDoesNotThrow(() -> ServerNetworkHandler.sortResults(results, PLAYER_POS, 1));
    }

    @Test
    void mainEnterKeyPicksFirstMatchingItem() {
        KeyEvent enter = new KeyEvent(InputConstants.KEY_RETURN, InputConstants.KEYCODE_RETURN, 0);

        SearchScreen.EnterAction action = SearchScreen.decideEnterAction(
            enter, 1, true, 2, false, false
        );

        Assertions.assertEquals(SearchScreen.EnterAction.PICK_FIRST, action);
    }

    @Test
    void numpadEnterScansAllMatches() {
        KeyEvent numpadEnter = new KeyEvent(
            InputConstants.KEY_NUMPADENTER, InputConstants.KEYCODE_NUMPADENTER, 0
        );

        SearchScreen.EnterAction action = SearchScreen.decideEnterAction(
            numpadEnter, 2, true, 2, false, false
        );

        Assertions.assertEquals(SearchScreen.EnterAction.SCAN_ALL, action);
    }

    @Test
    void nonConfirmationKeyDoesNotTriggerEnterAction() {
        KeyEvent y = new KeyEvent(InputConstants.KEY_Y, InputConstants.KEYCODE_Y, 0);

        SearchScreen.EnterAction action = SearchScreen.decideEnterAction(
            y, 2, true, 2, false, false
        );

        Assertions.assertEquals(SearchScreen.EnterAction.NONE, action);
    }
}