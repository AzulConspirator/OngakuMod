package com.azulc.ongakumod.network;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.util.ControllerRegistry.ControllerSnapshot;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// 1. Define the S2C Custom Packet Payload
public record TerminalUpdatePayload(
    ControllerSnapshot snapshot,
    boolean isControllerLoaded
) implements CustomPacketPayload {
    public static final Type<TerminalUpdatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "terminal_sync_snapshot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalUpdatePayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            payload.snapshot.write(buf);
            buf.writeBoolean(payload.isControllerLoaded);
        },
        buf -> {
            ControllerSnapshot snapshot = ControllerSnapshot.read(buf);
            boolean isLoaded = buf.readBoolean();
            return new TerminalUpdatePayload(snapshot, isLoaded);
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}