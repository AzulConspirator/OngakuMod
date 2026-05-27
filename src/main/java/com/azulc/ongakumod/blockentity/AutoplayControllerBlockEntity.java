package com.azulc.ongakumod.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.network.SyncPlaylistPayload;

public class AutoplayControllerBlockEntity extends BlockEntity 
{
    private final Set<BlockPos> linkedRackPositions = new HashSet<>();
    private int tickCounter = 0;
    private PlaylistEntry currentlyPlayingEntry = null;
    private int currentPlaylistIndex = -1;
    public record PlaylistEntry( BlockPos rackPos,int slotIndex,ItemStack stack) {}
    public record DisplayPlaylistEntry( ItemStack stack, int count) {}

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

                            // Match the disc in the jukebox back to our list
                            // buildCollapsedPlaylist() MUST include the jukebox item for this to work
                            List<DisplayPlaylistEntry> displayList = buildCollapsedPlaylist();
                            for (int i = 0; i < displayList.size(); i++) {
                                if (ItemStack.isSameItem(displayList.get(i).stack(), playing)) {
                                    yield i;
                                }
                            }
                        }
                        yield -1;
                    }
                case 2 -> 
                        {
                            BlockPos jukePos = findJukebox();
                            if (jukePos == null) yield -1;
                            if (level.getBlockEntity(jukePos) instanceof JukeboxBlockEntity jukebox) {
                                yield jukebox.getTheItem().isEmpty() ? 0 : 1;
                            }
                            yield -1;
                        }
                default -> 0;
            };
        }
        @Override
        public void set(int index, int value) 
        {
            if (index == 1) {
                currentPlaylistIndex = value;
            }
        }
        @Override
        public int getCount() 
        {
            return 3;
        }
    };
    //#endregion
    //#region Linked Rack Handler
    public boolean addLinkedRack(BlockPos rackPos) 
    {
        if (level.getBlockEntity(rackPos) instanceof DiscRackBlockEntity rack) 
        {
            BlockPos existingController = rack.getControllerPos();
            if (existingController != null && !existingController.equals(worldPosition)) {
                // Optional: Tell the old controller to remove this rack
                if (level.getBlockEntity(existingController) instanceof AutoplayControllerBlockEntity oldController) {
                    oldController.removeLinkedRack(rackPos);
                }
            }
            rack.setControllerPos(worldPosition);
            this.linkedRackPositions.add(rackPos);
            this.setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            return true;
        }
        return false;
    }

    public void removeLinkedRack(BlockPos pos) {
        this.linkedRackPositions.remove(pos);
        this.setChanged();
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
    public static void serverTick(Level level, BlockPos pos, BlockState state, AutoplayControllerBlockEntity entity) {
        entity.tickCounter++;
        // Run validation every 20 ticks (1 second) to save performance
        if (entity.tickCounter % 20 == 0) {
            entity.validateAndProcess(level,pos);
        }
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

        BlockPos jukeboxPos = findJukebox();

        if (jukeboxPos != null && level.getBlockEntity(jukeboxPos) instanceof JukeboxBlockEntity jukebox) 
        {
            ItemStack playing = jukebox.getTheItem();
            // QoL Patch: If jukebox has a disc but the controller thinks it's empty, try to claim it
            if (!playing.isEmpty() && currentlyPlayingEntry == null) {
                reconcileJukeboxState(playing);
            }
            this.data.set(2, playing.isEmpty() ? 0 : 1);// 0 = Empty, 1 = Has Disc
        } 
        else 
        {
            this.data.set(2, -1); // -1 = No Jukebox Found
        }
    }
    //#endregion
    //#region NBT Data Storage
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        long[] positions = linkedRackPositions.stream()
                .mapToLong(BlockPos::asLong)
                .toArray();

        tag.putLongArray("LinkedRacks", positions);

        tag.putInt("CurrentPlayingSlot", currentPlaylistIndex);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        linkedRackPositions.clear();

        long[] positions = tag.getLongArray("LinkedRacks");

        for (long posLong : positions) {
            linkedRackPositions.add(BlockPos.of(posLong));
        }

        currentPlaylistIndex = tag.getInt("CurrentPlayingSlot");
    }

    //#endregion
    //#region Helpers 

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
    public int getCurrentProgressFrame()
    {
        if (level == null) return 0;

        BlockPos jukeboxPos = findJukebox();
        if (jukeboxPos == null) return 0;

        if (level.getBlockEntity(jukeboxPos) instanceof JukeboxBlockEntity jukebox) {
            var Songplayer = jukebox.getSongPlayer();
            
            // If it's not playing or the song holder is missing, show frame 0 (empty)
            if (!Songplayer.isPlaying() || Songplayer.getSong() == null) {
                return 0;
            }

            // Current progress in ticks
            long currentTicks = Songplayer.getTicksSinceSongStarted();
            
            // Total duration of the song in ticks
            // JukeboxSong is a record containing 'lengthInTicks'
            float totalTicks = jukebox.getSongPlayer().getSong().lengthInTicks();

            if (totalTicks <= 0) return 0;

            // Calculate the ratio (0.0 to 1.0)
            float progress = (float) currentTicks / totalTicks;

            // Map progress to our 13 frames (0 to 12)
            // Using Math.round helps prevent the bar from feeling "late"
            int frame = Math.round(progress * 12);

            // Clamp the result to ensure we don't exceed texture bounds
            return Math.max(0, Math.min(12, frame));
        }

        return 0;
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
        rack.setChanged();
        level.sendBlockUpdated(rack.getBlockPos(),rack.getBlockState(),rack.getBlockState(),3);
        jukebox.setChanged();
        this.setChanged();
        level.sendBlockUpdated(jukeboxPos,oldState,newState,3);
        broadcastPlaylistUpdate();
    }

    public void stopAndReturnDisc() {
        if (level == null || level.isClientSide) return;
        
        BlockPos jukeboxPos = findJukebox();
        if (jukeboxPos != null && level.getBlockEntity(jukeboxPos) instanceof JukeboxBlockEntity jukebox) {
            // Use the helper ONLY. It handles returning disc, clearing item, and stopping sound.
            clearJukebox(jukebox, jukeboxPos);
            
            jukebox.setChanged();
            this.setChanged();
            // Notify the client that the jukebox is now empty
            level.sendBlockUpdated(jukeboxPos, level.getBlockState(jukeboxPos), level.getBlockState(jukeboxPos), 3);
            broadcastPlaylistUpdate();
        }      
    }
    //#endregion
    //#region playlist Handler
    public List<PlaylistEntry> buildPlaylist() {
        List<PlaylistEntry> playlist = new ArrayList<>();
        if (level == null) return playlist;

        // 1. Add discs from Racks
        for (BlockPos rackPos : linkedRackPositions) {
            if (!level.isLoaded(rackPos)) continue;
            BlockEntity be = level.getBlockEntity(rackPos);
            if (!(be instanceof DiscRackBlockEntity rack)) continue;
            for (int i = 0; i < rack.getContainerSize(); i++) {
                ItemStack stack = rack.getItem(i);
                if (!stack.isEmpty()) {
                    playlist.add(new PlaylistEntry(rackPos, i, stack.copy()));
                }
            }
        }

        // 2. ADD THIS: Include the disc currently in the jukebox
        BlockPos jukePos = findJukebox();
        if (jukePos != null && level.getBlockEntity(jukePos) instanceof JukeboxBlockEntity jukebox) {
            ItemStack inJukebox = jukebox.getTheItem();
            if (!inJukebox.isEmpty()) {
                // Check if this disc is already accounted for via currentlyPlayingEntry
                boolean alreadyInList = false;
                if (currentlyPlayingEntry != null) {
                    // Check if the slot it 'came from' is currently empty (it should be)
                    // If it's already in our loop above, we don't add it again.
                    // However, if the rack is FULL, the loop above won't find it.
                    alreadyInList = playlist.stream().anyMatch(e -> 
                        e.rackPos().equals(currentlyPlayingEntry.rackPos()) && 
                        e.slotIndex() == currentlyPlayingEntry.slotIndex());
                }

                if (!alreadyInList) {
                    // Use a dummy position (worldPosition) if it didn't come from a rack
                    // This ensures it shows up in the UI list
                    BlockPos sourcePos = (currentlyPlayingEntry != null) ? currentlyPlayingEntry.rackPos() : worldPosition;
                    int sourceSlot = (currentlyPlayingEntry != null) ? currentlyPlayingEntry.slotIndex() : -1;
                    playlist.add(new PlaylistEntry(sourcePos, sourceSlot, inJukebox.copy()));
                }
            }
        }
        // Sort remains the same
        playlist.sort((a, b) -> {
            int posCompare = a.rackPos().compareTo(b.rackPos());
            return posCompare != 0 ? posCompare : Integer.compare(a.slotIndex(), b.slotIndex());
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

    public void broadcastPlaylistUpdate() {
        if (level == null || level.isClientSide) {
            return;
        }
        List<ItemStack> currentDiscs = new ArrayList<>();
        for (PlaylistEntry entry : buildPlaylist()) {
            currentDiscs.add(entry.stack().copy());
        }

        PacketDistributor.sendToPlayersTrackingChunk(
                (ServerLevel) level,
                new ChunkPos(worldPosition),
                new SyncPlaylistPayload(currentDiscs)
        );
    }
    //#endregion
}