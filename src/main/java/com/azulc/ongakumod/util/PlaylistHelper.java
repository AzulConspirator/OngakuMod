package com.azulc.ongakumod.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;
import com.azulc.ongakumod.blockentity.DiscRackBlockEntity;
import com.azulc.ongakumod.compat.EtchedBridge;
import com.azulc.ongakumod.network.SyncPlaylistPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

public class PlaylistHelper {
    
    public record PlaylistEntry( BlockPos rackPos,int slotIndex,ItemStack stack) {}
    public record DisplayPlaylistEntry(ItemStack stack, int count) {}

    public static List<PlaylistEntry> buildPlaylist(AutoplayControllerBlockEntity Controller) 
    {
        Level lvl = Controller.getLevel();
        List<PlaylistEntry> playlist = new ArrayList<>();
        Set<BlockPos> linkedRackPositions = Controller.getLinkedRackPositions();
        List<Item> customQueueOrder = Controller.getCustomQueue();
        // Track unique item types physically present in the racks/jukebox
        Set<Item> physicalItemTypes = new java.util.HashSet<>();
        // 1. Physical scan of racks
        for (BlockPos rackPos : linkedRackPositions) 
        {
            if (!lvl.isLoaded(rackPos)) continue;
            if (lvl.getBlockEntity(rackPos) instanceof DiscRackBlockEntity rack) 
            {
                for (int i = 0; i < rack.getContainerSize(); i++) {
                    ItemStack stack = rack.getItem(i);
                    if (!stack.isEmpty()) {
                        playlist.add(new PlaylistEntry(rackPos, i, stack.copy()));
                        physicalItemTypes.add(stack.getItem());
                    }
                }
            }
        }
        // 2. Add the disc currently in the jukebox
        if (Controller.currentlyPlayingEntry != null) {
            playlist.add(Controller.currentlyPlayingEntry);
            physicalItemTypes.add(Controller.currentlyPlayingEntry.stack().getItem());
        } else {
            BlockPos jukeboxPos = JukeboxHelper.findJukebox(Controller);
            if (jukeboxPos != null && lvl.getBlockEntity(jukeboxPos) instanceof JukeboxBlockEntity jukebox) {
                ItemStack manualDisc = jukebox.getTheItem();
                if (!manualDisc.isEmpty()) {
                    playlist.add(new PlaylistEntry(BlockPos.ZERO, -1, manualDisc.copy()));
                    physicalItemTypes.add(manualDisc.getItem());
                }
            }
        }
        //Step 3
        boolean changed = false;
        // A. Remove items from the queue that are no longer physically present
        if (customQueueOrder.removeIf(item -> !physicalItemTypes.contains(item))) {
            changed = true;
        }
        // B. Find the first physical item not in the queue and add it 
        for (Item item : physicalItemTypes) {
            if (!customQueueOrder.contains(item)) {
                customQueueOrder.add(item);
                changed = true;
                break; // Stop after finding the first new one
            }
        }
        if (changed) {
            Controller.setChanged();
        }
        // SORT
        playlist.sort((a, b) -> {
            int indexA = customQueueOrder.indexOf(a.stack().getItem());
            int indexB = customQueueOrder.indexOf(b.stack().getItem());
            //
            if (indexA != -1 && indexB != -1) {
                int queueComp = Integer.compare(indexA, indexB);
                if (queueComp != 0) return queueComp;
            }
            //
            int rackCompare = a.rackPos().compareTo(b.rackPos());
            if (rackCompare != 0) return rackCompare;
            return Integer.compare(a.slotIndex(), b.slotIndex());
        });
        return playlist;
    }

    public static List<DisplayPlaylistEntry> buildCollapsedPlaylist(AutoplayControllerBlockEntity Controller) 
    {
        Map<String, DisplayPlaylistEntry> map = new LinkedHashMap<>();
        for (PlaylistEntry entry : buildPlaylist(Controller)) 
        {
            ItemStack stack = entry.stack();
            String key = stack.getItem().toString();
            String uniqueGroupKey = key;

            if (OngakuMod.IS_ETCHED_LOADED && key =="etched:etched_music_disc")
            {
                uniqueGroupKey = key + "_" + EtchedBridge.getEtchedUrl(stack);
            }
            if (map.containsKey(uniqueGroupKey)) 
            {
                DisplayPlaylistEntry existing = map.get(uniqueGroupKey);
                map.put(uniqueGroupKey,new DisplayPlaylistEntry(existing.stack(),existing.count() + 1));
            }
            else 
            {
                map.put(uniqueGroupKey,new DisplayPlaylistEntry(stack.copy(),1));
            }
        }
        return new ArrayList<>(map.values());
    }

    // 3. Update broadcast to use the SORTED list
    public static void broadcastPlaylistUpdate(AutoplayControllerBlockEntity Controller) 
    {
        Level Lvl = Controller.getLevel();
        if (Lvl == null || Lvl.isClientSide) return;

        List<ItemStack> sortedStacks = new ArrayList<>();
        for (PlaylistEntry entry : buildPlaylist(Controller)) {
            sortedStacks.add(entry.stack());
        }
        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) Lvl, new ChunkPos(Controller.getBlockPos()),  new SyncPlaylistPayload(sortedStacks));
    }
}
