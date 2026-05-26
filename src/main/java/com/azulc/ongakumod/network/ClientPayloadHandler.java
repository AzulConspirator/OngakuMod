package com.azulc.ongakumod.network;

import com.azulc.ongakumod.client.screen.AutoplayScreen;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {
    public static void handleSyncPlaylist(final SyncPlaylistPayload payload, final IPayloadContext context) {
    context.enqueueWork(() -> {
        // Check if the player currently has the Autoplay GUI open
        if (net.minecraft.client.Minecraft.getInstance().screen instanceof AutoplayScreen screen) {
            screen.refreshLivePlaylist(payload.discs());
        }
    });
}
}
