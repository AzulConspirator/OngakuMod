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
import java.util.List;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.network.SyncPlaylistPayload;

public class AutoplayControllerBlockEntity extends BlockEntity {
    
    private BlockPos linkedRackPos = null;
    private int tickCounter = 0;
    private int currentPlayingSlot = -1;
    
    public AutoplayControllerBlockEntity(BlockPos pos, BlockState state) {
        super(OngakuMod.AUTOPLAY_BLOCK_ENTITY.get(), pos, state);
    }

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> linkedRackPos != null ? 1 : 0;
                case 1 -> currentPlayingSlot;
                case 2 -> {
                    BlockPos jukePos = findJukebox();
                    if (jukePos == null) yield -1; // No Jukebox
                    if (level.getBlockEntity(jukePos) instanceof JukeboxBlockEntity jukebox) {
                        yield jukebox.getTheItem().isEmpty() ? 0 : 1; // 0 = Connected/Empty, 1 = Connected/Playing
                    }
                    yield -1;
                }
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 1) currentPlayingSlot = value;
            // Index 2 and 0 are read-only dynamic values calculated by the server logic
        }

        @Override
        public int getCount() {
            return 3;
        }
    };
        
    

    // Called by the Block's use() method
    public void setLinkedRack(BlockPos pos) {
        this.linkedRackPos = pos;
        this.setChanged(); // Crucial: Tells the game this needs to be saved
    }

    // The core loop (runs every tick on the server)
    public static void serverTick(Level level, BlockPos pos, BlockState state, AutoplayControllerBlockEntity entity) {
        entity.tickCounter++;
        
        // Run validation every 20 ticks (1 second) to save performance
        if (entity.tickCounter % 20 == 0) {
            entity.validateAndProcess(level,pos);
        }
    }

    private void validateAndProcess(Level level,BlockPos pos) {
        if (this.linkedRackPos == null) return; // Not linked yet

        // 1. Is the chunk loaded? (Don't wake up unloaded chunks)
        if (!level.isLoaded(this.linkedRackPos)) return;

        // 2. Is it actually a Disc Rack?
        BlockEntity targetBE = level.getBlockEntity(this.linkedRackPos);
        if (!(targetBE instanceof DiscRackBlockEntity)) {
            // The rack was broken or replaced! Break the link to prevent crashes.
            this.linkedRackPos = null;
            this.setChanged();
            return;
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

    //#region --- NBT SAVING & LOADING --- //
        @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.linkedRackPos != null) {
            tag.putLong("LinkedRackPos", this.linkedRackPos.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("LinkedRackPos")) {
            this.linkedRackPos = BlockPos.of(tag.getLong("LinkedRackPos"));
        }
    }
    //#endregion
    //#region Helpers 
    public BlockPos getLinkedRackPos() {
        return this.linkedRackPos;
    }

    // Helper to get the actual Rack instance
    public DiscRackBlockEntity getRack(Level level) {
        if (linkedRackPos == null || !level.isLoaded(linkedRackPos)) return null;
        if (level.getBlockEntity(linkedRackPos) instanceof DiscRackBlockEntity rack) {
            return rack;
        }
        return null;
    }
    
    // Add this inside AutoplayControllerBlockEntity
    public void broadcastPlaylistUpdate() {
        if (level == null || level.isClientSide) return;
        DiscRackBlockEntity rack = getRack(level);
        if (rack == null) return;
        
        List<ItemStack> currentDiscs = new ArrayList<>();
        for (int i = 0; i < rack.getContainerSize(); i++) {
            currentDiscs.add(rack.getItem(i).copy());
        }
        
        // Broadcast to anyone looking at this chunk
        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(worldPosition), new SyncPlaylistPayload(currentDiscs));
    }

    public boolean isJukeboxAdjacent() {
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(worldPosition.relative(dir)).is(Blocks.JUKEBOX)) {
                return true;
            }
        }
        return false;
    }

    private void returnDiscToRack(ItemStack existingDisc, int preferredSlot) 
    {
        DiscRackBlockEntity rack = this.getRack(level);
        if (rack == null || existingDisc.isEmpty()) return;

        boolean reinserted = false;
        if (preferredSlot >= 0 && preferredSlot < rack.getContainerSize() && rack.getItem(preferredSlot).isEmpty()) 
        {
            rack.setItem(preferredSlot, existingDisc.copy());
            reinserted = true;
        }
        if (!reinserted) {
            for (int i = 0; i < rack.getContainerSize(); i++) {
                if (rack.getItem(i).isEmpty()) {
                    rack.setItem(i, existingDisc.copy());
                    reinserted = true;
                    break;
                }
            }
        }
        if (!reinserted) {
            Block.popResource(level, worldPosition, existingDisc);
        }

        rack.setChanged();
        level.sendBlockUpdated(rack.getBlockPos(), rack.getBlockState(), rack.getBlockState(), 3);
    }

    private void clearJukebox(JukeboxBlockEntity jukebox, BlockPos pos) {
        ItemStack existing = jukebox.getTheItem();
        if (!existing.isEmpty()) {
            returnDiscToRack(existing, this.currentPlayingSlot);
            jukebox.setTheItem(ItemStack.EMPTY);
        }
        
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(JukeboxBlock.HAS_RECORD)) {
            level.setBlock(pos, state.setValue(JukeboxBlock.HAS_RECORD, false), 3);
        }
        jukebox.getSongPlayer().stop(level, state);
        this.currentPlayingSlot = -1;
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

public void tryPlayDisc(int slotIndex) {
    if (level == null || level.isClientSide) return;

    DiscRackBlockEntity rack = this.getRack(level);
    if (rack == null) return;

    ItemStack discInRack = rack.getItem(slotIndex);
    if (discInRack.isEmpty()) return;

    BlockPos jukeboxPos = findJukebox();
    if (jukeboxPos != null && level.getBlockEntity(jukeboxPos) instanceof JukeboxBlockEntity jukebox) {
        
        // 1. Full Stop & Reset
        clearJukebox(jukebox, jukeboxPos);

        // 2. Refresh target disc (Safety check)
        ItemStack discToPlay = rack.getItem(slotIndex);
        if (discToPlay.isEmpty()) return;

        // 3. Physical Insert
        ItemStack discCopy = discToPlay.split(1);
        jukebox.setTheItem(discCopy);
        this.currentPlayingSlot = slotIndex;

        // 4. Update BlockState FIRST to tell the client "A record is here"
        BlockState state = level.getBlockState(jukeboxPos);
        if (state.hasProperty(JukeboxBlock.HAS_RECORD)) {
            level.setBlock(jukeboxPos, state.setValue(JukeboxBlock.HAS_RECORD, true), 3);
        }
        // 6. Final Sync
        rack.setChanged();
        jukebox.setChanged();
        this.setChanged();
        level.sendBlockUpdated(jukeboxPos, state, state, 3);
    }
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
}