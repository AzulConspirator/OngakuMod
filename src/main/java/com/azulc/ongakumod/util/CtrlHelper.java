package com.azulc.ongakumod.util;

import java.util.List;
import java.util.Set;

import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;
import com.azulc.ongakumod.util.PlaylistHelper.DiscIdentity;
import com.azulc.ongakumod.util.PlaylistHelper.DiscIdentityHelper;
import com.azulc.ongakumod.util.PlaylistHelper.PlaylistEntry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;

public class CtrlHelper {
        //#region Helpers
    public static int GetCurrentPlayingIndex(AutoplayControllerBlockEntity Ctrl)
    {
        Level level = Ctrl.getLevel();
        BlockPos jukePos = JukeboxHelper.findJukebox(Ctrl);
        if (jukePos == null) return -1;
        if (level.getBlockEntity(jukePos) instanceof JukeboxBlockEntity jukebox) {
            ItemStack playing = jukebox.getTheItem();
            if (playing.isEmpty()) return -1;

            List<PlaylistEntry> fullList = PlaylistHelper.buildPlaylist(Ctrl); // raw list - matches widget/SyncPlaylistPayload indexing
            DiscIdentity playingIdentity = getCurrentlyPlayingIdentity(Ctrl);
            for (int i = 0; i < fullList.size(); i++) {
                if (DiscIdentityHelper.get(fullList.get(i).stack(),fullList.get(i).rackPos(),fullList.get(i).slotIndex()).equals(playingIdentity)) {
                    return i;
                }
            }
        }
        return -1;
    }
    public static DiscIdentity getCurrentlyPlayingIdentity(AutoplayControllerBlockEntity Ctrl)
    {
        if (Ctrl.currentlyPlayingEntry == null || Ctrl.currentlyPlayingEntry.stack() == null)
            return null;
        return DiscIdentityHelper.get(Ctrl.currentlyPlayingEntry.stack(),Ctrl.currentlyPlayingEntry.rackPos(),Ctrl.currentlyPlayingEntry.slotIndex());
    }
    public static DiscIdentity resolveIdentity(AutoplayControllerBlockEntity Ctrl,int playlistIndex, boolean reconcile) 
    {
        List<PlaylistEntry> fullList = reconcile ? PlaylistHelper.buildPlaylist(Ctrl) : PlaylistHelper.peekPlaylist(Ctrl);
        if (playlistIndex < 0 || playlistIndex >= fullList.size()) return null;
        PlaylistEntry entry = fullList.get(playlistIndex);
        return DiscIdentityHelper.get(entry.stack(), entry.rackPos(), entry.slotIndex());
    }
    public static List<DiscIdentity> getCustomQueue(AutoplayControllerBlockEntity Ctrl) {
        return Ctrl.customQueueOrder;
    }
    public static boolean isExcluded(AutoplayControllerBlockEntity Ctrl, int playlistIndex) {
        DiscIdentity id = resolveIdentity(Ctrl,playlistIndex, false);
        return id != null && Ctrl.excludedTracks.contains(id);
    }
    public static int getCustomOrder(AutoplayControllerBlockEntity Ctrl, int playlistIndex) {
        DiscIdentity id = resolveIdentity(Ctrl,playlistIndex, false);
        return id == null ? -1 : Ctrl.customQueueOrder.indexOf(id);
    }
    public static long getSongStartTick(AutoplayControllerBlockEntity Ctrl) {
        return Ctrl.songStartTick;
    }
    public static int getSongDurationTicks(AutoplayControllerBlockEntity Ctrl) {
        return Ctrl.songDurationTicks;
    }
    public static Set<BlockPos> getLinkedRackPositions(AutoplayControllerBlockEntity Ctrl) {
        return Ctrl.linkedRackPositions;
    }
    public static Set<BlockPos> getLinkedSpeakerPositions(AutoplayControllerBlockEntity Ctrl) {
        return Ctrl.linkedSpeakers;
    }
    public static void StopJukebox(AutoplayControllerBlockEntity Ctrl) {
        Level level = Ctrl.getLevel();
        if (level == null || level.isClientSide) return;
        Ctrl.autoplayEnabled = false;
        Ctrl.stopAndReturnDisc();
    }
    public static void toggleAutoplay(AutoplayControllerBlockEntity Ctrl) {
         Level level = Ctrl.getLevel();
        Ctrl.autoplayEnabled = !Ctrl.autoplayEnabled;
        Ctrl.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(Ctrl.getworldposition(), Ctrl.getBlockState(), Ctrl.getBlockState(), 3);
        }
    }
    public static void toggleExclusion(AutoplayControllerBlockEntity Ctrl,int playlistIndex) {
        DiscIdentity id = resolveIdentity(Ctrl,playlistIndex, true);
        if (id == null) return;
        if (Ctrl.excludedTracks.contains(id)) 
        {
            Ctrl.excludedTracks.remove(id);
        } 
        else
        {
            Ctrl.excludedTracks.add(id);
        }
    }
}
