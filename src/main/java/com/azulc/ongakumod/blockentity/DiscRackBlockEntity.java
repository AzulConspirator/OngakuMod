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
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;

import java.util.stream.IntStream;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.container.DiscContainer;

public class DiscRackBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer 
{

    public DiscRackBlockEntity(BlockPos pos, BlockState state) {
        super(OngakuMod.DISCRACK_BLOCK_ENTITY.get(), pos, state);
        this.inventory = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
    }

    private NonNullList<ItemStack> inventory;
    //private int users = 0;


    @Override
    protected AbstractContainerMenu createMenu(int syncId, Inventory inventory) {
        return new DiscContainer(syncId, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return DiscContainer.SIZE;
    }


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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.inventory = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        // If no loot table is being loaded, load the items from NBT
        if (!this.tryLoadLootTable(tag)) {
            ContainerHelper.loadAllItems(tag, this.inventory, registries);
        }
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
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        this.saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

}