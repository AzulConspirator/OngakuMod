package com.azulc.ongakumod.network;

import java.util.Optional;
import java.util.UUID;

import com.azulc.ongakumod.OngakuMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record ManagePlaylistPayload(Optional<BlockPos> pos, Optional<UUID> networkId,Optional<ItemStack> itemRegistryName, Action action, Optional<Integer> slotIndex) implements CustomPacketPayload {
    public static final Type<ManagePlaylistPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "manage_playlist"));

    public enum Action { TOGGLE_AUTOPLAY, MOVE_UP, MOVE_DOWN, EXCLUDE,SKIP,STOP,PLAY }

    public static final StreamCodec<RegistryFriendlyByteBuf, ManagePlaylistPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), ManagePlaylistPayload::pos,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), ManagePlaylistPayload::networkId,
            ByteBufCodecs.optional(ItemStack.STREAM_CODEC), ManagePlaylistPayload::itemRegistryName,
            ByteBufCodecs.idMapper(i -> Action.values()[i], Action::ordinal), ManagePlaylistPayload::action,
            ByteBufCodecs.optional(ByteBufCodecs.INT), ManagePlaylistPayload::slotIndex,
            ManagePlaylistPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}