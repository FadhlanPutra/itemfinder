package com.lann.itemfinder;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


public class SearchResultFriendlyNameTest {

    @BeforeAll
    static void beforeAll() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    void knownBlockContainerTypesGetProperNames() {
        assertFriendlyContains("chest", "Chest");
        assertFriendlyContains("trapped_chest", "Trapped Chest");
        assertFriendlyContains("barrel", "Barrel");
        assertFriendlyContains("shulker_box", "Shulker Box");
        assertFriendlyContains("white_shulker_box", "Shulker Box");
        assertFriendlyContains("furnace", "Furnace");
        assertFriendlyContains("blast_furnace", "Blast Furnace");
        assertFriendlyContains("smoker", "Smoker");
        assertFriendlyContains("hopper", "Hopper");
        assertFriendlyContains("dispenser", "Dispenser");
        assertFriendlyContains("dropper", "Dropper");
    }

    @Test
    void specialEntityAndEnderChestTypesGetProperNames() {
        assertFriendlyContains("EnderChestBlockEntity", "Ender Chest");
        assertFriendlyContains("MinecartChest", "Minecart Chest");
        assertFriendlyContains("ChestBoat", "Chest Boat");
    }

    @Test
    void unknownContainerTypeNeverLeaksRawIdOrClassName() {
        String result = formatWithType("chiseled_bookshelf");

        Assertions.assertFalse(result.contains("class_"),
            "format() result leaks raw class name (class_XXXX) -- indicates bug similar to previous case.");
        Assertions.assertFalse(result.contains("_"),
            "format() result still contains raw underscore from registry ID, should already be title-cased to normal words.");
    }

    private void assertFriendlyContains(String containerType, String expectedFriendlyName) {
        String result = formatWithType(containerType);
        Assertions.assertTrue(result.contains(expectedFriendlyName),
            "containerType '" + containerType + "' should produce name '" + expectedFriendlyName +
            "', but format() result: " + result);
    }

    private String formatWithType(String containerType) {
        StorageScanner.SearchResult result = new StorageScanner.SearchResult(
            new BlockPos(0, 0, 0), containerType, 1
        );
        return result.format();
    }
}