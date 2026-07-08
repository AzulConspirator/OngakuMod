package com.azulc.ongakumod.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;
import com.azulc.ongakumod.blockentity.DiscRackBlockEntity;
import com.azulc.ongakumod.compat.EtchedBridge;
import com.azulc.ongakumod.network.SyncPlaylistPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

public class PlaylistHelper
{
    public record PlaylistEntry(BlockPos rackPos, int slotIndex, ItemStack stack) {}
    public record DiscIdentity(ResourceLocation itemId, String variant,String InstanceId)
    {
        public boolean hasVariant()
        {
            return variant != null && !variant.isBlank();
        }
    }

    public static List<PlaylistEntry> buildPlaylist(AutoplayControllerBlockEntity controller) {
    return buildPlaylist(controller, true);
    }

    public static List<PlaylistEntry> peekPlaylist(AutoplayControllerBlockEntity controller) {
        return buildPlaylist(controller, false);
    }

    private static List<PlaylistEntry> buildPlaylist(AutoplayControllerBlockEntity controller, boolean reconcileQueue)
    {
        Level lvl = controller.getLevel();
        List<PlaylistEntry> playlist = new ArrayList<>();
        if (lvl == null) return playlist;
        Set<BlockPos> linkedRackPositions = CtrlHelper.getLinkedRackPositions(controller);
        List<DiscIdentity> customQueueOrder = CtrlHelper.getCustomQueue(controller);
        List<DiscIdentity> physicalItemTypes = new ArrayList<>();
        // 1. Scan linked racks
        for (BlockPos rackPos : linkedRackPositions)
        {
            if (!lvl.isLoaded(rackPos)) continue;
            if (lvl.getBlockEntity(rackPos) instanceof DiscRackBlockEntity rack)
            {
                for (int i = 0; i < rack.getContainerSize(); i++)
                {
                    ItemStack stack = rack.getItem(i);
                    if (stack.isEmpty() && controller.currentlyPlayingEntry != null && controller.currentlyPlayingEntry.rackPos().equals(rackPos) && controller.currentlyPlayingEntry.slotIndex() == i)
                    {
                        stack = controller.currentlyPlayingEntry.stack();
                    }
                    if (stack.isEmpty()) continue;
                    playlist.add(new PlaylistEntry(rackPos, i, stack.copy()));
                    DiscIdentity id = DiscIdentityHelper.get(stack,rackPos,i);
                    if(!physicalItemTypes.contains(id))
                    {
                        physicalItemTypes.add(id);
                    }
                }
            }
        }
        // 2. Add the disc currently in the jukebox, if any
        boolean playingAccountedFor = controller.currentlyPlayingEntry != null && playlist.stream().anyMatch(e -> e.rackPos().equals(controller.currentlyPlayingEntry.rackPos()) && e.slotIndex() == controller.currentlyPlayingEntry.slotIndex());
        if (controller.currentlyPlayingEntry != null && !playingAccountedFor)
        {
            playlist.add(controller.currentlyPlayingEntry);
            physicalItemTypes.add(DiscIdentityHelper.get(controller.currentlyPlayingEntry.stack(),controller.currentlyPlayingEntry.rackPos(), controller.currentlyPlayingEntry.slotIndex()));
        }
        else if (controller.currentlyPlayingEntry == null)
        {
            BlockPos jukeboxPos = JukeboxHelper.findJukebox(controller);
            if (jukeboxPos != null && lvl.getBlockEntity(jukeboxPos) instanceof JukeboxBlockEntity jukebox)
            {
                ItemStack manualDisc = jukebox.getTheItem();
                if (!manualDisc.isEmpty())
                {
                    playlist.add(new PlaylistEntry(BlockPos.ZERO, -1, manualDisc.copy()));
                    physicalItemTypes.add(DiscIdentityHelper.get(manualDisc,BlockPos.ZERO,-1));
                }
            }
        }
        // 3. Keep queue order synced to what is physically present
        if (reconcileQueue) 
        {
            boolean changed = false;
            if (customQueueOrder.removeIf(identity -> !physicalItemTypes.contains(identity))) {changed = true; }
            for (DiscIdentity identity : physicalItemTypes)
            {
                if (!customQueueOrder.contains(identity))
                {
                    customQueueOrder.add(identity);
                    changed = true;
                }
            }
            if (changed) { controller.setChanged();}
        }
        // 4. Sort by custom queue order first, then by rack position and slot
        playlist.sort((a, b) ->
        {
            int indexA = customQueueOrder.indexOf(DiscIdentityHelper.get(a.stack(), a.rackPos(), a.slotIndex()));
            int indexB = customQueueOrder.indexOf(DiscIdentityHelper.get(b.stack(), b.rackPos(), b.slotIndex()));
            if (indexA == -1) indexA = Integer.MAX_VALUE;
            if (indexB == -1) indexB = Integer.MAX_VALUE;
            return Integer.compare(indexA, indexB);
        });
        return playlist;
    }

    public static final class DiscIdentityHelper
    {
        private DiscIdentityHelper() {}

        private static DiscIdentity get(ItemStack stack)
        {
            ResourceLocation item = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (OngakuMod.IS_ETCHED_LOADED && LinkHelper.hasComponentByString(stack, "etched:music"))
            {
                String url = EtchedBridge.getEtchedUrl(stack);
                if (url != null && !url.isBlank())
                {
                    return new DiscIdentity(item, url,null);
                }
            }

            return new DiscIdentity(item, null,null);
        }
        public static DiscIdentity get(ItemStack stack, BlockPos rackPos, int slotIndex)
        {
            DiscIdentity base = get(stack);
            if (!base.hasVariant()) return base; // regular disc: no split needed, keeps collapsing
            return new DiscIdentity(base.itemId(), base.variant(), rackPos.asLong() + ":" + slotIndex);
        }

        public static boolean shouldCollapse(ItemStack stack)
        {
            return !get(stack).hasVariant();
        }
    }

    public static void broadcastPlaylistUpdate(AutoplayControllerBlockEntity controller)
    {
        Level lvl = controller.getLevel();
        if (lvl == null || lvl.isClientSide) return;
        List<ItemStack> sortedStacks = new ArrayList<>();
        for (PlaylistEntry entry : buildPlaylist(controller)) { sortedStacks.add(entry.stack()); }
        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) lvl,new ChunkPos(controller.getBlockPos()),new SyncPlaylistPayload(sortedStacks));
    }
}