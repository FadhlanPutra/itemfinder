package com.lann.itemfinder;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchResultPacket implements CustomPacketPayload {

    public static final Type<SearchResultPacket> TYPE = new Type<>(
        Identifier.parse("itemfinder:search_result")
    );

    public static final StreamCodec<FriendlyByteBuf, SearchResultPacket> CODEC =
        StreamCodec.of(
            (buf, packet) -> {
                buf.writeInt(packet.results.size());
                for (StorageScanner.SearchResult r : packet.results) {
                    buf.writeBlockPos(r.pos);
                    buf.writeUtf(r.containerType);
                    buf.writeInt(r.count);
                    buf.writeInt(r.items.size());
                    for (Map.Entry<String, Integer> entry : r.items.entrySet()) {
                        buf.writeUtf(entry.getKey());
                        buf.writeInt(entry.getValue());
                    }
                }
            },
            buf -> {
                int size = buf.readInt();
                List<StorageScanner.SearchResult> results = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    BlockPos pos = buf.readBlockPos();
                    String containerType = buf.readUtf();
                    int count = buf.readInt();
                    int itemCount = buf.readInt();
                    Map<String, Integer> items = new HashMap<>();
                    for (int j = 0; j < itemCount; j++) {
                        String itemId = buf.readUtf();
                        int itemCnt = buf.readInt();
                        items.put(itemId, itemCnt);
                    }
                    results.add(new StorageScanner.SearchResult(pos, containerType, count, items));
                }
                return new SearchResultPacket(results);
            }
        );

    public final List<StorageScanner.SearchResult> results;

    public SearchResultPacket(List<StorageScanner.SearchResult> results) {
        this.results = results;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
    }
}
