package com.azulc.ongakumod.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public record DisplayPlaylistEntry(ItemStack stack, int count) {}
    public record DiscIdentity(ResourceLocation itemId, String variant)
    {
        public boolean hasVariant()
        {
            return variant != null && !variant.isBlank();
        }
    }

    public static List<PlaylistEntry> buildPlaylist(AutoplayControllerBlockEntity controller)
    {
        Level lvl = controller.getLevel();
        List<PlaylistEntry> playlist = new ArrayList<>();
        if (lvl == null) return playlist;
        Set<BlockPos> linkedRackPositions = controller.getLinkedRackPositions();
        List<DiscIdentity> customQueueOrder = controller.getCustomQueue();
        // Keep insertion order stable so newly discovered discs append predictably.
        Set<DiscIdentity> physicalItemTypes = new LinkedHashSet<>();

        // 1. Scan linked racks
        for (BlockPos rackPos : linkedRackPositions)
        {
            if (!lvl.isLoaded(rackPos)) continue;

            if (lvl.getBlockEntity(rackPos) instanceof DiscRackBlockEntity rack)
            {
                for (int i = 0; i < rack.getContainerSize(); i++)
                {
                    ItemStack stack = rack.getItem(i);
                    if (stack.isEmpty()) continue;

                    playlist.add(new PlaylistEntry(rackPos, i, stack.copy()));
                    physicalItemTypes.add(DiscIdentityHelper.get(stack));
                }
            }
        }
        // 2. Add the disc currently in the jukebox, if any
        if (controller.currentlyPlayingEntry != null)
        {
            playlist.add(controller.currentlyPlayingEntry);
            physicalItemTypes.add(DiscIdentityHelper.get(controller.currentlyPlayingEntry.stack()));
        }
        else
        {
            BlockPos jukeboxPos = JukeboxHelper.findJukebox(controller);
            if (jukeboxPos != null && lvl.getBlockEntity(jukeboxPos) instanceof JukeboxBlockEntity jukebox)
            {
                ItemStack manualDisc = jukebox.getTheItem();
                if (!manualDisc.isEmpty())
                {
                    playlist.add(new PlaylistEntry(BlockPos.ZERO, -1, manualDisc.copy()));
                    physicalItemTypes.add(DiscIdentityHelper.get(manualDisc));
                }
            }
        }
        // 3. Keep queue order synced to what is physically present
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
        // 4. Sort by custom queue order first, then by rack position and slot
        playlist.sort((a, b) ->
        {
            DiscIdentity idA = DiscIdentityHelper.get(a.stack());
            DiscIdentity idB = DiscIdentityHelper.get(b.stack());

            int indexA = customQueueOrder.indexOf(idA);
            int indexB = customQueueOrder.indexOf(idB);

            boolean aQueued = indexA != -1;
            boolean bQueued = indexB != -1;

            if (aQueued && bQueued)
            {
                int queueComp = Integer.compare(indexA, indexB);
                if (queueComp != 0) return queueComp;
            }
            else if (aQueued != bQueued)
            {
                return aQueued ? -1 : 1;
            }
            int rackCompare = a.rackPos().compareTo(b.rackPos());
            if (rackCompare != 0) return rackCompare;
            return Integer.compare(a.slotIndex(), b.slotIndex());
        });
        return playlist;
    }

    public static List<DisplayPlaylistEntry> buildCollapsedPlaylist(AutoplayControllerBlockEntity controller)
    {
        Map<DiscIdentity, DisplayPlaylistEntry> map = new LinkedHashMap<>();

        for (PlaylistEntry entry : buildPlaylist(controller))
        {
            ItemStack stack = entry.stack();
            DiscIdentity id = DiscIdentityHelper.get(stack);

            if (map.containsKey(id))
            {
                DisplayPlaylistEntry existing = map.get(id);
                map.put(id, new DisplayPlaylistEntry(existing.stack(), existing.count() + 1));
            }
            else
            {
                map.put(id, new DisplayPlaylistEntry(stack.copy(), 1));
            }
        }

        return new ArrayList<>(map.values());
    }

    public static final class DiscIdentityHelper
    {
        private DiscIdentityHelper() {}

        public static DiscIdentity get(ItemStack stack)
        {
            ResourceLocation item = BuiltInRegistries.ITEM.getKey(stack.getItem());
            // Etched discs become unique by URL, not just by item type
            if (LinkHelper.hasComponentByString(stack, "etched:music"))
            {
                String url = EtchedBridge.getEtchedUrl(stack);
                if (url != null && !url.isBlank())
                {
                    return new DiscIdentity(item, url);
                }
            }

            return new DiscIdentity(item, null);
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