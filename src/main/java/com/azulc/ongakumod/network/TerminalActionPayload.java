package com.azulc.ongakumod.network;

import com.azulc.ongakumod.OngakuMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.UUID;

public record TerminalActionPayload(
        UUID controllerUuid,
        BlockPos targetControllerPos,
        int actionId,
        int playlistIndex,
        boolean isBlockMode,
        Optional<BlockPos> terminalBlockPos
) implements CustomPacketPayload {

    public static final Type<TerminalActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "terminal_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalActionPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, TerminalActionPayload::controllerUuid,
            BlockPos.STREAM_CODEC, TerminalActionPayload::targetControllerPos,
            ByteBufCodecs.VAR_INT, TerminalActionPayload::actionId,
            ByteBufCodecs.VAR_INT, TerminalActionPayload::playlistIndex,
            ByteBufCodecs.BOOL, TerminalActionPayload::isBlockMode,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), TerminalActionPayload::terminalBlockPos,
            TerminalActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}