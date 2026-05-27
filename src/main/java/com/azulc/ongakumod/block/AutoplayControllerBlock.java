package com.azulc.ongakumod.block;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;
import com.azulc.ongakumod.container.AutoplayMenu;
import com.azulc.ongakumod.item.TuningWrenchItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

import org.jetbrains.annotations.Nullable;

public class AutoplayControllerBlock extends BaseEntityBlock 
{
    
    public AutoplayControllerBlock(Properties properties) 
    {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() 
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
        return level.isClientSide ? null : createTickerHelper(type, OngakuMod.AUTOPLAY_BLOCK_ENTITY.get(), AutoplayControllerBlockEntity::serverTick);
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
                List<ItemStack> playlistDiscs = controller.buildCollapsedPlaylist().stream().map(entry -> entry.stack().copy()).toList();
                serverPlayer.openMenu(
                    new SimpleMenuProvider((id, inv, p) -> 
                        new AutoplayMenu(id,inv,controller,controller.data,playlistDiscs),
                        Component.literal("Autoplay Controller")
                    ),
                    buf -> {
                        buf.writeBlockPos(pos);
                        ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode(buf,playlistDiscs);
                    }
                );
            }
        }
        return ItemInteractionResult.SUCCESS;
    }
}
