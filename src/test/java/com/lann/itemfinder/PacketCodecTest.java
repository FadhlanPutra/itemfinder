package com.lann.itemfinder;


import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import io.netty.buffer.Unpooled;

public class PacketCodecTest {

    @BeforeAll
    static void beforeAll() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    void searchPacket_roundTrip_preservesItemId() {
        SearchPacket original = new SearchPacket("*stone_bricks");

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        SearchPacket.CODEC.encode(buf, original);
        SearchPacket decoded = SearchPacket.CODEC.decode(buf);

        Assertions.assertEquals(original.itemId, decoded.itemId,
            "itemId changed after encode->decode. Check order/format of " +
            "buf.writeUtf/readUtf in SearchPacket.CODEC.");
    }

    @Test
    void searchResultPacket_roundTrip_preservesAllFields() {
        List<StorageScanner.SearchResult> original = List.of(
            new StorageScanner.SearchResult(new BlockPos(10, 64, -5), "ChestBlockEntity", 12),
            new StorageScanner.SearchResult(new BlockPos(-3, 70, 100), "BarrelBlockEntity", 3)
        );
        SearchResultPacket originalPacket = new SearchResultPacket(original);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        SearchResultPacket.CODEC.encode(buf, originalPacket);
        SearchResultPacket decoded = SearchResultPacket.CODEC.decode(buf);

        Assertions.assertEquals(original.size(), decoded.results.size(),
            "Result count changed after encode->decode — likely " +
            "order of writeInt(size) is out of sync with read loop in CODEC.");

        for (int i = 0; i < original.size(); i++) {
            StorageScanner.SearchResult exp = original.get(i);
            StorageScanner.SearchResult act = decoded.results.get(i);
            Assertions.assertEquals(exp.pos, act.pos, "BlockPos at result index " + i + " changed.");
            Assertions.assertEquals(exp.containerType, act.containerType, "containerType at result index " + i + " changed.");
            Assertions.assertEquals(exp.count, act.count, "count at result index " + i + " changed.");
        }
    }

    @Test
    void searchResultPacket_roundTrip_handlesEmptyList() {
        SearchResultPacket originalPacket = new SearchResultPacket(List.of());

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        SearchResultPacket.CODEC.encode(buf, originalPacket);
        SearchResultPacket decoded = SearchResultPacket.CODEC.decode(buf);

        Assertions.assertTrue(decoded.results.isEmpty(),
            "Empty list should remain empty after round-trip, not error/null.");
    }
}