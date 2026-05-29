package com.azulc.ongakumod.item;

import net.minecraft.world.item.Item;
import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;
import com.azulc.ongakumod.blockentity.DiscRackBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class TuningWrenchItem extends Item {

    public TuningWrenchItem(Properties properties) {
        super(properties);
    }
    
    @SuppressWarnings("unused")
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(clickedPos);

        // 1. COPY LOGIC (Now on the Controller)
        if (be instanceof AutoplayControllerBlockEntity) {
            GlobalPos globalPos = GlobalPos.of(level.dimension(), clickedPos);
            stack.set(OngakuMod.SAVED_LOCATION.get(), globalPos);
            
            context.getPlayer().displayClientMessage(Component.literal("Controller Position Saved!"), true);
            level.playSound(null, clickedPos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 1.0f, 1.0f);
            return InteractionResult.SUCCESS;
        }

        // 2. PASTE LOGIC (Now on the Rack)
        if (be instanceof  DiscRackBlockEntity rack) {
            GlobalPos saved = stack.get(OngakuMod.SAVED_LOCATION.get());
            
            if (saved == null) {
                context.getPlayer().displayClientMessage(Component.literal("No Controller Saved on Wrench!"), true);
                return InteractionResult.FAIL;
            }

            if (saved.dimension() != level.dimension()) {
                context.getPlayer().displayClientMessage(Component.literal("Wrong Dimension!"), true);
                return InteractionResult.FAIL;
            }

            // Attempt to find the saved controller in the world
            BlockEntity savedBE = level.getBlockEntity(saved.pos());
            if (savedBE instanceof AutoplayControllerBlockEntity controller) {
                // Tell the controller to add THIS rack
                boolean added = controller.addLinkedRack(clickedPos);
                
                if (added) {
                    context.getPlayer().displayClientMessage(Component.literal("Rack Linked to Controller!"), true);
                    level.playSound(null, clickedPos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.0f, 1.2f);
                    
                    // Crucial for rendering: The controller needs to sync its new list to clients
                    controller.setChanged();
                    level.sendBlockUpdated(saved.pos(), controller.getBlockState(), controller.getBlockState(), 3);
                } else {
                    controller.removeLinkedRack(clickedPos);
                    context.getPlayer().displayClientMessage(Component.literal("Disconnected the Rack"), true);
                }
                return InteractionResult.SUCCESS;
            } else {
                context.getPlayer().displayClientMessage(Component.literal("Saved Controller not found!"), true);
                stack.remove(OngakuMod.SAVED_LOCATION.get()); // Clean up if it was broken
                return InteractionResult.FAIL;
            }
        }

        return InteractionResult.PASS;
    }
}