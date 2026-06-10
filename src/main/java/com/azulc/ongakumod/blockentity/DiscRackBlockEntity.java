package com.azulc.ongakumod.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.block.DiscRackWallBlock;
import com.azulc.ongakumod.container.DiscContainer;

public class DiscRackBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer 
{

    private BlockPos controllerPos = null;

    public DiscRackBlockEntity(BlockPos pos, BlockState state) {
        super(OngakuMod.DISCRACK_BLOCK_ENTITY.get(), pos, state);
        // Dynamically set size based on the block type
        int size = (state.getBlock() instanceof DiscRackWallBlock) ? 1 : DiscContainer.SIZE;
        this.inventory = NonNullList.withSize(size, ItemStack.EMPTY);
        //this.inventory = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
    }

    private NonNullList<ItemStack> inventory;
    //private int users = 0;


    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory inventory) {
        if (this.getContainerSize() == 1) return null; // No GUI for the wall mount
        return new DiscContainer(syncId, inventory, this,this.getControllerPos());
    }

    @Override
    public int getContainerSize() {
        return this.inventory.size();
    }
/*     @Override
    public int getContainerSize() {
        return DiscContainer.SIZE;
    }
 */

    @Override
    protected Component getDefaultName() {
        return this.getBlockState().getBlock().getName();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> newInventory) {
        inventory = newInventory;
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return IntStream.range(0, getContainerSize()).toArray();
    }

    @Override
    public boolean canPlaceItemThroughFace(int i, ItemStack itemStack, Direction direction) {
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int i, ItemStack itemStack, Direction direction) {
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // If not using a loot table, save the actual inventory
        if (!this.trySaveLootTable(tag)) {
            ContainerHelper.saveAllItems(tag, this.inventory, registries);
        }
        if (controllerPos != null) tag.putLong("ControllerPos", controllerPos.asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.inventory = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        // If no loot table is being loaded, load the items from NBT
        if (!this.tryLoadLootTable(tag)) {
            ContainerHelper.loadAllItems(tag, this.inventory, registries);
        }
        if (tag.contains("ControllerPos")) controllerPos = BlockPos.of(tag.getLong("ControllerPos"));
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
        super.setItem(i, itemStack);
        if (this.level != null && !this.level.isClientSide) {
            // This forces the server to send the new inventory data to nearby players
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public ItemStack removeItem(int i, int j) {
        ItemStack ret = super.removeItem(i, j);
        return ret;
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        ItemStack ret = super.removeItemNoUpdate(i);
        return ret;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    public List<ItemStack> getContents() {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < this.getContainerSize(); i++) {
            ItemStack s = this.getItem(i);
            if (!s.isEmpty()) list.add(s.copy()); // Always copy when sending over network!
        }
        return list;
    }

    public void setControllerPos(BlockPos pos) {
    this.controllerPos = pos;
    this.setChanged();
    }

    public BlockPos getControllerPos() {
        return controllerPos;
    }

}