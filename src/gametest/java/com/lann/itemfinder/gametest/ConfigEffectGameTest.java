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

public class ConfigEffectGameTest {

    @GameTest
    public void radius_excludesChestOutsideRange(GameTestHelper helper) {
        BlockPos nearRelative = new BlockPos(1, 2, 1);
        helper.setBlock(nearRelative, Blocks.CHEST);
        setStone(helper, nearRelative, 5);

        BlockPos farRelative = new BlockPos(20, 2, 20);
        helper.setBlock(farRelative, Blocks.CHEST);
        setStone(helper, farRelative, 5);

        BlockPos center = helper.absolutePos(BlockPos.ZERO);
        BlockPos nearAbsolute = helper.absolutePos(nearRelative);
        BlockPos farAbsolute = helper.absolutePos(farRelative);

        List<StorageScanner.SearchResult> smallRadiusResults = StorageScanner.scanExact(
            helper.getLevel().getServer(), helper.getLevel(), "dummy-player",
            center, 5, "stone"
        );
        boolean nearFoundSmall = smallRadiusResults.stream().anyMatch(r -> r.pos.equals(nearAbsolute));
        boolean farFoundSmall  = smallRadiusResults.stream().anyMatch(r -> r.pos.equals(farAbsolute));

        if (!nearFoundSmall) {
            helper.fail(Component.literal("Chest DEKAT harusnya ketemu walau radius kecil -- ada bug lain di luar soal radius."));
            return;
        }
        if (farFoundSmall) {
            helper.fail(Component.literal("Chest JAUH ikut ketemu padahal radius kecil (5 blok) -- parameter radius TIDAK membatasi hasil dengan benar. " +
                "Kalau config radius di in-game diubah kecil tapi hasil pencarian tetap mencakup area luas, ini penyebabnya."));
            return;
        }

        List<StorageScanner.SearchResult> bigRadiusResults = StorageScanner.scanExact(
            helper.getLevel().getServer(), helper.getLevel(), "dummy-player",
            center, 25, "stone"
        );
        boolean nearFoundBig = bigRadiusResults.stream().anyMatch(r -> r.pos.equals(nearAbsolute));
        boolean farFoundBig  = bigRadiusResults.stream().anyMatch(r -> r.pos.equals(farAbsolute));

        if (!nearFoundBig || !farFoundBig) {
            helper.fail(Component.literal("Dengan radius besar (25 blok), kedua chest harusnya ketemu semua. " +
                "nearFound=" + nearFoundBig + ", farFound=" + farFoundBig));
            return;
        }

        helper.succeed();
    }

    @GameTest
    public void radius_boundaryIsInclusive(GameTestHelper helper) {
        int radius = 10;
        BlockPos edgeRelative = new BlockPos(radius, 2, 0);
        helper.setBlock(edgeRelative, Blocks.BARREL);
        setStone(helper, edgeRelative, 3);

        BlockPos center = helper.absolutePos(BlockPos.ZERO);
        BlockPos edgeAbsolute = helper.absolutePos(edgeRelative);

        List<StorageScanner.SearchResult> results = StorageScanner.scanExact(
            helper.getLevel().getServer(), helper.getLevel(), "dummy-player",
            center, radius, "stone"
        );

        boolean found = results.stream().anyMatch(r -> r.pos.equals(edgeAbsolute));
        if (!found) {
            helper.fail(Component.literal("Barrel tepat di jarak == radius (" + radius + " blok) harusnya tetap ke-scan (inclusive), tapi tidak ketemu."));
        } else {
            helper.succeed();
        }
    }

    private void setStone(GameTestHelper helper, BlockPos relativePos, int count) {
        BlockEntity be = helper.getBlockEntity(relativePos, BlockEntity.class);
        if (be instanceof Container container) {
            container.setItem(0, new net.minecraft.world.item.ItemStack(Items.STONE, count));
        }
    }
}