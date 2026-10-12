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
    void knownClassStyleContainerTypesGetProperNames() {
        assertFriendlyContains("ChestBlockEntity", "Chest");
        assertFriendlyContains("TrappedChestBlockEntity", "Trapped Chest");
        assertFriendlyContains("BarrelBlockEntity", "Barrel");
        assertFriendlyContains("ShulkerBoxBlockEntity", "Shulker Box");
        assertFriendlyContains("FurnaceBlockEntity", "Furnace");
        assertFriendlyContains("BlastFurnaceBlockEntity", "Blast Furnace");
        assertFriendlyContains("SmokerBlockEntity", "Smoker");
        assertFriendlyContains("HopperBlockEntity", "Hopper");
        assertFriendlyContains("DispenserBlockEntity", "Dispenser");
        assertFriendlyContains("DropperBlockEntity", "Dropper");
    }

    @Test
    void specialEntityAndEnderChestTypesGetProperNames() {
        assertFriendlyContains("EnderChestBlockEntity", "Ender Chest");
        assertFriendlyContains("MinecartChest", "Minecart Chest");
        assertFriendlyContains("ChestBoat", "Chest Boat");
    }

    @Test
    void formatMatchesOriginal1215Layout() {
        StorageScanner.SearchResult result = new StorageScanner.SearchResult(
            new BlockPos(752, 101, 86), "ChestBlockEntity", 1
        );
        String formatted = result.format();
        Assertions.assertTrue(formatted.contains("Chest"),
            "should show friendly container name, got: " + formatted);
        Assertions.assertTrue(formatted.contains("752, 101, 86"),
            "should show coordinates, got: " + formatted);
        Assertions.assertTrue(formatted.contains("1 item"),
            "should show count, got: " + formatted);
        Assertions.assertFalse(formatted.contains("class_"),
            "should not leak intermediary class names, got: " + formatted);
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