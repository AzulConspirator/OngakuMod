package com.azulc.ongakumod.blockentity;

import com.azulc.ongakumod.OngakuMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SpeakerBlockEntity extends BlockEntity {
    private BlockPos controllerPos = null;
    private boolean isPlayingMusic = false; // Default to false [cite: 228]

    public SpeakerBlockEntity(BlockPos pos, BlockState blockState) {
        super(OngakuMod.SPEAKER_BLOCK_ENTITY.get(), pos, blockState);
    }

    public void setPlaying(boolean state) {
        this.isPlayingMusic = state;
        this.setChanged();
        if (level != null && !level.isClientSide) {
            // Sync this change to the client so particles start/stop [cite: 273]
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public BlockPos getControllerPos()
    {
        return controllerPos;
    }

      public void setControllerPos(BlockPos pos) {
    this.controllerPos = pos;
    this.setChanged();
    }

    public boolean isPlaying() { // Removed static 
        return isPlayingMusic;
    }

    // Sync NBT so the client knows the state when it loads the chunk [cite: 292]
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("IsPlaying", isPlayingMusic);
        if (controllerPos != null) tag.putLong("ControllerPos", controllerPos.asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.isPlayingMusic = tag.getBoolean("IsPlaying");
        if (tag.contains("ControllerPos")) this.controllerPos = BlockPos.of(tag.getLong("ControllerPos"));
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, SpeakerBlockEntity blockEntity) {
        if (blockEntity.isPlaying()) 
            { // Use the instance state 
            if (level.random.nextInt(5) == 0) { // Add a random check so it's not too crowded
                double x = pos.getX() + 0.5;
                double y = pos.getY() + 1.2;
                double z = pos.getZ() + 0.5;
                level.addParticle(ParticleTypes.NOTE, x, y, z, 0.0, 0.2, 0.0);
            }
        }
    }
}