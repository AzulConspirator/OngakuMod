package com.azulc.ongakumod.network;

import com.azulc.ongakumod.OngakuMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

public record TerminalAudioPayload(UUID controllerId,Optional<ItemStack> Disc, boolean isStopPacket,boolean isBlockMode,Optional<BlockPos> blockPos,Optional<Integer> entityId) implements CustomPacketPayload 
{
    public static final Type<TerminalAudioPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "terminal_audio"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalAudioPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> { // Encoder
            UUIDUtil.STREAM_CODEC.encode(buf, payload.controllerId);
            ByteBufCodecs.optional(ItemStack.STREAM_CODEC).encode(buf, payload.Disc);
            buf.writeBoolean(payload.isStopPacket);
            buf.writeBoolean(payload.isBlockMode);
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC).encode(buf, payload.blockPos);
            ByteBufCodecs.optional(ByteBufCodecs.VAR_INT).encode(buf, payload.entityId);
            //ByteBufCodecs.optional(ByteBufCodecs.registry(Registries.SOUND_EVENT)).encode(buf, payload.soundEvent);
        },
        (buf) -> { // Decoder
            return new TerminalAudioPayload(
                UUIDUtil.STREAM_CODEC.decode(buf),
                ByteBufCodecs.optional(ItemStack.STREAM_CODEC).decode(buf),
                buf.readBoolean(),
                buf.readBoolean(),
                ByteBufCodecs.optional(BlockPos.STREAM_CODEC).decode(buf),
                ByteBufCodecs.optional(ByteBufCodecs.VAR_INT).decode(buf)
            );
        }
    );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}