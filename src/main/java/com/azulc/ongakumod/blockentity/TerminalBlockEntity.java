package com.azulc.ongakumod.blockentity;

import java.util.UUID;
import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.container.TerminalMenu;
import com.azulc.ongakumod.util.ControllerRegistry;
import com.azulc.ongakumod.util.ControllerRegistry.ControllerSnapshot;
import com.azulc.ongakumod.util.LinkHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

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

    public void ClearNetworkId() {
        this.networkId = null;
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