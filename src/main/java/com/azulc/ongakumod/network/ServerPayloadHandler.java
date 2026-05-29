package com.azulc.ongakumod.network;

import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerPayloadHandler {

    public static void handlePlayDisc(final PlayDiscPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                Level level = player.level();
                if (level.getBlockEntity(payload.controllerPos()) instanceof AutoplayControllerBlockEntity controller) {
                    controller.tryPlayDisc(payload.slotIndex());
                }
            }
        });
    }

    public static void handleStopDisc(final StopDiscPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (player.level().getBlockEntity(payload.controllerPos()) instanceof AutoplayControllerBlockEntity controller) {
                    controller.stopAndReturnDisc();
                }
            }
        });
    }
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
                }
                
                // This is the key: after moving or excluding, tell the UI to refresh!
                controller.setChanged();
                controller.broadcastPlaylistUpdate(); 
                level.sendBlockUpdated(payload.pos(), controller.getBlockState(), controller.getBlockState(), 3);
            }
        });
    }
}