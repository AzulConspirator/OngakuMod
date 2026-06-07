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
        boolean isControllerLoaded,
        int actionId,
        int playlistIndex,
        boolean isBlockMode,
        Optional<BlockPos> terminalBlockPos
) implements CustomPacketPayload {

    public static final Type<TerminalActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "terminal_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalActionPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> { // Encoder
            UUIDUtil.STREAM_CODEC.encode(buf, payload.controllerUuid);
            BlockPos.STREAM_CODEC.encode(buf, payload.targetControllerPos);
            buf.writeBoolean(payload.isControllerLoaded);
            buf.writeVarInt(payload.actionId);
            buf.writeVarInt(payload.playlistIndex);
            buf.writeBoolean(payload.isBlockMode);
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC).encode(buf, payload.terminalBlockPos);
        },
        (buf) -> { // Decoder
            return new TerminalActionPayload(
                UUIDUtil.STREAM_CODEC.decode(buf),
                BlockPos.STREAM_CODEC.decode(buf),
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean(),
                ByteBufCodecs.optional(BlockPos.STREAM_CODEC).decode(buf)
            );
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}