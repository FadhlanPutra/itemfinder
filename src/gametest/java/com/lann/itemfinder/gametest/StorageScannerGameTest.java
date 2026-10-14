package com.lann.itemfinder.gametest;


import com.lann.itemfinder.StorageScanner;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class StorageScannerGameTest {

    private static final BlockPos CHEST_POS = new BlockPos(1, 2, 1);

    @GameTest
    public void scanExact_findsChestWithMatchingItem(GameTestHelper helper) {
        helper.setBlock(CHEST_POS, Blocks.CHEST);

        BlockEntity be = helper.getBlockEntity(CHEST_POS, BlockEntity.class);
        if (!(be instanceof Container container)) {
            helper.fail(Component.literal("Chest does not produce a BlockEntity of type Container — " +
                "the block entity name/class may have changed in this Minecraft version."));
            return;
        }
        container.setItem(0, new net.minecraft.world.item.ItemStack(Items.STONE, 5));

        BlockPos absoluteChestPos = helper.absolutePos(CHEST_POS);
        BlockPos center = helper.absolutePos(BlockPos.ZERO);

        List<StorageScanner.SearchResult> results = StorageScanner.scanExact(
            helper.getLevel().getServer(),
            helper.getLevel(),
            "dummy-player",
            center,
            10,
            "stone"
        );

        boolean found = results.stream().anyMatch(r ->
            r.pos.equals(absoluteChestPos) && r.count == 5);

        if (!found) {
            helper.fail(Component.literal("scanExact('stone') did NOT find chest that should have 5 stone. " +
                "Result: " + results.size() + " containers found, none match position/count. " +
                "This indicates a silent bug in StorageScanner or API changes in BuiltInRegistries/Container."));
        } else {
            helper.succeed();
        }
    }

    @GameTest
    public void scanContains_findsPartialMatch(GameTestHelper helper) {
        helper.setBlock(CHEST_POS, Blocks.BARREL);

        BlockEntity be = helper.getBlockEntity(CHEST_POS, BlockEntity.class);
        if (!(be instanceof Container container)) {
            helper.fail(Component.literal("Barrel does not produce a BlockEntity of type Container."));
            return;
        }
        container.setItem(0, new net.minecraft.world.item.ItemStack(Items.STONE, 3));

        BlockPos center = helper.absolutePos(BlockPos.ZERO);

        List<StorageScanner.SearchResult> results = StorageScanner.scanContains(
            helper.getLevel().getServer(),
            helper.getLevel(),
            "dummy-player",
            center,
            10,
            "sto"
        );

        if (results.isEmpty()) {
            helper.fail(Component.literal("scanContains('sto') did not find barrel containing stone — " +
                "check countMatchingContains() in StorageScanner, or API BuiltInRegistries.ITEM.getKey()."));
        } else {
            helper.succeed();
        }
    }
}