package com.azulc.ongakumod.blockentity;

import java.util.UUID;
import com.azulc.ongakumod.OngakuMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TerminalBlockEntity extends BlockEntity 
{
    private UUID networkId;

    public TerminalBlockEntity(BlockPos pos, BlockState blockState) {
        super(OngakuMod.TERMINAL_BLOCK_ENTITY.get(), pos, blockState);
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public void setNetworkId(UUID id) {
        this.networkId = id;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.networkId != null) {
            tag.putUUID("controller_id", this.networkId);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("controller_id")) {
            this.networkId = tag.getUUID("controller_id");
        }
    }
}