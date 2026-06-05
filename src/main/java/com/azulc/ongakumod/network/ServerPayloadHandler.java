package com.azulc.ongakumod.network;

import java.util.Optional;
import java.util.UUID;

import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;
import com.azulc.ongakumod.network.ManagePlaylistPayload.Action;
import com.azulc.ongakumod.util.ControllerRegistry;
import com.azulc.ongakumod.util.ControllerRegistry.ControllerSnapshot;
import com.azulc.ongakumod.util.PlaylistHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerPayloadHandler {
    public static void handlePlaylistAction(final ManagePlaylistPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = context.player().level();
            BlockPos pos = payload.pos().orElseThrow(() -> new IllegalArgumentException("PLAY requires a slot index"));
            if (level.getBlockEntity(pos) instanceof AutoplayControllerBlockEntity controller) {
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
                level.sendBlockUpdated(pos, controller.getBlockState(), controller.getBlockState(), 3);
            }
        });
    }
    public static void handleTerminalAction(ManagePlaylistPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player.level() instanceof ServerLevel serverLevel)) return;
            BlockPos pos = payload.pos().orElseThrow(() -> new IllegalArgumentException("PLAY requires a slot index"));
            if (serverLevel.isLoaded(pos)) {
                BlockEntity be = serverLevel.getBlockEntity(pos); 
                if (be instanceof AutoplayControllerBlockEntity controller) { 
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
                    return; // Operations completed successfully on the live block, exit early.
                }
            }

            // --- SPLIT BRAIN RESOLUTION ROUTE ---
            // Execution hits here ONLY if chunk is unloaded. Read from snapshot instead:
            ControllerRegistry registry = ControllerRegistry.get(serverLevel);
            UUID netId = payload.networkId().orElseThrow(() -> new IllegalArgumentException("PLAY requires a slot index"));
            ControllerSnapshot snapshotOpt = registry.getSnapshot(netId); // Fixed method reference
            
            if (snapshotOpt !=null) {
                ControllerSnapshot snapshot = snapshotOpt;
                
                if(payload.action() == Action.PLAY) {
                     // Local client audio fallback emission point
                     serverLevel.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.RECORDS, 1.0f, 1.0f);
                }
            }
        });
    }
}