package com.azulc.ongakumod.util;

import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;
import com.azulc.ongakumod.blockentity.DiscRackBlockEntity;
import com.azulc.ongakumod.util.PlaylistHelper.PlaylistEntry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class JukeboxHelper {
    public static BlockPos findJukebox(AutoplayControllerBlockEntity Controller) {
        BlockPos jukeboxPos = null;
        for (Direction dir : Direction.values()) {
            BlockPos adjacent = Controller.getBlockPos().relative(dir);
            if (Controller.getLevel().getBlockState(adjacent).is(Blocks.JUKEBOX)) {
                jukeboxPos = adjacent;
                break;
            }
        }
        return jukeboxPos;
    }
    public static void reconcileJukeboxState(AutoplayControllerBlockEntity Controller,ItemStack playing) {
        var linkedRackPositions = Controller.getLinkedRackPositions();
        for (BlockPos rackPos : linkedRackPositions) {
            DiscRackBlockEntity rack = LinkHelper.getRack(Controller,rackPos);
            if (rack == null) continue;

            for (int i = 0; i < rack.getContainerSize(); i++) {
                if (rack.getItem(i).isEmpty()) {
                    Controller.currentlyPlayingEntry = new PlaylistEntry(rackPos, i, playing.copy());
                    Controller.setChanged();
                    return; 
                }
            }
        }
    }
    public static void clearJukebox(AutoplayControllerBlockEntity Controller,JukeboxBlockEntity jukebox, BlockPos pos) 
    {
        Level level = Controller.getLevel();
        ItemStack existing = jukebox.getTheItem();
        if (!existing.isEmpty()) {
            returnDiscToRack(Controller,existing);
            jukebox.setTheItem(ItemStack.EMPTY);
        }
        
        BlockState state = level.getBlockState(pos);
        BlockState oldState = state;
        BlockState newState = state.setValue(JukeboxBlock.HAS_RECORD, false);
        level.setBlock(pos, newState, 3);
        level.sendBlockUpdated(pos,oldState,newState,3);
        jukebox.getSongPlayer().stop(level, state);
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
