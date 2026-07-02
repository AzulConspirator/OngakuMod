package com.azulc.ongakumod.blockentity;

import com.azulc.ongakumod.OngakuMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SpeakerBlockEntity extends BlockEntity {
    private BlockPos controllerPos = null;
    private boolean isPlayingMusic = false; 

    public SpeakerBlockEntity(BlockPos pos, BlockState blockState) {
        super(OngakuMod.SPEAKER_BLOCK_ENTITY.get(), pos, blockState);
    }

    public void setPlaying(boolean state) {
        this.isPlayingMusic = state;
        this.setChanged();
        if (level != null && !level.isClientSide) {
            // CRITICAL: This tells Minecraft to sync the Block Entity to all nearby clients
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean isPlaying() {
        return isPlayingMusic;
    }

    // --- SYNCHRONIZATION BOLT-ON ---

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries); // Include our custom data in the update
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        // Creates the actual network packet to send to the client
        return ClientboundBlockEntityDataPacket.create(this);
    }

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

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            this.loadAdditional(tag, registries);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.loadAdditional(tag, registries);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, SpeakerBlockEntity blockEntity) {
        if (blockEntity.isPlaying() && level.isClientSide()) {
            if (level.random.nextInt(10) == 0) { // Don't spawn every single tick (less lag)
                double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.3;
                double y = pos.getY() + 1.1;
                double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.3;
                level.addParticle(ParticleTypes.NOTE, x, y, z, 0.0, 0.1, 0.0);
            }
        }
    }

    public BlockPos getControllerPos() {
       return controllerPos;
    }

    public void setControllerPos(BlockPos object) {
        this.controllerPos = object;
    }
}