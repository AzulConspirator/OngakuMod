package com.azulc.ongakumod.container;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class AutoplayMenu extends AbstractContainerMenu {
    private final AutoplayControllerBlockEntity blockEntity;
    private final ContainerData _data;
    private final List<ItemStack> syncedDiscs;

    // Client-side constructor (Used by IMenuTypeExtension)
    public AutoplayMenu(int containerId, Inventory inv, BlockPos pos, List<ItemStack> initialDiscs) {
        this(containerId, inv, (AutoplayControllerBlockEntity) inv.player.level().getBlockEntity(pos), 
             new SimpleContainerData(3), initialDiscs); // Note: Data size 3
    }

    // Server-side constructor (Used by the Block)
    public AutoplayMenu(int containerId, Inventory inv, AutoplayControllerBlockEntity entity, ContainerData data, List<ItemStack> initialDiscs) {
        super(OngakuMod.AUTOPLAY_MENU.get(), containerId);
        this.blockEntity = entity;
        this._data = data;
        this.syncedDiscs = initialDiscs; // Now properly passed in
        addDataSlots(_data); 
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        // Safe check for null blockEntity to prevent client crashes
        if (blockEntity == null) return false;
        return AbstractContainerMenu.stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), 
               blockEntity.getBlockPos()), player, OngakuMod.AUTOPLAY_CONTROLLER.get());
    }

    // Use THIS in your Screen to show the list
    public List<ItemStack> getSyncedDiscs() {
        return syncedDiscs;
    }

    public ContainerData getData() {
        return this._data;
    }

    public BlockPos getBlockPos() {
        return this.blockEntity.getBlockPos();
    }
}