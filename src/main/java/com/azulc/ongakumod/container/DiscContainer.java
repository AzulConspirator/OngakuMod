package com.azulc.ongakumod.container;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Predicate;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;
import com.azulc.ongakumod.util.LinkHelper;
import com.azulc.ongakumod.util.PlaylistHelper;

public class DiscContainer extends AbstractContainerMenu {
    public static final int SIZE = 8;
    public static final Predicate<ItemStack> FILTER = stack -> {

        return stack.has(DataComponents.JUKEBOX_PLAYABLE) || LinkHelper.hasComponentByString(stack,"etched:music");
    };

    protected final Container inventory;
    private static BlockPos controllerpos;

    public DiscContainer(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleContainer(8),controllerpos);
    }

    @SuppressWarnings("static-access")
    public DiscContainer(int syncId, Inventory playerInventory, Container inventory, BlockPos Controller) {
        super(OngakuMod.DISC_MENU.get(), syncId);
        this.inventory = inventory;
        this.inventory.startOpen(playerInventory.player);
        this.controllerpos = Controller;
        // inventory
        for(int ind = 0; ind < 8; ind++) {
            this.addSlot(new ConditionalSlot(FILTER, inventory, ind, 17 + ind * 18, 18));
        }

        // i = (#rows - #playersRows) * 18
        int i = -3 * 18;

        // player's inventory
        for(int l = 0; l < 3; ++l) {
            for(int j1 = 0; j1 < 9; ++j1) {
                this.addSlot(new Slot(playerInventory, j1 + l * 9 + 9, 8 + j1 * 18, 103 + l * 18 + i));
            }
        }

        // player's hotbar
        for(int i1 = 0; i1 < 9; ++i1) {
            this.addSlot(new Slot(playerInventory, i1, 8 + i1 * 18, 161 + i));
        }
    }

    @SuppressWarnings("static-access")
    @Override
    public void removed(Player playerEntity) {
        if (this.controllerpos != null)
        {
           BlockEntity AC = playerEntity.level().getBlockEntity(controllerpos);
           if (AC instanceof AutoplayControllerBlockEntity Ctrl){
                PlaylistHelper.broadcastPlaylistUpdate(Ctrl);
           }
        }
        super.removed(playerEntity);
        this.inventory.stopOpen(playerEntity);

    }

    @Override
    public boolean stillValid(Player playerEntity) {
        return inventory.stillValid(playerEntity);
    }

    @Override
    public ItemStack quickMoveStack(Player playerEntity, int slotIndex) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            stack = stackInSlot.copy();

            if (slotIndex < this.inventory.getContainerSize()) {
                if (!this.moveItemStackTo(stackInSlot, this.inventory.getContainerSize(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stackInSlot, 0, this.inventory.getContainerSize(), false)) {
                return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return stack;
    }
}


class ConditionalSlot extends Slot {
    private Predicate<ItemStack> predicate;

    public ConditionalSlot(Predicate<ItemStack> predicate, Container container, int i, int j, int k) {
        super(container, i, j, k);
        this.predicate = predicate;
    }

    @Override
    public boolean mayPlace(ItemStack itemStack) {
        return predicate.test(itemStack);
    }
}