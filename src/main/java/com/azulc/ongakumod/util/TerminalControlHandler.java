package com.azulc.ongakumod.util;

import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;
import com.azulc.ongakumod.network.TerminalAudioPayload;
import com.azulc.ongakumod.util.ControllerRegistry.ControllerSnapshot;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TerminalControlHandler {

    public static final int ACTION_PLAY_TRACK = 0;
    public static final int ACTION_STOP       = 1;
    public static final int ACTION_TOGGLE_AP  = 3;

    public static ControllerSnapshot processTerminalCommand(ServerLevel level, UUID controllerUuid,Boolean isControllerLoaded, BlockPos targetPos, int actionId, int playlistIndex,ServerPlayer player,boolean isBlockMode, Optional<BlockPos> terminalBlockPos)
    {
        // is the controller chunk loaded? get variable from method, if chunk not loaded persume exist, continue offline
        // if it is, where is the block entity grab grom level.getblockentity, if blockEntity not found, delete snapshot registry cancel command
        // if found, does it have the same UUID? just compare controller UUID and the one given from Terminal UI, if UUID not the same, delete snapshot registry
        if (!isControllerLoaded)
        {
            executeOfflineAction(level, controllerUuid, actionId, player, isBlockMode, terminalBlockPos);
            return ControllerRegistry.get(level).getSnapshot(controllerUuid); 
        }
        BlockEntity be = level.getBlockEntity(targetPos);
        if (be instanceof AutoplayControllerBlockEntity controller) 
        {
            if (AutoplayControllerBlockEntity.getNetworkId(controller).equals(controllerUuid))
            {
                executeOnlineAction(controller, actionId, playlistIndex, player, isBlockMode, terminalBlockPos);
                ControllerSnapshot freshSnapshot = AutoplayControllerBlockEntity.createSnapshot(controller);
                ControllerRegistry.get(level).updateSnapshot(controllerUuid, freshSnapshot);
                return freshSnapshot;
            }
        }
        //so invalid Terminal ID then
        ControllerRegistry.get(level).unregister(controllerUuid);
        return null; 
    }

    private static void executeOnlineAction(AutoplayControllerBlockEntity controller,int actionId, int playlistIndex, ServerPlayer player, boolean isBlockMode, Optional<BlockPos> terminalBlockPos) {
        UUID networkId = AutoplayControllerBlockEntity.getNetworkId(controller);
        BlockPos targetPos = controller.getBlockPos();
        Level level = controller.getLevel();
        
        switch (actionId) 
        {
            case ACTION_STOP -> {
                controller.StopJukebox();
                broadcastToTerminalOffline(player, networkId,Optional.empty(),  true, isBlockMode, terminalBlockPos);
            }
            case ACTION_PLAY_TRACK -> {
                controller.playNextInQueue();
                if (controller.currentlyPlayingEntry != null) {
                    ItemStack stack = controller.currentlyPlayingEntry.stack();
                    if (stack != null || LinkHelper.hasComponentByString(stack, "etched:music")) {
                        broadcastToTerminalOffline(player, networkId, Optional.of(stack), false, isBlockMode, terminalBlockPos);
                    }
                }
            }
            case ACTION_TOGGLE_AP -> controller.toggleAutoplay();
            default -> throw new IllegalArgumentException("Unknown local action ID: " + actionId);
        }
        // no need for case for ACTION_SKIP due to terminal combines Play n Skip
        controller.setChanged();
        BlockState state = level.getBlockState(targetPos);
        level.sendBlockUpdated(targetPos, state, state, 3);
    }

    private static void executeOfflineAction(ServerLevel level, UUID controllerUuid, int actionId, ServerPlayer player,boolean isBlockMode, Optional<BlockPos> terminalBlockPos) 
    {
        ControllerRegistry registry = ControllerRegistry.get(level);
        ControllerRegistry.ControllerSnapshot snapshot = registry.getSnapshot(controllerUuid);
        if (snapshot == null) return;

        UUID id = snapshot.networkId(); 
        BlockPos pos = snapshot.pos();
        ItemStack currentDisc = snapshot.currentDisc();
        List<ItemStack> playlist = snapshot.playlist();
        int trackIndex = snapshot.playlistIndex();
        boolean apEnabled = snapshot.autoplay();
        long startTick = snapshot.songStartTick();
        long duration = snapshot.songDurationTicks();

        if (playlist == null || playlist.isEmpty()) return;

        switch (actionId) 
        {
            case ACTION_STOP -> 
            {
                apEnabled = false;
                startTick = -1;
                broadcastToTerminalOffline(player, controllerUuid,Optional.of(currentDisc), true, isBlockMode, terminalBlockPos);
            }
            case ACTION_PLAY_TRACK -> 
            {
                if (trackIndex < 0 || trackIndex >= playlist.size()) {
                    trackIndex = 0;
                } else {
                    trackIndex = (trackIndex + 1) % playlist.size();
                }
                
                currentDisc = playlist.get(trackIndex);
                startTick = level.getGameTime();
                if (currentDisc != null || LinkHelper.hasComponentByString(currentDisc, "etched:music")) {
                    broadcastToTerminalOffline(player, controllerUuid, Optional.of(currentDisc), false, isBlockMode, terminalBlockPos);
                }
            }
            case ACTION_TOGGLE_AP -> apEnabled = !apEnabled;
            // no need for case for ACTION_SKIP due to terminal combines Play n Skip
        }
        ControllerRegistry.ControllerSnapshot updatedSnapshot = new ControllerRegistry.ControllerSnapshot(id, pos, currentDisc, playlist, trackIndex, apEnabled, startTick, duration);
        registry.updateSnapshot(controllerUuid, updatedSnapshot);
    }

    public static void broadcastToTerminalOffline(ServerPlayer player, UUID controllerId,Optional<ItemStack> Disc, boolean isStop, boolean isBlockMode, Optional<BlockPos> terminalPos) 
    {
        if (controllerId == null) {return;}
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,new TerminalAudioPayload(controllerId,Disc, isStop, isBlockMode, terminalPos,Optional.ofNullable(player.getId())));
    }

    public static void broadcastToTerminalOnline(ServerLevel level, UUID controllerId, boolean isPlaying, ItemStack disc) 
    {
        for (BlockPos pos : ControllerRegistry.get(level).getLinkedTerminals(controllerId)) {
            if (!level.isLoaded(pos)) continue;
            TerminalAudioPayload packet = new TerminalAudioPayload(controllerId, Optional.ofNullable(disc), !isPlaying, true, Optional.of(pos), Optional.empty());
            PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(pos), packet);
        }
    }

}