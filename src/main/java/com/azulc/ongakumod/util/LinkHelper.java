package com.azulc.ongakumod.util;

import java.util.UUID;

import javax.annotation.Nullable;
import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;
import com.azulc.ongakumod.blockentity.DiscRackBlockEntity;
import com.azulc.ongakumod.blockentity.SpeakerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class LinkHelper {
    //#region Rack Link
    public static boolean addLinkedRack(AutoplayControllerBlockEntity Controller, BlockPos rackPos) 
    {
        if (Controller.getLevel().getBlockEntity(rackPos) instanceof DiscRackBlockEntity rack) 
        {
            BlockPos existingController = rack.getControllerPos();
            // CASE 1: TOGGLE OFF - If already linked to THIS controller, disconnect it
            if (Controller.getBlockPos().equals(existingController)) {
                removeLinkedRack(Controller,rackPos);
                return false; // Return false to indicate "Disconnected"
            }
            // CASE 2: REPLACE - If linked to a DIFFERENT controller, force a transfer
            if (existingController != null) {
                if (Controller.getLevel().getBlockEntity(existingController) instanceof AutoplayControllerBlockEntity oldController) 
                {
                    // Tell the OLD controller to forget this rack
                    removeLinkedRack(oldController,rackPos);
                }
            }
            // CASE 3: CONNECT - Establish the new link
            rack.setControllerPos(Controller.getBlockPos());
            Controller.getLinkedRackPositions().add(rackPos);
            // Finalize sync
            Controller.setChanged();
            Controller.getLevel().sendBlockUpdated(Controller.getBlockPos(), Controller.getBlockState(), Controller.getBlockState(), 3);
            rack.setChanged();
            Controller.getLevel().sendBlockUpdated(rackPos, rack.getBlockState(), rack.getBlockState(), 3);
            return true; 
        }
        return false;
    }

    public static void removeLinkedRack(AutoplayControllerBlockEntity Controller, BlockPos rackPos) 
    {
        // 1. Remove from Controller's memory
        boolean removed = Controller.getLinkedRackPositions().remove(rackPos);
        // 2. IMPORTANT: Clear the Rack's internal memory
        if (Controller.getLevel().getBlockEntity(rackPos) instanceof DiscRackBlockEntity rack) {
            // Only clear it if the rack actually thinks it belongs to THIS controller
            if (Controller.getBlockPos().equals(rack.getControllerPos())) {
                rack.setControllerPos(null);
                rack.setChanged();
                Controller.getLevel().sendBlockUpdated(rackPos, rack.getBlockState(), rack.getBlockState(), 3);
            }
        }
        // 3. Refresh playlist and sync
        if (removed) {
            //PlaylistHelper.buildPlaylist(); 
            if (Controller.getLevel() != null && !Controller.getLevel().isClientSide) {
                Controller.getLevel().sendBlockUpdated(Controller.getBlockPos(), Controller.getBlockState(), Controller.getBlockState(), 3);
                //PlaylistHelper.broadcastPlaylistUpdate(); 
            }
            Controller.setChanged();
        }
    }

    public static void clearLinkedRacks(AutoplayControllerBlockEntity Controller) 
    {
        Controller.getLinkedRackPositions().clear();
        Controller.setChanged();
    }

    public static DiscRackBlockEntity getRack(AutoplayControllerBlockEntity Controller,BlockPos pos) 
    {
        Level level = Controller.getLevel();
        if (level == null) { return null; }
        if (!level.isLoaded(pos)) {return null;}

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DiscRackBlockEntity rack) {
            return rack;
        }
        return null;
    }
    //#endregion
    //#region Speaker Link
    public static boolean addLinkedSpeaker(AutoplayControllerBlockEntity Controller, BlockPos speakerPos) 
    {
        if (Controller.getLevel() == null || Controller.getLevel().isClientSide) return false;

        // Stop music whenever a connection state changes (as requested)
        Controller.StopJukebox(); 
        if (Controller.getLinkedSpeakerPositions().contains(speakerPos)) 
        {
            Controller.getLinkedSpeakerPositions().remove(speakerPos);
            if (Controller.getLevel().getBlockEntity(speakerPos) instanceof SpeakerBlockEntity speaker) 
            {
                speaker.setControllerPos(null);
                speaker.setPlaying(false); // Stop particles immediately
            }
            Controller.setChanged();
            return false;
        } else {
            Controller.getLinkedSpeakerPositions().add(speakerPos);
            if (Controller.getLevel().getBlockEntity(speakerPos) instanceof SpeakerBlockEntity speaker) {
                speaker.setControllerPos(Controller.getBlockPos());
                // Don't set playing to true yet; wait for the next song to start
            }
            Controller.setChanged();
            return true;
        }
    }

    public static void removeLinkedSpeaker(AutoplayControllerBlockEntity Controller,BlockPos speakerPos) {
        boolean removed = Controller.getLinkedSpeakerPositions().remove(speakerPos);
        if (Controller.getLevel().getBlockEntity(speakerPos) instanceof SpeakerBlockEntity rack) {
            if (Controller.getBlockPos().equals(rack.getControllerPos())) {
                rack.setControllerPos(null);
                rack.setChanged();
                Controller.getLevel().sendBlockUpdated(speakerPos, rack.getBlockState(), rack.getBlockState(), 3);
            }
        }
        if (removed) {
            broadcastToSpeakers(Controller,false, null);
            Controller.setChanged();
        }
    }

    public static void broadcastToSpeakers(AutoplayControllerBlockEntity Controller, boolean isPlaying, @Nullable ItemStack disc) {
        Level level = Controller.getLevel();
        var linkedSpeakers = Controller.getLinkedSpeakerPositions();
            if (level == null || level.isClientSide) return;
        for (BlockPos speakerPos : linkedSpeakers) {
            if (level.isLoaded(speakerPos)) {
                BlockEntity be = level.getBlockEntity(speakerPos);
                if (be instanceof SpeakerBlockEntity speaker) {
                    // Update the BE state so particles work!
                    speaker.setPlaying(isPlaying);

                    if (isPlaying && disc != null) {
                        int songId = getSongId(level, disc);
                        if (songId != -1) {
                            level.levelEvent(null, 1010, speakerPos, songId);
                        }
                    } else {
                        level.levelEvent(1011, speakerPos, 0);
                    }
                }
                else
                {
                    removeLinkedSpeaker(Controller,speakerPos);
                }
            }
        }
    }

    // Helper to get the correct Registry ID for the song
    private static int getSongId(Level level, ItemStack stack) {
        var songHolder = stack.get(DataComponents.JUKEBOX_PLAYABLE);
        if (songHolder != null) 
        {   
            var E = level.registryAccess().registryOrThrow(Registries.JUKEBOX_SONG).getId(songHolder.song().key());
            OngakuMod.LOGGER.info("ID is " + E);
            return E;
        }
        return -1;
    }

    public static SoundEvent getSoundFromDiscId(Level level, ItemStack discId) 
    {
        if (discId == null || discId.isEmpty()) {return null;}
        return JukeboxSong.fromStack(level.registryAccess(), discId).map(holder -> holder.value().soundEvent().value()).orElse(null);
    }
    //#endregion
    public static boolean ControllerExist(UUID UUIDgiven, Level level,GlobalPos BE)
    {
        if (level.isLoaded(BE.pos()))
        {
            BlockEntity ctrl = level.getBlockEntity(BE.pos());
            if (ctrl instanceof AutoplayControllerBlockEntity controller) 
            {
                if (AutoplayControllerBlockEntity.getNetworkId(controller).equals(UUIDgiven))
                {
                    return true;
                }
            }
        }
        return false;
    }
}
