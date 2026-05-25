package com.azulc.ongakumod.block;

import com.azulc.ongakumod.blockentity.DiscRackBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class DiscRackBlock extends HorizontalDirectionalBlock implements EntityBlock {
    
    public DiscRackBlock(Properties properties) {
        super(properties);
    }

    private static final VoxelShape RACK_X = Block.box(0, 0, 4, 16, 2, 12);
    private static final VoxelShape RACK_Z = Block.box(4, 0, 0, 12, 2, 16);

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof DiscRackBlockEntity rack) {
                // This helper drops everything in the inventory at the block's location
                Containers.dropContents(level, pos, rack);
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        return defaultBlockState().setValue(FACING, blockPlaceContext.getHorizontalDirection().getOpposite());
    }

        @Override
    protected InteractionResult useWithoutItem(BlockState blockState, Level level, BlockPos blockPos, Player player, BlockHitResult blockHitResult) {
        if(level.isClientSide()) return InteractionResult.SUCCESS;
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if(blockEntity != null && blockEntity instanceof MenuProvider menuProvider)
            player.openMenu(menuProvider);
        return InteractionResult.CONSUME;
    }


    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(DiscRackBlock::new);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL; // Tells Minecraft to render the JSON model
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true; // Prevents weird dark shadows inside your rack
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new DiscRackBlockEntity(blockPos, blockState);
    }

    @Override
    protected VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return blockState.getValue(FACING).getAxis() == Direction.Axis.X ? RACK_X : RACK_Z;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    // get comparator redstone output
    @Override
    protected boolean hasAnalogOutputSignal(BlockState blockState) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos blockPos) 
    {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if(blockEntity != null)
            return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(blockEntity);
        return super.getAnalogOutputSignal(blockState, level, blockPos);
    }
}