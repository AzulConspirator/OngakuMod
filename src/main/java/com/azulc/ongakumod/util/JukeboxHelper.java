package com.azulc.ongakumod.util;

import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;
import com.azulc.ongakumod.blockentity.DiscRackBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class JukeboxHelper 
{
    public static BlockPos findJukebox(AutoplayControllerBlockEntity Controller) 
    {
        BlockPos jukeboxPos = null;
        if (Controller.JukeboxPosition != null)
        {
            if (Controller.getLevel().getBlockState(Controller.JukeboxPosition).is(Blocks.JUKEBOX))
            {
                return Controller.JukeboxPosition;
            }
        }
        for (Direction dir : Direction.values()) {
            BlockPos adjacent = Controller.getBlockPos().relative(dir);
            if (Controller.getLevel().getBlockState(adjacent).is(Blocks.JUKEBOX)) {
                jukeboxPos = adjacent;
                break;
            }
        }
        Controller.JukeboxPosition = jukeboxPos;
        return jukeboxPos;
    }
    
    public static int CheckJukeStatus(AutoplayControllerBlockEntity controller, BlockPos jukeboxPos)
    {
        Level level = controller.getLevel();
        if (jukeboxPos != null && level.getBlockEntity(jukeboxPos) instanceof JukeboxBlockEntity jukebox) {
            if (jukebox.getBlockState().getValue(JukeboxBlock.HAS_RECORD)) {
                boolean playing = jukebox.getSongPlayer().isPlaying() || controller.currentlyPlayingEntry != null;
                return playing ? 1 : 0;
            } 
            else {
                return 0;
            }
        }
        else {
            return -1;
        }
    }
    
    public static void clearJukebox(AutoplayControllerBlockEntity Controller, JukeboxBlockEntity jukebox, BlockPos pos) 
    {
        Level level = Controller.getLevel();
        BlockState state = level.getBlockState(pos);
        level.levelEvent(1011, pos, 0); 
        //jukebox.getSongPlayer().stop(level, state);
        ItemStack existing = jukebox.getTheItem();
        if (!existing.isEmpty()) {
            returnDiscToRack(Controller, existing);
        }
        jukebox.setTheItem(ItemStack.EMPTY);
        BlockState newState = state.setValue(JukeboxBlock.HAS_RECORD, false);
        level.setBlock(pos,newState,3);

        level.gameEvent(GameEvent.BLOCK_CHANGE,pos,GameEvent.Context.of(newState));
        level.sendBlockUpdated(pos,state,newState,3);
        Controller.currentPlaylistIndex = -1;
    }

    public static void returnDiscToRack(AutoplayControllerBlockEntity Controller,ItemStack existingDisc)
    {
        if (existingDisc.isEmpty()) return;
        // 1. Try the "Remembered" slot first
        if (Controller.currentlyPlayingEntry != null) {
            DiscRackBlockEntity originalRack = LinkHelper.getRack(Controller,Controller.currentlyPlayingEntry.rackPos());
            if (originalRack != null && originalRack.getItem(Controller.currentlyPlayingEntry.slotIndex()).isEmpty()) {
                originalRack.setItem(Controller.currentlyPlayingEntry.slotIndex(), existingDisc.copy());
                finalizeReturn(Controller,originalRack);
                return;
            }
        }
        var linkedRackPositions = Controller.getLinkedRackPositions();
        // 2. Emergency Search: Try to find ANY empty slot in ANY linked rack
        for (BlockPos rackPos : linkedRackPositions) {
            DiscRackBlockEntity rack = LinkHelper.getRack(Controller,rackPos);
            if (rack == null) continue;
            for (int i = 0; i < rack.getContainerSize(); i++) {
                if (rack.getItem(i).isEmpty()) {
                    rack.setItem(i, existingDisc.copy());
                    finalizeReturn(Controller,rack);
                    return;
                }
            }
        }
        
        Block.popResource(Controller.getLevel(), Controller.getBlockPos(), existingDisc);
        Controller.currentlyPlayingEntry = null;
        Controller.currentPlaylistIndex = -1;
    }

    private static void finalizeReturn(AutoplayControllerBlockEntity Controller,DiscRackBlockEntity rack) {
        rack.setChanged();
        Controller.getLevel().sendBlockUpdated(rack.getBlockPos(), rack.getBlockState(), rack.getBlockState(), 3);
        Controller.currentlyPlayingEntry = null;
        Controller.currentPlaylistIndex = -1;
        Controller.setChanged();
    }

}
