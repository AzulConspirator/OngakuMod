package com.azulc.ongakumod.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
                case 1 -> currentPlaylistIndex;
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
    public boolean addLinkedRack(BlockPos pos) 
    {
        if (this.linkedRackPositions.contains(pos))
        {
            return false;
        }
        this.linkedRackPositions.add(pos);
        this.setChanged();
        return true;
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
        //if (this.linkedRackPos == null) return; // Not linked yet
        if (this.linkedRackPositions.isEmpty()) return;
        // 2. Is it actually a Disc Rack?
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

        BlockPos jukeboxPos = null;
        // Find adjacent jukebox
        for (Direction dir : Direction.values()) 
        {
            if (level.getBlockState(pos.relative(dir)).is(Blocks.JUKEBOX)) 
            {
                jukeboxPos = pos.relative(dir);
                break;
            }
        }

        if (jukeboxPos != null && level.getBlockEntity(jukeboxPos) instanceof JukeboxBlockEntity jukebox) 
        {
            ItemStack playing = jukebox.getTheItem();
            this.data.set(2, playing.isEmpty() ? 0 : 1);// 0 = Empty, 1 = Has Disc
            
            // BONUS: If you want the actual name, we'd need a custom packet.
            // For now, let's just stick to the OK/Empty status.
        } 
        else 
        {
            this.data.set(2, -1); // -1 = No Jukebox Found
        }
    }
    //#endregion
    //#region NBT SAVING & LOADING
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

    private BlockPos findJukebox() {
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
    private void returnDiscToRack(ItemStack existingDisc) {
        if (existingDisc.isEmpty()) { return; }
        if (currentlyPlayingEntry == null) 
        {
            Block.popResource(level, worldPosition, existingDisc);
            return;
        }
        DiscRackBlockEntity rack =getRack(currentlyPlayingEntry.rackPos());
        if (rack == null) {
            Block.popResource(level, worldPosition, existingDisc);
            return;
        }
        int slot = currentlyPlayingEntry.slotIndex();
        if (slot >= 0 && slot < rack.getContainerSize() && rack.getItem(slot).isEmpty()) {
            rack.setItem(slot, existingDisc.copy());
        } 
        else 
        {
            boolean inserted = false;
            for (int i = 0; i < rack.getContainerSize(); i++) 
            {
                if (rack.getItem(i).isEmpty()) 
                {
                    rack.setItem(i, existingDisc.copy());
                    inserted = true;
                    break;
                }
            }
            if (!inserted)
            {
                Block.popResource(level, worldPosition, existingDisc);
            }
        }
        rack.setChanged();
        level.sendBlockUpdated(rack.getBlockPos(),rack.getBlockState(), rack.getBlockState(), 3);

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
        }
    }
    //#endregion
    //#region playlist Handler
    public List<PlaylistEntry> buildPlaylist() 
        {
        List<PlaylistEntry> playlist = new ArrayList<>();
        if (level == null) 
        {
            return playlist;
        }

        for (BlockPos rackPos : linkedRackPositions) 
        {
            if (!level.isLoaded(rackPos)) 
            {
                continue;
            }
            BlockEntity be = level.getBlockEntity(rackPos);
            if (!(be instanceof DiscRackBlockEntity rack))
            {
                continue;
            }
            for (int i = 0; i < rack.getContainerSize(); i++) 
            {
                ItemStack stack = rack.getItem(i);
                if (!stack.isEmpty()) 
                {
                    playlist.add(new PlaylistEntry(rackPos,i,stack.copy()));
                }
            }
        }
        playlist.sort((a, b) -> 
        {
            int posCompare = a.rackPos().compareTo(b.rackPos());
            if (posCompare != 0)
            {
                return posCompare;
            }
            return Integer.compare(a.slotIndex(), b.slotIndex());
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