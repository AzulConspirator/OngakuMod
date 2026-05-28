package com.azulc.ongakumod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DiscRackBoxBlock extends DiscRackBlock {
    
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 14, 14);

    public DiscRackBoxBlock(Properties properties) {
        super(properties);
    }
       @Override
    protected VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return SHAPE;
    }
}