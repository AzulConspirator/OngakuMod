package com.azulc.ongakumod.blockentity;

import java.util.UUID;
import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.container.TerminalMenu;
import com.azulc.ongakumod.util.ControllerRegistry;
import com.azulc.ongakumod.util.ControllerRegistry.ControllerSnapshot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
        this.setChanged(); // Keep chunk save-state updated
    }

    // --- NEW: Persist network identifier across saves/chunk unloads ---
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

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if(level.isClientSide) return InteractionResult.SUCCESS;

        if(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            if (this.networkId == null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Terminal unlinked!"), true);
                return InteractionResult.FAIL;
            }

            serverPlayer.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("Wireless Vinyl Terminal");
                }

                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                    return new TerminalMenu(id, inv, networkId);
                }
            }, buf -> {
                // 1. Write form type indicator
                buf.writeBoolean(true); // true = opened from item
                // 2. Write targeting identity
                buf.writeUUID(networkId);
                
                // 3. FIX: Fetch and write the actual snapshot data to the wire!
                ControllerSnapshot currentSnapshot = ControllerRegistry.get((ServerLevel) serverPlayer.level()).getSnapshot(networkId);
                if (currentSnapshot != null) {
                    buf.writeBoolean(true);
                    currentSnapshot.write(buf); // Implement this method inside your ControllerSnapshot class to save its fields
                } else {
                    buf.writeBoolean(false);
                }
            });
                    }
        return InteractionResult.CONSUME;
    }
}