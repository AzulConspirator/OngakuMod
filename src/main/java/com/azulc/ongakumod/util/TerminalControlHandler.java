package com.azulc.ongakumod.util;

import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;
import com.azulc.ongakumod.network.TerminalAudioPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TerminalControlHandler {

    public static final int ACTION_PLAY_TRACK = 0;
    public static final int ACTION_STOP       = 1;
    public static final int ACTION_SKIP       = 2;
    public static final int ACTION_TOGGLE_AP  = 3;

    public static void processTerminalCommand(ServerLevel level, UUID controllerUuid,Boolean isControllerLoaded, BlockPos targetPos, int actionId, int playlistIndex,ServerPlayer player,boolean isBlockMode, Optional<BlockPos> terminalBlockPos)
    {
        // Branch based on chunk loading state
        // 1. Attempt to fetch the BlockEntity ONLY if the chunk is loaded.
        // If unloaded, this safely evaluates to null without forcing a chunk load.
        BlockEntity be = isControllerLoaded ? level.getBlockEntity(targetPos) : null;
        // 2. Truth over agreement: Is the physical controller actually accessible right now?
        if (be instanceof AutoplayControllerBlockEntity controller) {
            // ONLINE MODE: The chunk is loaded and the controller exists.
            executeOnlineAction(controller, actionId, playlistIndex,player,isBlockMode,terminalBlockPos);
        } else {
            // OFFLINE MODE: Catch-all for unloaded chunks, destroyed blocks, or bad BlockPos fallbacks.
            executeOfflineAction(level, controllerUuid, actionId, player, isBlockMode, terminalBlockPos);
        }
    }

    private static void executeOnlineAction(AutoplayControllerBlockEntity controller, int actionId, int playlistIndex, ServerPlayer player, boolean isBlockMode, Optional<BlockPos> terminalBlockPos) {
        switch (actionId) {
            case ACTION_PLAY_TRACK -> {
                controller.tryPlayDisc(playlistIndex);
                // NEW: Pipe the audio back to the terminal user!
                if (controller.currentlyPlayingEntry != null) {
                    SoundEvent sound = getSoundFromDiscId(controller.getLevel(), BuiltInRegistries.ITEM.getKey(controller.currentlyPlayingEntry.stack().getItem()).toString());
                    if (sound != null) {
                        dispatchAudioPayload(player, controller.getNetworkId(controller), false, isBlockMode, terminalBlockPos, sound);
                    }
                }
            }
            case ACTION_STOP       -> {
                controller.StopJukebox();
                dispatchAudioPayload(player, controller.getNetworkId(controller), true, isBlockMode, terminalBlockPos, null);
            }
            case ACTION_SKIP       -> {
                controller.playNextInQueue();
                dispatchAudioPayload(player, controller.getNetworkId(controller), true, isBlockMode, terminalBlockPos, null);
            }
            case ACTION_TOGGLE_AP  -> controller.toggleAutoplay();
            default -> throw new IllegalArgumentException("Unknown local action ID: " + actionId);
        }
    }

    private static void executeOfflineAction(ServerLevel level, UUID controllerUuid, int actionId, ServerPlayer player,boolean isBlockMode, Optional<BlockPos> terminalBlockPos) 
    {
        ControllerRegistry registry = ControllerRegistry.get(level);
        ControllerRegistry.ControllerSnapshot snapshot = registry.getSnapshot(controllerUuid);
        if (snapshot == null) return;
        // Aligned strictly to your record field accessors
        UUID id = snapshot.networkId(); 
        BlockPos pos = snapshot.pos();
        String currentDisc = snapshot.currentDisc();
        List<String> playlist = snapshot.playlist();
        int trackIndex = snapshot.playlistIndex();
        boolean apEnabled = snapshot.autoplay();
        long startTick = snapshot.songStartTick();
        long duration = snapshot.songDurationTicks();
        switch (actionId) {
            case ACTION_PLAY_TRACK -> {
                if(startTick > 0)
                {
                    trackIndex++;
                }
                trackIndex =(trackIndex + 1) % playlist.size();
                currentDisc = playlist.get(trackIndex);
                startTick = level.getGameTime();
                SoundEvent sound = getSoundFromDiscId(level, currentDisc);
                if(sound != null)
                {
                    dispatchAudioPayload(player,controllerUuid,false,isBlockMode,terminalBlockPos,sound);
                }
            }
            case ACTION_STOP -> {
                apEnabled = false;
                startTick = -1;
                dispatchAudioPayload(player, controllerUuid, true, isBlockMode, terminalBlockPos, null);
            }
            case ACTION_SKIP -> {
                //trackIndex++;
                trackIndex =(trackIndex + 1) % playlist.size();
                currentDisc = playlist.get(trackIndex);
                dispatchAudioPayload(player, controllerUuid, true, isBlockMode, terminalBlockPos, null);
            }
            case ACTION_TOGGLE_AP -> {
                apEnabled = !apEnabled;
            }
        }
        ControllerRegistry.ControllerSnapshot updatedSnapshot = new ControllerRegistry.ControllerSnapshot(id, pos, currentDisc,playlist, trackIndex, apEnabled, startTick, duration);
        registry.updateSnapshot(controllerUuid, updatedSnapshot);
    }
    private static SoundEvent getSoundFromDiscId(Level level, String discId) {
        if (discId == null || discId.isEmpty()) {
            return null;
        }
        // 1. Parse the resource location and fetch the item safely
        ResourceLocation location = ResourceLocation.parse(discId);
        Item item = BuiltInRegistries.ITEM.get(location);
        // Construct a temporary stack to evaluate its default properties
        ItemStack stack = new ItemStack(item);
        // 2. Use Vanilla's built-in stack unpacker for Jukebox songs
        return JukeboxSong.fromStack(level.registryAccess(), stack).map(holder -> holder.value().soundEvent().value()).orElse(null);
    }

    private static void dispatchAudioPayload(ServerPlayer player, UUID controllerId, boolean isStop, boolean isBlockMode, Optional<BlockPos> terminalPos, SoundEvent sound) 
    {
        TerminalAudioPayload packet = new TerminalAudioPayload(controllerId, isStop, isBlockMode, terminalPos,isBlockMode ? Optional.empty() : Optional.of(player.getId()),Optional.ofNullable(sound));
        if (isBlockMode && terminalPos.isPresent()) 
        {
            ChunkPos chunkPos = new ChunkPos(terminalPos.get());
            PacketDistributor.sendToPlayersTrackingChunk(player.serverLevel(), chunkPos, packet);
        } 
        else 
        {
            PacketDistributor.sendToPlayer(player, packet); 
            PacketDistributor.sendToPlayersTrackingEntity(player, packet);
        }
    }

}