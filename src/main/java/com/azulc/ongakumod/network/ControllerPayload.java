package com.azulc.ongakumod.network;

import java.util.List;

import com.azulc.ongakumod.OngakuMod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record ControllerPayload(BlockPos pos, List<ItemStack> items) implements CustomPacketPayload {
    public static final Type<ControllerPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "controller_payload"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ControllerPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ControllerPayload::pos,
            ItemStack.OPTIONAL_LIST_STREAM_CODEC, ControllerPayload::items,
            ControllerPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}