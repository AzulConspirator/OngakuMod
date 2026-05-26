package com.azulc.ongakumod.network;

import com.azulc.ongakumod.OngakuMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlayDiscPayload(BlockPos controllerPos, int slotIndex) implements CustomPacketPayload {

    // 1. Unique ID for this packet
    public static final Type<PlayDiscPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "play_disc"));

    // 2. The Codec that tells the game how to translate this record into bytes and back
    public static final StreamCodec<FriendlyByteBuf, PlayDiscPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PlayDiscPayload::controllerPos,
            ByteBufCodecs.INT, PlayDiscPayload::slotIndex,
            PlayDiscPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}