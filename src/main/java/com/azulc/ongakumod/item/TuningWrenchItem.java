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
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);

        // COPY LOGIC
        if (be instanceof DiscRackBlockEntity) {
            // We create a GlobalPos which includes the dimension and coordinates
            GlobalPos globalPos = GlobalPos.of(level.dimension(), pos);
            stack.set(OngakuMod.SAVED_LOCATION.get(), globalPos);
            
            context.getPlayer().displayClientMessage(Component.literal("Location Saved!"), true);
            return InteractionResult.SUCCESS;
        }

        // PASTE LOGIC
        if (be instanceof AutoplayControllerBlockEntity controller) {
            GlobalPos saved = stack.get(OngakuMod.SAVED_LOCATION.get());
            
            if (saved != null) {
                // Check if dimension matches
                if (saved.dimension() != level.dimension()) {
                    context.getPlayer().displayClientMessage(Component.literal("Wrong Dimension!"), true);
                    return InteractionResult.FAIL;
                }
                
                controller.setLinkedRack(saved.pos());
                stack.remove(OngakuMod.SAVED_LOCATION.get()); // Clear it
                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.0f, 1.0f);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
}