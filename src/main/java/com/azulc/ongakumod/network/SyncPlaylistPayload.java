package com.azulc.ongakumod.network;

import java.util.ArrayList;
import java.util.List;

import com.azulc.ongakumod.OngakuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record SyncPlaylistPayload(List<ItemStack> discs) implements CustomPacketPayload {
    public static final Type<SyncPlaylistPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "sync_playlist"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPlaylistPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, ItemStack.OPTIONAL_STREAM_CODEC), SyncPlaylistPayload::discs,
            SyncPlaylistPayload::new
    );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}