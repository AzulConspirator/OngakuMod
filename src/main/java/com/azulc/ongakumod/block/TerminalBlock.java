package com.azulc.ongakumod.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.azulc.ongakumod.blockentity.TerminalBlockEntity;
import com.azulc.ongakumod.container.TerminalMenu;
import com.azulc.ongakumod.util.ControllerRegistry;
import com.azulc.ongakumod.util.ControllerRegistry.ControllerSnapshot;
import com.azulc.ongakumod.util.LinkHelper;
import com.azulc.ongakumod.network.TerminalControlHandler;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
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
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TerminalBlock extends HorizontalDirectionalBlock implements EntityBlock  {
    
    public static final MapCodec<TerminalBlock> CODEC = simpleCodec(TerminalBlock::new);
    public static final VoxelShape SHAPE_FLOOR = Block.box(1, 0, 6, 15, 8, 10);
    public static final VoxelShape SHAPE_WALL = Block.box(1, 4, 12, 15, 12, 16);

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;
    
    public MapCodec<TerminalBlock> codec() {
        return CODEC;
    }
    
    public TerminalBlock(Properties properties) 
    {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FACE, AttachFace.FLOOR));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING, FACE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        Direction clickedFace = context.getClickedFace();
        if(clickedFace == Direction.UP)
        {
            return defaultBlockState().setValue(FACE,AttachFace.FLOOR).setValue(FACING,context.getHorizontalDirection().getOpposite());
        }
        return defaultBlockState().setValue(FACE,AttachFace.WALL).setValue(FACING,clickedFace);
    }
        
    protected boolean hasAnalogOutputSignal(BlockState pState) {
        return true;
    }
    
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }
    
    @Override
    public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context)
    {
        return rotateShape((state.getValue(FACE) == AttachFace.WALL)? SHAPE_WALL : SHAPE_FLOOR, state.getValue(FACING));
    }

    private static VoxelShape rotateShape(VoxelShape shape, Direction direction) {
        if (direction == Direction.NORTH) return shape;
        int rotations = (direction.get2DDataValue() - Direction.NORTH.get2DDataValue() + 4) % 4;
        VoxelShape rotated = shape;
        for (int i = 0; i < rotations; i++) {
            final List<AABB> boxes = new ArrayList<>();
            rotated.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {boxes.add(new AABB(1 - maxZ, minY, minX,1 - minZ, maxY, maxX ));});
            rotated = Shapes.empty();
            for (AABB box : boxes) {
                rotated = Shapes.or(rotated,Shapes.box(box.minX, box.minY, box.minZ,box.maxX, box.maxY, box.maxZ));
            }
        }
        return rotated;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new TerminalBlockEntity(blockPos, blockState);
    }
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TerminalBlockEntity terminal) 
            {
                ItemStack dropStack = new ItemStack(this);
                if (terminal.getNetworkId() != null)
                {
                    CompoundTag tag = new CompoundTag();                
                    tag.putUUID("controller_id", terminal.getNetworkId());
                    dropStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) 
                    {
                        TerminalControlHandler.broadcastToTerminalOffline(player, terminal.getNetworkId(), Optional.empty(), true, true,Optional.of(pos));
                    }
                    ControllerRegistry.get((ServerLevel) level).unregisterTerminal(terminal.getNetworkId(), pos);
                }
                Block.popResource(level, pos, dropStack);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TerminalBlockEntity terminalBE) {
                CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                CompoundTag tag = customData.copyTag();
                if (tag.hasUUID("controller_id")) {
                    UUID id = tag.getUUID("controller_id");
                    terminalBE.setNetworkId(id);
                    ControllerRegistry.get((ServerLevel) level).registerTerminal(id, pos);
                }
            }
        }
    }
    
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult)
    {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) 
        {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TerminalBlockEntity terminal) 
            {
                UUID controllerId = terminal.getNetworkId();
                if (controllerId != null && pos != null) 
                {            
                    ControllerRegistry CtrlRegistry = ControllerRegistry.get((ServerLevel)level);
                    GlobalPos GlobePos = CtrlRegistry.get(controllerId);
                    if(CtrlRegistry.isControllerLoaded(level.getServer(), controllerId) && !LinkHelper.ControllerExist(controllerId, level, GlobePos))
                    {
                        player.displayClientMessage(Component.literal("Controller Missing, unlinking Terminal"), true);
                        ControllerRegistry.get((ServerLevel)level).unregister(controllerId);
                        terminal.ClearNetworkId();
                        return  InteractionResult.FAIL;
                    }
                    ControllerSnapshot CurrentSnapshot = ControllerRegistry.get((ServerLevel)level).getSnapshot(controllerId); 
                    if (CurrentSnapshot == null )
                    {
                        player.displayClientMessage(Component.literal("Unable to find Soundbox Snapshot"), true);
                        return InteractionResult.FAIL;
                    }
                    serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new TerminalMenu(id, inv, terminal,true), 
                    Component.literal("")
                    ), buf -> {
                        buf.writeBoolean(false);
                        buf.writeUUID(terminal.getNetworkId());
                        buf.writeBlockPos(pos);
                        ControllerSnapshot currentSnapshot = ControllerRegistry.get((ServerLevel) serverPlayer.level()).getSnapshot(terminal.getNetworkId());
                        if(currentSnapshot != null)
                        {
                            buf.writeBoolean(true);
                            currentSnapshot.write(buf);
                            buf.writeBoolean(CtrlRegistry.isControllerLoaded(serverPlayer.server, controllerId));
                        }
                        else
                        {
                            buf.writeBoolean(false);
                        }
                    });
                    return InteractionResult.SUCCESS;
                }
                player.displayClientMessage(Component.literal("Terminal Not linked!"), true);
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.SUCCESS;
    }
}