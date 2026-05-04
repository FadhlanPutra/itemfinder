package com.lann.itemfinder;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class SearchPacket implements CustomPacketPayload {

    public static final Type<SearchPacket> TYPE = new Type<>(
        ResourceLocation.parse("itemfinder:search_request")
    );

    public static final StreamCodec<FriendlyByteBuf, SearchPacket> CODEC =
        StreamCodec.of(
            (buf, packet) -> buf.writeUtf(packet.itemId),
            buf -> new SearchPacket(buf.readUtf())
        );

    public final String itemId;

    public SearchPacket(String itemId) {
        this.itemId = itemId;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(TYPE, CODEC);
    }
}
