package com.azulc.ongakumod.client;

import org.joml.Matrix4f;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;

public class AutoplayControllerRenderer implements BlockEntityRenderer<AutoplayControllerBlockEntity> 
{    
    @SuppressWarnings("unused")
    private final net.minecraft.client.renderer.block.BlockRenderDispatcher blockRenderer;

    public AutoplayControllerRenderer(BlockEntityRendererProvider.Context ctx) 
    {
        this.blockRenderer = ctx.getBlockRenderDispatcher();
    }

    @Override
    public void render(AutoplayControllerBlockEntity controller, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) 
    {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.level() == null) return;

        boolean holdingWrench = player.getMainHandItem().is(OngakuMod.TUNING_WRENCH.get()) || player.getOffhandItem().is(OngakuMod.TUNING_WRENCH.get());

        if (holdingWrench) {
            for (BlockPos rackPos : controller.getLinkedRackPositions()) {
                renderLineBetweenBlocks(controller.getBlockPos(), rackPos, poseStack, bufferSource, 0x00FFFF); // Cyan
            }
        }
    }
    
    private void renderLineBetweenBlocks(BlockPos start, BlockPos end, PoseStack poseStack, MultiBufferSource bufferSource, int color) 
    {
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        VertexConsumer buffer = bufferSource.getBuffer(LINES);
        Matrix4f matrix = poseStack.last().pose();
        float dx = end.getX() - start.getX();
        float dy = end.getY() - start.getY();
        float dz = end.getZ() - start.getZ();
        buffer.addVertex(matrix, 0.5f, 0.5f, 0.5f).setColor(red, green, blue, 1.0f).setNormal(0, 1, 0);
        buffer.addVertex(matrix, dx + 0.5f, dy + 0.5f, dz + 0.5f).setColor(red, green, blue, 1.0f).setNormal(0, 1, 0);
    }

    private static final RenderType LINES = RenderType.create("ongaku_lines",DefaultVertexFormat.POSITION_COLOR_NORMAL,VertexFormat.Mode.LINES,256,false,false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                    .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    @Override
    public boolean shouldRenderOffScreen(AutoplayControllerBlockEntity be) {
        return true;
    }
}