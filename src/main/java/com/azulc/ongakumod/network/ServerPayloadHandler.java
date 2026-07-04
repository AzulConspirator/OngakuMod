package com.azulc.ongakumod.network;

import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;
import com.azulc.ongakumod.container.TerminalMenu;
import com.azulc.ongakumod.util.ControllerRegistry;
import com.azulc.ongakumod.util.ControllerRegistry.ControllerSnapshot;
import com.azulc.ongakumod.util.PlaylistHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerPayloadHandler {
    public static void handlePlaylistAction(final ManagePlaylistPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = context.player().level();
            BlockPos pos = payload.pos().orElseThrow(() -> new IllegalArgumentException("PLAY requires a slot index"));
            if (level.getBlockEntity(pos) instanceof AutoplayControllerBlockEntity controller) {
                switch (payload.action()) {
                    case TOGGLE_AUTOPLAY -> controller.toggleAutoplay();
                    case EXCLUDE -> controller.toggleExclusion(payload.itemRegistryName().get());
                    case MOVE_UP -> controller.moveInQueue(payload.itemRegistryName().get(), -1);
                    case MOVE_DOWN -> controller.moveInQueue(payload.itemRegistryName().get(), 1);
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
            ControllerRegistry registry = ControllerRegistry.get(level);
            boolean isLoaded = registry.isControllerLoaded(player.server, payload.controllerUuid());
            // Use the controller's own dimension when it's actually loaded, so getBlockEntity()
            // in the online path looks in the right world instead of wherever the player is standing.
            ServerLevel controllerLevel = isLoaded
                ? registry.resolveControllerLevel(player.server, payload.controllerUuid())
                : level;

            ControllerSnapshot freshSnapshot = TerminalControlHandler.processTerminalCommand(
                controllerLevel, payload.controllerUuid(), isLoaded, payload.targetControllerPos(),
                payload.actionId(), payload.playlistIndex(), player, payload.isBlockMode(), payload.terminalBlockPos());

            boolean freshIsLoaded = freshSnapshot != null && registry.isControllerLoaded(player.server, payload.controllerUuid());
            if (freshSnapshot != null && player.containerMenu instanceof TerminalMenu terminalMenu) {
                if (terminalMenu.getNetworkId().equals(payload.controllerUuid())) {
                    context.reply(new TerminalUpdatePayload(freshSnapshot, freshIsLoaded));
                }
            }
        });
    }
}