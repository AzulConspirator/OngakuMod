package com.azulc.ongakumod.network;

import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;
import com.azulc.ongakumod.util.PlaylistHelper;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerPayloadHandler {
    public static void handlePlaylistAction(final ManagePlaylistPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = context.player().level();
            if (level.getBlockEntity(payload.pos()) instanceof AutoplayControllerBlockEntity controller) {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(payload.itemRegistryName()));
                switch (payload.action()) {
                    case TOGGLE_AUTOPLAY -> controller.toggleAutoplay();
                    case EXCLUDE -> controller.toggleExclusion(item);
                    case MOVE_UP -> controller.moveInQueue(item, -1);
                    case MOVE_DOWN -> controller.moveInQueue(item, 1);
                    case SKIP -> controller.playNextInQueue();
                    case STOP -> controller.StopJukebox();
                    case PLAY -> {    
                        int slot = payload.slotIndex().orElseThrow(() -> new IllegalArgumentException("PLAY requires a slot index"));
                        controller.tryPlayDisc(slot);
                    }
                }
                controller.setChanged();
                PlaylistHelper.broadcastPlaylistUpdate(controller); 
                level.sendBlockUpdated(payload.pos(), controller.getBlockState(), controller.getBlockState(), 3);
            }
        });
    }
}