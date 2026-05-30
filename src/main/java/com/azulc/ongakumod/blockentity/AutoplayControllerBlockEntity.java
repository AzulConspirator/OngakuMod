package com.azulc.ongakumod.blockentity;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.network.SyncPlaylistPayload;

public class AutoplayControllerBlockEntity extends BlockEntity 
{
    private final Set<BlockPos> linkedRackPositions = new HashSet<>();
    private final Set<BlockPos> linkedSpeakers = new HashSet<>();
    private int tickCounter = 0;
    public PlaylistEntry currentlyPlayingEntry = null;
    private int currentPlaylistIndex = -1;
    private long songStartTick = -1;
    private int songDurationTicks = 0;
    public record PlaylistEntry( BlockPos rackPos,int slotIndex,ItemStack stack) {}
    public record DisplayPlaylistEntry( ItemStack stack, int count) {}
    private boolean autoplayEnabled = false;
    public int cachedStatus = 0;
    private final List<Item> customQueueOrder = new ArrayList<>();
    private final Set<Item> excludedTracks = new HashSet<>();

    public AutoplayControllerBlockEntity(BlockPos pos, BlockState state) 
    {
        super(OngakuMod.AUTOPLAY_BLOCK_ENTITY.get(), pos, state);
    }
    //#region Container Data
    public final ContainerData data = new ContainerData() 
    {
        @Override
        public int get(int index) 
        {
            return switch (index) 
            {
                case 0 -> linkedRackPositions.isEmpty() ? 0 : 1;
                case 1 ->
                {
                    BlockPos jukePos = findJukebox();
                    if (jukePos == null) yield -1;
                    if (level.getBlockEntity(jukePos) instanceof JukeboxBlockEntity jukebox) {
                        ItemStack playing = jukebox.getTheItem();
                        if (playing.isEmpty()) yield -1;
                        List<DisplayPlaylistEntry> displayList = buildCollapsedPlaylist();
                        for (int i = 0; i < displayList.size(); i++) {
                            if (ItemStack.isSameItem(displayList.get(i).stack(), playing)) {
                                yield i;
                            }
                        }
                    }
                    yield -1;
                }
                case 2 -> cachedStatus;
                case 3 ->  
                {
                    yield 0;
                }
                case 4 -> autoplayEnabled ? 1 : 0;
                default -> 0;
            };
        }
        @Override
        public void set(int index, int value) 
        {
            if (index == 1) {
                currentPlaylistIndex = value;
            }
            if (index == 2) {
                cachedStatus = value;
                // Trigger a block update to sync NBT to the client renderer
                AutoplayControllerBlockEntity.this.setChanged();
                if (level != null && !level.isClientSide) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
            }
            if (index == 4) {
                autoplayEnabled = (value == 1);
                // CRITICAL: Mark for sync so the UI button updates for everyone
                AutoplayControllerBlockEntity.this.setChanged();
                if (level != null && !level.isClientSide) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
            }
        }
        @Override
        public int getCount() 
        {
            return 5;
        }
    };
    //#endregion
    //#region Linked Rack Handler
    public boolean addLinkedRack(BlockPos rackPos) 
    {
        if (level.getBlockEntity(rackPos) instanceof DiscRackBlockEntity rack) 
        {
            BlockPos existingController = rack.getControllerPos();
            // CASE 1: TOGGLE OFF - If already linked to THIS controller, disconnect it
            if (this.worldPosition.equals(existingController)) {
                this.removeLinkedRack(rackPos);
                return false; // Return false to indicate "Disconnected"
            }
            // CASE 2: REPLACE - If linked to a DIFFERENT controller, force a transfer
            if (existingController != null) {
                if (level.getBlockEntity(existingController) instanceof AutoplayControllerBlockEntity oldController) 
                {
                    // Tell the OLD controller to forget this rack
                    oldController.removeLinkedRack(rackPos);
                }
            }
            // CASE 3: CONNECT - Establish the new link
            rack.setControllerPos(this.worldPosition);
            this.linkedRackPositions.add(rackPos);
            // Finalize sync
            this.setChanged();
            level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
            rack.setChanged();
            level.sendBlockUpdated(rackPos, rack.getBlockState(), rack.getBlockState(), 3);
            return true; 
        }
        return false;
    }

    public void removeLinkedRack(BlockPos rackPos) {
    // 1. Remove from Controller's memory
        boolean removed = this.linkedRackPositions.remove(rackPos);
        
        // 2. IMPORTANT: Clear the Rack's internal memory
        if (level.getBlockEntity(rackPos) instanceof DiscRackBlockEntity rack) {
            // Only clear it if the rack actually thinks it belongs to THIS controller
            if (this.worldPosition.equals(rack.getControllerPos())) {
                rack.setControllerPos(null);
                rack.setChanged();
                level.sendBlockUpdated(rackPos, rack.getBlockState(), rack.getBlockState(), 3);
            }
        }

        // 3. Refresh playlist and sync
        if (removed) {
            this.buildPlaylist(); 
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
                broadcastPlaylistUpdate(); 
            }
            this.setChanged();
        }
    }

    public void clearLinkedRacks() {
        this.linkedRackPositions.clear();
        this.setChanged();
    }
    //#endregion
    //#region Update Tag
    // 1. Send the data to the client when the chunk loads
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries); 
        return tag;
    }

    // 2. Create the packet that carries the data
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // 3. (Optional but safer) Explicitly handle the packet on the client
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            loadAdditional(tag, registries);
        }
    }
    //#endregion
    //#region Server Tick
    public static void serverTick(Level level, BlockPos pos, BlockState state, AutoplayControllerBlockEntity entity) 
    {
        entity.tickCounter++;
        // Run validation every 20 ticks (1 second) to save performance
        if (entity.tickCounter % 20 == 0) {
            entity.validateAndProcess(level,pos);
        }
        // 2. The Autoplay Engine (Runs every tick)
        if (entity.autoplayEnabled) {
            // CASE 1: Something is playing, wait for it to end
            if (entity.songStartTick != -1 && entity.songDurationTicks > 0) {
                long elapsed = level.getGameTime() - entity.songStartTick;
                if (elapsed >= entity.songDurationTicks + 10) {
                    entity.playNextInQueue();
                }
            } 
            // CASE 2: FIX - Nothing is playing, but Autoplay is ON. Start the first disc!
            else if (entity.songStartTick == -1) {
                entity.playNextInQueue();
            }
        }
    }

    private void playNextInQueue() {
        List<PlaylistEntry> fullPlaylist = buildPlaylist();
        if (fullPlaylist.isEmpty()) return;
        int nextIndex = this.currentPlaylistIndex;
        int attempts = 0;
        while (attempts < fullPlaylist.size()) {
            nextIndex = (nextIndex + 1) % fullPlaylist.size();
            attempts++;

            ItemStack nextStack = fullPlaylist.get(nextIndex).stack();
            if (!excludedTracks.contains(nextStack.getItem())) {
                tryPlayDisc(nextIndex);
                return;
            }
        }
        this.stopAndReturnDisc();
    }

    private void validateAndProcess(Level level,BlockPos pos) 
    {
        if (this.linkedRackPositions.isEmpty()) return;
        Iterator<BlockPos> iterator = this.linkedRackPositions.iterator();
        while (iterator.hasNext()) {
            BlockPos rackPos = iterator.next();
            if (!level.isLoaded(rackPos)) {
                continue;
            }
            BlockEntity targetBE = level.getBlockEntity(rackPos);
            if (!(targetBE instanceof DiscRackBlockEntity)) {
                iterator.remove();
                this.setChanged();
            }
        }
        int newStatus = -1;
        BlockPos jukeboxPos = findJukebox();
        if (jukeboxPos != null && level.getBlockEntity(jukeboxPos) instanceof JukeboxBlockEntity jukebox) {
                if (jukebox.getBlockState().getValue(JukeboxBlock.HAS_RECORD)) {
                    // Server-side isPlaying() is reliable [cite: 289]
                    newStatus = jukebox.getSongPlayer().isPlaying() ? 1 : 0;
                } else {
                    newStatus = 0; // Idle [cite: 291]
                }
            }
        if (newStatus != 1)
        {
            broadcastToSpeakers(false, null);
        }
        // Only sync if the status actually changed to save bandwidth
        if (this.cachedStatus != newStatus) {
            this.cachedStatus = newStatus;
            this.setChanged();
            if (level instanceof ServerLevel) {
                level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }
    //#endregion
    //#region NBT Data Storage
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) 
    {
        super.saveAdditional(tag, registries);
        ListTag queueTag = new ListTag();
        ListTag exclusionTag = new ListTag();
        long[] positions = linkedRackPositions.stream().mapToLong(BlockPos::asLong).toArray();
        long[] Speakerpositions = linkedSpeakers.stream().mapToLong(BlockPos::asLong).toArray();
        // Save Queue as a List of Strings (Registry Names)
        for (Item item : customQueueOrder) {
            queueTag.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(item).toString()));
        }
        // Save Exclusions
        for (Item item : excludedTracks) {
            exclusionTag.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(item).toString()));
        }
        tag.putLongArray("LinkedRacks", positions);
        tag.putLongArray("linkedSpeakers", Speakerpositions);
        tag.putLong("SongStart", songStartTick);
        tag.putInt("SongDuration", songDurationTicks);
        tag.putInt("CurrentPlayingSlot", currentPlaylistIndex);
        tag.putBoolean("autoplayEnabled", this.autoplayEnabled);
        tag.put("excludedTracks", exclusionTag);
        tag.put("queueOrder", queueTag);
        tag.putInt("CachedStatus", this.cachedStatus);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) 
    {
        super.loadAdditional(tag, registries);
        linkedRackPositions.clear();
        long[] _positions = tag.getLongArray("LinkedRacks");
        for (long posLong : _positions) {
            linkedRackPositions.add(BlockPos.of(posLong));
        }
        linkedSpeakers.clear();
        long[] _linkedSpeakers = tag.getLongArray("linkedSpeakers");
        for (long posLong : _linkedSpeakers) {
            linkedSpeakers.add(BlockPos.of(posLong));
        }
        songStartTick = tag.getLong("SongStart");
        songDurationTicks = tag.getInt("SongDuration");
        currentPlaylistIndex = tag.getInt("CurrentPlayingSlot");
        this.cachedStatus = tag.getInt("CachedStatus");
        this.autoplayEnabled = tag.getBoolean("autoplayEnabled");
        customQueueOrder.clear();
        ListTag queueTag = tag.getList("queueOrder", 8); // 8 is the ID for StringTag
        for (int i = 0; i < queueTag.size(); i++) 
        {
            ResourceLocation rl = ResourceLocation.parse(queueTag.getString(i));
            customQueueOrder.add(BuiltInRegistries.ITEM.get(rl));
        }
        excludedTracks.clear();
        ListTag exclusionTag = tag.getList("excludedTracks", 8);
        for (int i = 0; i < exclusionTag.size(); i++) 
        {
            ResourceLocation rl = ResourceLocation.parse(exclusionTag.getString(i));
            excludedTracks.add(BuiltInRegistries.ITEM.get(rl));
        }
    }

    //#endregion
    //#region Helpers 
    public boolean isItemExcluded(Item item)
    {
        return this.excludedTracks.contains(item);
    }
    public List<Item> getCustomQueue() {
        return this.customQueueOrder;
    }

    public long getSongStartTick() {
        return songStartTick;
    }

    public int getSongDurationTicks() {
        return songDurationTicks;
    }
    private void finalizeReturn(DiscRackBlockEntity rack) {
        rack.setChanged();
        level.sendBlockUpdated(rack.getBlockPos(), rack.getBlockState(), rack.getBlockState(), 3);
        currentlyPlayingEntry = null;
        currentPlaylistIndex = -1;
        this.setChanged();
    }

    public DiscRackBlockEntity getRack(BlockPos pos) 
    {
        if (level == null) { return null; }
        if (!level.isLoaded(pos)) {return null;}

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DiscRackBlockEntity rack) {
            return rack;
        }
        return null;
    }
    
    public Set<BlockPos> getLinkedRackPositions() {
        return linkedRackPositions;
    }

    @SuppressWarnings("unused")
    private void reconcileJukeboxState(ItemStack playing) {
        for (BlockPos rackPos : linkedRackPositions) {
            DiscRackBlockEntity rack = getRack(rackPos);
            if (rack == null) continue;

            for (int i = 0; i < rack.getContainerSize(); i++) {
                if (rack.getItem(i).isEmpty()) {
                    this.currentlyPlayingEntry = new PlaylistEntry(rackPos, i, playing.copy());
                    this.setChanged();
                    return; 
                }
            }
        }
    }
    private void clearJukebox(JukeboxBlockEntity jukebox, BlockPos pos) 
    {
        ItemStack existing = jukebox.getTheItem();
        if (!existing.isEmpty()) {
            returnDiscToRack(existing);
            jukebox.setTheItem(ItemStack.EMPTY);
        }
        
        BlockState state = level.getBlockState(pos);
        BlockState oldState = state;
        BlockState newState = state.setValue(JukeboxBlock.HAS_RECORD, false);
        level.setBlock(pos, newState, 3);
        level.sendBlockUpdated(pos,oldState,newState,3);
        jukebox.getSongPlayer().stop(level, state);
        this.currentPlaylistIndex = -1;
    }

    public BlockPos findJukebox() {
        BlockPos jukeboxPos = null;
        for (Direction dir : Direction.values()) {
            BlockPos adjacent = worldPosition.relative(dir);
            if (level.getBlockState(adjacent).is(Blocks.JUKEBOX)) {
                jukeboxPos = adjacent;
                break;
            }
        }
        return jukeboxPos;
    }

    //#endregion
    //#region Main Packet
    private void returnDiscToRack(ItemStack existingDisc)
    {
        if (existingDisc.isEmpty()) return;
        // 1. Try the "Remembered" slot first
        if (currentlyPlayingEntry != null) {
            DiscRackBlockEntity originalRack = getRack(currentlyPlayingEntry.rackPos());
            if (originalRack != null && originalRack.getItem(currentlyPlayingEntry.slotIndex()).isEmpty()) {
                originalRack.setItem(currentlyPlayingEntry.slotIndex(), existingDisc.copy());
                finalizeReturn(originalRack);
                return;
            }
        }
        // 2. Emergency Search: Try to find ANY empty slot in ANY linked rack
        for (BlockPos rackPos : linkedRackPositions) {
            DiscRackBlockEntity rack = getRack(rackPos);
            if (rack == null) continue;
            for (int i = 0; i < rack.getContainerSize(); i++) {
                if (rack.getItem(i).isEmpty()) {
                    rack.setItem(i, existingDisc.copy());
                    finalizeReturn(rack);
                    return;
                }
            }
        }
        Block.popResource(level, worldPosition, existingDisc);
        currentlyPlayingEntry = null;
        currentPlaylistIndex = -1;
    }

    public void tryPlayDisc(int playlistIndex) {
        if (level == null || level.isClientSide) {
            return;
        }
        List<PlaylistEntry> playlist = buildPlaylist();
        if (playlistIndex < 0 || playlistIndex >= playlist.size()) {
            return;
        }
        PlaylistEntry entry = playlist.get(playlistIndex);
        DiscRackBlockEntity rack = getRack(entry.rackPos());
        if (rack == null) {
            return;
        }
        ItemStack discInRack = rack.getItem(entry.slotIndex());
        if (discInRack.isEmpty()) {
            return;
        }
        BlockPos jukeboxPos = findJukebox();
        if (jukeboxPos == null) {
            return;
        }
        if (!(level.getBlockEntity(jukeboxPos) instanceof JukeboxBlockEntity jukebox)) {
            return;
        }
        clearJukebox(jukebox, jukeboxPos);
        ItemStack refreshedDisc = rack.getItem(entry.slotIndex());
        if (refreshedDisc.isEmpty()) {
            return;
        }
        ItemStack discCopy = refreshedDisc.split(1);
        jukebox.setTheItem(discCopy);
        this.songDurationTicks = jukebox.getSongPlayer().getSong().lengthInTicks();
        this.songStartTick = level.getGameTime();

        currentPlaylistIndex = playlistIndex;
        currentlyPlayingEntry = new PlaylistEntry(
                entry.rackPos(),
                entry.slotIndex(),
                discCopy.copy()
        );
        BlockState state = level.getBlockState(jukeboxPos);
        BlockState oldState = state;
        BlockState newState = state;
        if (state.hasProperty(JukeboxBlock.HAS_RECORD)) {
            newState = state.setValue(JukeboxBlock.HAS_RECORD, true);
            level.setBlock(jukeboxPos, newState, 3);
        }
        broadcastToSpeakers(true, discCopy);
        rack.setChanged();
        level.sendBlockUpdated(rack.getBlockPos(),rack.getBlockState(),rack.getBlockState(),3);
        level.levelEvent(null, 1010, jukeboxPos, Item.getId(discCopy.getItem()));
        jukebox.setChanged();
        this.setChanged();
        level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        level.sendBlockUpdated(jukeboxPos,oldState,newState,3);
        broadcastPlaylistUpdate();
    }

    public void stopAndReturnDisc() {
        if (level == null || level.isClientSide) return;
        
        BlockPos jukeboxPos = findJukebox();
        if (jukeboxPos != null && level.getBlockEntity(jukeboxPos) instanceof JukeboxBlockEntity jukebox) {
            // Use the helper ONLY. It handles returning disc, clearing item, and stopping sound.
            clearJukebox(jukebox, jukeboxPos);
            this.songStartTick = -1;
            this.songDurationTicks = 0;
            jukebox.setChanged();
            this.setChanged();
            // Notify the client that the jukebox is now empty
            level.sendBlockUpdated(jukeboxPos, level.getBlockState(jukeboxPos), level.getBlockState(jukeboxPos), 3);
            level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
            broadcastToSpeakers(false,null);
            broadcastPlaylistUpdate();
        }      
    }
    public void StopJukebox() {
        if (level == null || level.isClientSide) return;
        this.autoplayEnabled = false;
        this.stopAndReturnDisc();
    }
    //#endregion
    //#region playlist Handler
    // New method to handle the Skip button
    public void skipTrack() {
        if (level == null || level.isClientSide) return;
        // Simply trigger the existing queue logic
        this.playNextInQueue();
    }
    public List<PlaylistEntry> buildPlaylist() {
        List<PlaylistEntry> playlist = new ArrayList<>();
        if (level == null) return playlist;

        // Physical scan of racks
        for (BlockPos rackPos : linkedRackPositions) {
            if (!level.isLoaded(rackPos)) continue;
            if (level.getBlockEntity(rackPos) instanceof DiscRackBlockEntity rack) {
                for (int i = 0; i < rack.getContainerSize(); i++) {
                    ItemStack stack = rack.getItem(i);
                    if (!stack.isEmpty()) {
                        playlist.add(new PlaylistEntry(rackPos, i, stack.copy()));
                    }
                }
            }
        }

        // Add the disc currently in the jukebox so it doesn't "vanish" from the list
        if (this.currentlyPlayingEntry != null) {
            playlist.add(this.currentlyPlayingEntry);
        }

        // SORT using customQueueOrder
        playlist.sort((a, b) -> {
            int indexA = customQueueOrder.indexOf(a.stack().getItem());
            int indexB = customQueueOrder.indexOf(b.stack().getItem());
            if (indexA != -1 && indexB != -1) return Integer.compare(indexA, indexB);
            if (indexA != -1) return -1;
            if (indexB != -1) return 1;
            return a.rackPos().compareTo(b.rackPos());
        });

        return playlist;
    }

    public List<DisplayPlaylistEntry> buildCollapsedPlaylist() 
    {
        Map<String, DisplayPlaylistEntry> map = new LinkedHashMap<>();
        for (PlaylistEntry entry : buildPlaylist()) 
        {
            ItemStack stack = entry.stack();
            String key = stack.getItem().toString();

            if (map.containsKey(key)) 
            {
                DisplayPlaylistEntry existing = map.get(key);
                map.put(key,new DisplayPlaylistEntry(existing.stack(),existing.count() + 1));
            } 
            else 
            {
                map.put(key,new DisplayPlaylistEntry(stack.copy(),1));
            }
        }
        broadcastPlaylistUpdate();
        return new ArrayList<>(map.values());
    }

    // 3. Update broadcast to use the SORTED list
    public void broadcastPlaylistUpdate() {
    if (level == null || level.isClientSide) return;

    List<ItemStack> sortedStacks = new ArrayList<>();
    for (PlaylistEntry entry : buildPlaylist()) {
        sortedStacks.add(entry.stack());
    }

    PacketDistributor.sendToPlayersTrackingChunk(
        (ServerLevel) level, 
        new ChunkPos(worldPosition), 
        new SyncPlaylistPayload(sortedStacks)
    );
}

    public void toggleAutoplay() 
    {
        this.autoplayEnabled = !this.autoplayEnabled;
        // Mark as changed so it saves to disk
        this.setChanged();
        
        // Sync to clients so the UI button updates immediately
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    public void moveInQueue(Item item, int direction) {
        int index = customQueueOrder.indexOf(item);
        
        // FIX: If the item is new to the system, add it to the end of the queue first
        if (index == -1) {
            customQueueOrder.add(item);
            index = customQueueOrder.size() - 1;
        }
        int newIndex = index + direction;
        // Prevent moving out of bounds (above top or below bottom)
        if (newIndex >= 0 && newIndex < customQueueOrder.size()) {
            Collections.swap(customQueueOrder, index, newIndex);
            // CRITICAL: Immediately broadcast the new order to the UI
            this.setChanged();
            this.broadcastPlaylistUpdate();
        }
    }

    public void toggleExclusion(Item item) {
        if (excludedTracks.contains(item)) {
            excludedTracks.remove(item);
        } else {
            excludedTracks.add(item);
        }
    }
    //#endregion
    //#region Speaker handler
    public void broadcastToSpeakers(boolean isPlaying, @Nullable ItemStack disc) {
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
                    removeLinkedSpeaker(speakerPos);
                }
            }
        }
    }

    // Helper to get the correct Registry ID for the song
    private int getSongId(Level level, ItemStack stack) {
        var songHolder = stack.get(DataComponents.JUKEBOX_PLAYABLE);
        if (songHolder != null) 
        {   
            var E = level.registryAccess().registryOrThrow(Registries.JUKEBOX_SONG).getId(songHolder.song().key());
            OngakuMod.LOGGER.info("ID is " + E);
            return E;
        }
        return -1;
    }

    // 2. Stop jukebox on connection & update speaker state 
    public boolean addLinkedSpeaker(BlockPos speakerPos) {
        if (level == null || level.isClientSide) return false;

        // Stop music whenever a connection state changes (as requested)
        this.StopJukebox(); 
        if (linkedSpeakers.contains(speakerPos)) {
            linkedSpeakers.remove(speakerPos);
            if (level.getBlockEntity(speakerPos) instanceof SpeakerBlockEntity speaker) {
                speaker.setControllerPos(null);
                speaker.setPlaying(false); // Stop particles immediately
            }
            this.setChanged();
            return false;
        } else {
            linkedSpeakers.add(speakerPos);
            if (level.getBlockEntity(speakerPos) instanceof SpeakerBlockEntity speaker) {
                speaker.setControllerPos(this.worldPosition);
                // Don't set playing to true yet; wait for the next song to start
            }
            this.setChanged();
            return true;
        }
    }

    public void removeLinkedSpeaker(BlockPos speakerPos) {
        boolean removed = this.linkedSpeakers.remove(speakerPos);
        if (level.getBlockEntity(speakerPos) instanceof SpeakerBlockEntity rack) {
            if (this.worldPosition.equals(rack.getControllerPos())) {
                rack.setControllerPos(null);
                rack.setChanged();
                level.sendBlockUpdated(speakerPos, rack.getBlockState(), rack.getBlockState(), 3);
            }
        }
        if (removed) {
            broadcastToSpeakers(false, null);
            this.setChanged();
        }
    }
    public Set<BlockPos> getLinkedSpeakerPositions() {
        return linkedSpeakers;
    }
    //#endregion


}