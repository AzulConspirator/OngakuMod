package com.azulc.ongakumod.network;

import com.azulc.ongakumod.OngakuMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.Optional;
import java.util.UUID;

public record TerminalAudioPayload(UUID controllerId, boolean isStopPacket,boolean isBlockMode,Optional<BlockPos> blockPos,Optional<Integer> entityId,Optional<SoundEvent> soundEvent) implements CustomPacketPayload 
{
    public static final Type<TerminalAudioPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "terminal_audio"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalAudioPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, TerminalAudioPayload::controllerId,
            ByteBufCodecs.BOOL, TerminalAudioPayload::isStopPacket,
            ByteBufCodecs.BOOL, TerminalAudioPayload::isBlockMode,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), TerminalAudioPayload::blockPos,
            ByteBufCodecs.optional(ByteBufCodecs.VAR_INT), TerminalAudioPayload::entityId,
            ByteBufCodecs.optional(ByteBufCodecs.registry(Registries.SOUND_EVENT)), TerminalAudioPayload::soundEvent,
            TerminalAudioPayload::new
    );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}