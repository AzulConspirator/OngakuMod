package com.azulc.ongakumod.item;

import net.minecraft.world.item.Item;
import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;
import com.azulc.ongakumod.blockentity.DiscRackBlockEntity;
import com.azulc.ongakumod.blockentity.SpeakerBlockEntity;
import com.azulc.ongakumod.util.LinkHelper;
import com.azulc.ongakumod.util.PlaylistHelper;

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
        if (context.getPlayer().isCrouching())
        {
            // 1. COPY LOGIC (Now on the Controller)
            if (be instanceof AutoplayControllerBlockEntity) {
                GlobalPos globalPos = GlobalPos.of(level.dimension(), clickedPos);
                stack.set(OngakuMod.SAVED_LOCATION.get(), globalPos);
                
                context.getPlayer().displayClientMessage(Component.translatable("tuningfork.ongakumod.positionsaved"), true);
                level.playSound(null, clickedPos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 1.0f, 1.0f);
                return InteractionResult.SUCCESS;
            }

            // 2. PASTE LOGIC (Now on the Rack)
            if (be instanceof  DiscRackBlockEntity rack) {
                GlobalPos saved = stack.get(OngakuMod.SAVED_LOCATION.get());
                
                if (saved == null) {
                    context.getPlayer().displayClientMessage(Component.translatable("tuningfork.ongakumod.noposition"), true);
                    return InteractionResult.FAIL;
                }

                if (saved.dimension() != level.dimension()) {
                    context.getPlayer().displayClientMessage(Component.translatable("tuningfork.ongakumod.wrongdimension"), true);
                    return InteractionResult.FAIL;
                }
                BlockEntity savedBE = level.getBlockEntity(saved.pos());
                if (savedBE instanceof AutoplayControllerBlockEntity controller) 
                {
                    boolean nowLinked = LinkHelper.addLinkedRack(controller,clickedPos);
                    if (nowLinked) {
                        context.getPlayer().displayClientMessage(Component.translatable("tuningfork.ongakumod.storagelinked"), true);
                        level.playSound(null, clickedPos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.0f, 1.2f);
                    } else {
                        context.getPlayer().displayClientMessage(Component.translatable("tuningfork.ongakumod.storageunlink"), true);
                        level.playSound(null, clickedPos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0f, 0.8f);
                    }
                    PlaylistHelper.broadcastPlaylistUpdate(controller);
                    return InteractionResult.SUCCESS;
                }
            }
            if (be instanceof  SpeakerBlockEntity speaker) {
                GlobalPos saved = stack.get(OngakuMod.SAVED_LOCATION.get());
                
                if (saved == null) {
                    context.getPlayer().displayClientMessage(Component.translatable("tuningfork.ongakumod.noposition"), true);
                    return InteractionResult.FAIL;
                }

                if (saved.dimension() != level.dimension()) {
                    context.getPlayer().displayClientMessage(Component.translatable("tuningfork.ongakumod.wrongdimension"), true);
                    return InteractionResult.FAIL;
                }
                BlockEntity savedBE = level.getBlockEntity(saved.pos());
                if (savedBE instanceof AutoplayControllerBlockEntity controller) 
                {
                    boolean nowLinked = LinkHelper.addLinkedSpeaker(controller,clickedPos);
                    if (nowLinked) {
                        context.getPlayer().displayClientMessage(Component.translatable("tuningfork.ongakumod.speakerlinked"), true);
                        level.playSound(null, clickedPos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.0f, 1.2f);
                    } else {
                        context.getPlayer().displayClientMessage(Component.translatable("tuningfork.ongakumod.speakerunlinked"), true);
                        level.playSound(null, clickedPos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0f, 0.8f);
                    }
                    PlaylistHelper.broadcastPlaylistUpdate(controller);
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return super.useOn(context);
    }
}