package com.azulc.ongakumod.network;

import com.azulc.ongakumod.OngakuMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record StopDiscPayload(BlockPos controllerPos) implements CustomPacketPayload {
    public static final Type<StopDiscPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "stop_music"));

    public static final StreamCodec<FriendlyByteBuf, StopDiscPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, StopDiscPayload::controllerPos,
            StopDiscPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}