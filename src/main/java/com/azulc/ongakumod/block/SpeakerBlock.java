package com.azulc.ongakumod.block;

import javax.annotation.Nullable;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;
import com.azulc.ongakumod.blockentity.SpeakerBlockEntity;
import com.azulc.ongakumod.util.CtrlHelper;
import com.azulc.ongakumod.util.LinkHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SpeakerBlock extends HorizontalDirectionalBlock implements EntityBlock  {
    
    public static final MapCodec<SpeakerBlock> CODEC = simpleCodec(SpeakerBlock::new);
    public static final VoxelShape SHAPE_FLOOR = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
    public static final VoxelShape SHAPE_CEILING = Block.box(0.0D, 8.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final DirectionProperty VERTICAL_FACING = DirectionProperty.create("vertical_facing", Direction.UP, Direction.DOWN);

    public MapCodec<SpeakerBlock> codec() {
        return CODEC;
    }
    
    public SpeakerBlock(Properties properties) 
    {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH).setValue(VERTICAL_FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, VERTICAL_FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();

        return this.defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite())
            .setValue( VERTICAL_FACING, clickedFace == Direction.DOWN ? Direction.DOWN : Direction.UP );
    }
    
    protected boolean hasAnalogOutputSignal(BlockState pState) {
        return true;
    }
    
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }
    
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return pState.getValue(VERTICAL_FACING) == Direction.DOWN?  SHAPE_CEILING : SHAPE_FLOOR;
    }
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new SpeakerBlockEntity(blockPos, blockState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // We only want the ticker to run on the client side for particles
        if (level.isClientSide) {
            return createTickerHelper(type, OngakuMod.SPEAKER_BLOCK_ENTITY.get(), SpeakerBlockEntity::clientTick);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(BlockEntityType<A> actualType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == actualType ? (BlockEntityTicker<A>) ticker : null;
    }
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof SpeakerBlockEntity rack) {
                level.updateNeighbourForOutputSignal(pos, this);
                BlockPos controllerPos = rack.getControllerPos();
                if (controllerPos != null && level.getBlockEntity(controllerPos) instanceof AutoplayControllerBlockEntity controller) {
                    CtrlHelper.StopJukebox(controller);
                    LinkHelper.broadcastToSpeakers(controller,false, null);
                    LinkHelper.removeLinkedSpeaker(controller,pos);             
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}