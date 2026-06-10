package com.azulc.ongakumod.block;

import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;
import com.azulc.ongakumod.blockentity.DiscRackBlockEntity;
import com.azulc.ongakumod.util.LinkHelper;
import com.azulc.ongakumod.util.PlaylistHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DiscRackWallBlock extends DiscRackBlock {
    
    // Custom shape for a wall mount (thin against the wall)
    private static final VoxelShape WALL_NORTH = Block.box(2, 2, 14, 14, 14, 16);
    private static final VoxelShape WALL_SOUTH = Block.box(2, 2, 0, 14, 14, 2);
    private static final VoxelShape WALL_EAST = Block.box(0, 2, 2, 2, 14, 14);
    private static final VoxelShape WALL_WEST = Block.box(14, 2, 2, 16, 14, 14);

    public DiscRackWallBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> WALL_NORTH;
            case SOUTH -> WALL_SOUTH;
            case EAST -> WALL_EAST;
            case WEST -> WALL_WEST;
            default -> WALL_NORTH;
        };
    }

    // Override the interaction to insert/extract directly instead of opening a menu
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DiscRackBlockEntity rack) {
            ItemStack inHand = player.getMainHandItem();
            ItemStack inRack = rack.getItem(0);

            // If rack has a disc, pop it out
            if (!inRack.isEmpty()) {
                player.getInventory().placeItemBackInInventory(inRack.copy());
                rack.setItem(0, ItemStack.EMPTY);
                return InteractionResult.CONSUME;
            } 
            // If rack is empty and player is holding a playable disc
            else if (inRack.isEmpty() && (inHand.has(DataComponents.JUKEBOX_PLAYABLE)||LinkHelper.hasComponentByString(inHand,"etched:music"))) {
                rack.setItem(0, inHand.split(1));
                return InteractionResult.CONSUME;
            }
            var CtrlPos = rack.getControllerPos();
            if (CtrlPos != null){            
                BlockEntity AE = level.getBlockEntity(CtrlPos);
                if (AE instanceof AutoplayControllerBlockEntity Ctrl)
                {
                    PlaylistHelper.broadcastPlaylistUpdate(Ctrl);
                }
            }
        }
        return InteractionResult.PASS;
    }
}