package com.azulc.ongakumod.block;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;
import com.azulc.ongakumod.container.AutoplayMenu;
import com.azulc.ongakumod.item.TuningWrenchItem;
import com.azulc.ongakumod.util.ControllerRegistry;
import com.azulc.ongakumod.util.CtrlHelper;
import com.azulc.ongakumod.util.PlaylistHelper;
import com.azulc.ongakumod.network.TerminalControlHandler;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

public class AutoplayControllerBlock extends HorizontalDirectionalBlock implements EntityBlock 
{
    
    public AutoplayControllerBlock(Properties properties) 
    {
        super(properties);
        this.registerDefaultState(
            this.defaultBlockState()
            .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() 
    {
        return simpleCodec(AutoplayControllerBlock::new);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) 
    {
        return new AutoplayControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) 
    {
        if (level.isClientSide) return null;
        return type == OngakuMod.AUTOPLAY_BLOCK_ENTITY.get()  ? (lvl, pos, st, be) -> AutoplayControllerBlockEntity.serverTick(lvl, pos, st, (AutoplayControllerBlockEntity) be) : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hitResult) 
    {
        if (stack.getItem() instanceof TuningWrenchItem) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AutoplayControllerBlockEntity controller) {
                List<ItemStack> playlistDiscs = PlaylistHelper.buildPlaylist(controller).stream().map(entry -> entry.stack().copy()).toList();
                serverPlayer.openMenu(new SimpleMenuProvider((id, inv, p) ->  new AutoplayMenu(id,inv,controller,controller.data,playlistDiscs),Component.literal("Autoplay Controller") ),
                    buf -> {buf.writeBlockPos(pos);ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode(buf,playlistDiscs);}
                );
            }
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) 
    {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AutoplayControllerBlockEntity controller) 
            {
                CtrlHelper.StopJukebox(controller);
                if (level.getServer() != null) 
                {
                    for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                        TerminalControlHandler.broadcastToTerminalOffline(player, AutoplayControllerBlockEntity.getNetworkId(controller), Optional.empty(),true, false,Optional.of(BlockPos.ZERO));
                    }
                }
                 if(level instanceof ServerLevel serverLevel)
                {
                    ControllerRegistry.get(serverLevel).unregister(AutoplayControllerBlockEntity.getNetworkId(controller));
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
