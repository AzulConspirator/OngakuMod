package com.azulc.ongakumod.network;

import java.util.Optional;
import java.util.UUID;

import com.azulc.ongakumod.client.screen.AutoplayScreen;
import com.azulc.ongakumod.container.TerminalMenu;
import com.azulc.ongakumod.util.SoundHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler 
{
    public static void handleSyncPlaylist(final SyncPlaylistPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (net.minecraft.client.Minecraft.getInstance().screen instanceof AutoplayScreen screen) {
                screen.refreshLivePlaylist(payload.discs());
            }
        });
    }
    public static void handleAudio(AudioPayload payload, IPayloadContext context)
    {
        context.enqueueWork(() -> {
            UUID controllerId = payload.controllerId();
            if (payload.isStopPacket()) {
                Optional<BlockPos> keyPos = payload.isBlockMode() ? payload.blockPos() : Optional.empty();
                SoundHandler.stopSound(controllerId, keyPos);
                return;
            }

            if (payload.isBlockMode()) {
                payload.blockPos().ifPresent(pos ->
                    SoundHandler.playBlockModeSound(controllerId, payload.Disc(), pos)
                );
            } else {
                int playerEntityId = Minecraft.getInstance().player.getId();
                SoundHandler.playItemModeSound(controllerId, payload.Disc(), playerEntityId);
            }
        });
    }
    public static void RefreshSnapshot(TerminalUpdatePayload payload, IPayloadContext context) 
    {
        context.enqueueWork(() -> 
        {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && player.containerMenu instanceof TerminalMenu terminalMenu) 
            {
                terminalMenu.clientUpdateSnapshot(payload.snapshot(), payload.isControllerLoaded());
            }
        });
    }
}
