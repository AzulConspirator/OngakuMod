package com.azulc.ongakumod.network;

import java.util.Optional;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ManagePlaylistPayload(BlockPos pos, String itemRegistryName, Action action, Optional<Integer> slotIndex) implements CustomPacketPayload {
    public static final Type<ManagePlaylistPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("ongakumod", "manage_playlist"));

    public enum Action { TOGGLE_AUTOPLAY, MOVE_UP, MOVE_DOWN, EXCLUDE,SKIP,STOP,PLAY }

    public static final StreamCodec<ByteBuf, ManagePlaylistPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ManagePlaylistPayload::pos,
            ByteBufCodecs.STRING_UTF8, ManagePlaylistPayload::itemRegistryName,
            ByteBufCodecs.idMapper(i -> Action.values()[i], Action::ordinal), ManagePlaylistPayload::action,
            ByteBufCodecs.optional(ByteBufCodecs.INT), ManagePlaylistPayload::slotIndex,
            ManagePlaylistPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}