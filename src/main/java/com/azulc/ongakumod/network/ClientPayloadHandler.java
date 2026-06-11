package com.azulc.ongakumod.network;

import java.util.UUID;

import com.azulc.ongakumod.client.screen.AutoplayScreen;
import com.azulc.ongakumod.container.TerminalMenu;
import com.azulc.ongakumod.util.TerminalSoundHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
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
    public static void handleTerminalAudio(TerminalAudioPayload payload, IPayloadContext context) 
    {
        context.enqueueWork(() -> {
            UUID controllerId = payload.controllerId();
            if (payload.isStopPacket()) {
                TerminalSoundHandler.stopSound(controllerId);
                return;
            }
            SoundEvent soundEvent = payload.soundEvent().orElse(null);
            
            if (payload.isBlockMode()) {
                payload.blockPos().ifPresent(pos -> 
                    TerminalSoundHandler.playBlockModeSound(controllerId, soundEvent, payload.Disc(), pos)
                );
            } else {
                int playerEntityId = Minecraft.getInstance().player.getId();
                TerminalSoundHandler.playItemModeSound(controllerId, soundEvent, payload.Disc(), playerEntityId);
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
