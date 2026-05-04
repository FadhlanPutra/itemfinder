package com.lann.itemfinder;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class SearchResultPacket implements CustomPacketPayload {

    public static final Type<SearchResultPacket> TYPE = new Type<>(
        ResourceLocation.parse("itemfinder:search_result")
    );

    public static final StreamCodec<FriendlyByteBuf, SearchResultPacket> CODEC =
        StreamCodec.of(
            (buf, packet) -> {
                buf.writeInt(packet.results.size());
                for (StorageScanner.SearchResult r : packet.results) {
                    buf.writeBlockPos(r.pos);
                    buf.writeUtf(r.containerType);
                    buf.writeInt(r.count);
                }
            },
            buf -> {
                int size = buf.readInt();
                List<StorageScanner.SearchResult> results = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    BlockPos pos = buf.readBlockPos();
                    String containerType = buf.readUtf();
                    int count = buf.readInt();
                    results.add(new StorageScanner.SearchResult(pos, containerType, count));
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
        PayloadTypeRegistry.playS2C().register(TYPE, CODEC);
    }
}
