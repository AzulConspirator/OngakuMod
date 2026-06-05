package com.azulc.ongakumod.item;

import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;
import com.azulc.ongakumod.container.TerminalMenu;
import com.azulc.ongakumod.util.ControllerRegistry;
import com.azulc.ongakumod.util.ControllerRegistry.ControllerSnapshot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.UUID;

public class TerminalBlockItem extends BlockItem {

    public TerminalBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public UUID getNetworkId(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        return tag.hasUUID("controller_id") ? tag.getUUID("controller_id") : null;
    }

    public void setNetworkId(ItemStack stack, UUID id) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("controller_id", id);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) return InteractionResult.PASS;

        // If shifting, check if we clicked a controller to link it
        if (player.isShiftKeyDown()) {
            BlockEntity be = level.getBlockEntity(clickedPos);
            if (be instanceof AutoplayControllerBlockEntity autoplayBE) {
                if (!level.isClientSide) {
                    // Assuming your controller has a way to get its persistent network ID
                    // Replace 'getNetworkId()' with your actual controller's unique ID getter
                    UUID controllerUUID = autoplayBE.getNetworkId(autoplayBE); 
                    setNetworkId(stack, controllerUUID);
                    player.displayClientMessage(Component.literal("Terminal linked to controller!"), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            // If shifting but NOT clicking a controller, allow normal block placement
            return super.useOn(context);
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (player.isCrouching()) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            UUID controllerId = getNetworkId(stack);
            
            if (controllerId != null) {
                // Correctly use vanilla MenuProvider with NeoForge's patched extra data consumer
                serverPlayer.openMenu(new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.literal("Wireless Vinyl Terminal");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                        return new TerminalMenu(id, inv, controllerId);
                    }
                }, buf -> {
                    // 1. Write form type indicator
                    buf.writeBoolean(true); // true = opened from item
                    // 2. Write targeting identity
                    buf.writeUUID(controllerId);
                    
                    // 3. FIX: Fetch and write the actual snapshot data to the wire!
                    ControllerSnapshot currentSnapshot = ControllerRegistry.get((ServerLevel) serverPlayer.level()).getSnapshot(controllerId);
                    if (currentSnapshot != null) {
                        buf.writeBoolean(true);
                        currentSnapshot.write(buf); // Implement this method inside your ControllerSnapshot class to save its fields
                    } else {
                        buf.writeBoolean(false);
                    }
                });
                return InteractionResultHolder.success(stack);
            } else {
                player.displayClientMessage(Component.literal("Terminal is not linked to a controller!"), true);
            }
        }
        return InteractionResultHolder.success(stack);
    }
}