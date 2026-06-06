package com.azulc.ongakumod.network;

import com.azulc.ongakumod.client.screen.AutoplayScreen;
import com.azulc.ongakumod.util.TerminalSoundHandler;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler 
{
    public static void handleSyncPlaylist(final SyncPlaylistPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            // Check if the player currently has the Autoplay GUI open
            if (net.minecraft.client.Minecraft.getInstance().screen instanceof AutoplayScreen screen) {
                screen.refreshLivePlaylist(payload.discs());
            }
        });
    }
    public static void handleTerminalAudio(TerminalAudioPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Execution context isolated safely onto the Client Render thread
            if (payload.isStopPacket()) {
                TerminalSoundHandler.stopSound(payload.controllerId());
                return;
            }

            if (payload.soundEvent().isEmpty()) return;

            if (payload.isBlockMode()) {
                payload.blockPos().ifPresent(pos -> 
                    TerminalSoundHandler.playBlockModeSound(payload.controllerId(), payload.soundEvent().get(), pos)
                );
            } else {
                payload.entityId().ifPresent(id -> 
                    TerminalSoundHandler.playItemModeSound(payload.controllerId(), payload.soundEvent().get(), id)
                );
            }
        });
    }
}
