package com.azulc.ongakumod.network;

import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;
import com.azulc.ongakumod.container.TerminalMenu;
import com.azulc.ongakumod.util.ControllerRegistry.ControllerSnapshot;
import com.azulc.ongakumod.util.PlaylistHelper;
import com.azulc.ongakumod.util.TerminalControlHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
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
    public static void handleTerminalAction(TerminalActionPayload payload, IPayloadContext context) 
    {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ServerLevel level = player.serverLevel();
            ControllerSnapshot freshSnapshot = TerminalControlHandler.processTerminalCommand(level,payload.controllerUuid(),level.isLoaded(payload.targetControllerPos()),payload.targetControllerPos(), payload.actionId(),payload.playlistIndex(),player,payload.isBlockMode(),payload.terminalBlockPos());
            boolean isLoaded = freshSnapshot != null && level.isLoaded(freshSnapshot.pos());
            if (freshSnapshot != null && player.containerMenu instanceof TerminalMenu terminalMenu) {
                if (terminalMenu.getNetworkId().equals(payload.controllerUuid())) {
                    context.reply(new TerminalUpdatePayload(freshSnapshot, isLoaded));
                }
            }
        });
    }
}