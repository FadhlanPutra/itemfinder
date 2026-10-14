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
    void specialEntityAndEnderChestTypesGetProperNames() {
        // Legacy class names should map to readable names without leaking raw class names or underscores
        assertReadableName("EnderChestBlockEntity");
        assertReadableName("MinecartChest");
        assertReadableName("ChestBoat");
        assertReadableName("ChestBlockEntity");
        assertReadableName("class_2595");
    }

    @Test
    void knownBlockContainerTypesGetProperNames() {
        assertReadableName("chest");
        assertReadableName("trapped_chest");
        assertReadableName("barrel");
        assertReadableName("shulker_box");
        assertReadableName("white_shulker_box");
        assertReadableName("furnace");
        assertReadableName("blast_furnace");
        assertReadableName("smoker");
        assertReadableName("hopper");
        assertReadableName("dispenser");
        assertReadableName("dropper");
    }

    @Test
    void unknownContainerTypeNeverLeaksRawIdOrClassName() {
        String result = formatWithType("chiseled_bookshelf");

        Assertions.assertFalse(result.contains("class_"),
            "format() result leaks raw class name (class_XXXX) -- indicates bug similar to previous case.");
        Assertions.assertFalse(result.contains("_"),
            "format() result still contains raw underscore from registry ID, should already be title-cased to normal words.");
    }

    private void assertReadableName(String containerType) {
        String result = formatWithType(containerType);
        Assertions.assertFalse(result.contains("class_"),
            "containerType '" + containerType + "' leaks raw class name: " + result);
        Assertions.assertFalse(result.contains("_"),
            "containerType '" + containerType + "' leaks raw underscore: " + result);
        Assertions.assertTrue(result.length() > 0,
            "containerType '" + containerType + "' produces empty result");
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