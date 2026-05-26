package com.azulc.ongakumod.client;

import java.util.List;

import com.azulc.ongakumod.OngakuModClient;
import com.azulc.ongakumod.blockentity.DiscRackBlockEntity;
import com.azulc.ongakumod.util.DiscColorCache;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class DiscRackRenderer implements BlockEntityRenderer<DiscRackBlockEntity> 
{
    private final net.minecraft.client.renderer.block.BlockRenderDispatcher blockRenderer;

    public DiscRackRenderer(BlockEntityRendererProvider.Context ctx) 
    {
        this.blockRenderer = ctx.getBlockRenderDispatcher();
    }

    @Override
    public void render(DiscRackBlockEntity rack, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) 
    {
        Direction dir = rack.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        BakedModel model = blockRenderer.getBlockModelShaper().getModelManager().getModel(OngakuModClient.VinylModel);

        ms.pushPose(); 
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(-dir.toYRot()));
        double startZ = -0.445;
        double spacing = 0.125;
        double yOffset = -0.44;

        for (int i = 0; i < rack.getContainerSize(); i++) 
        {
            ItemStack stack = rack.getItem(i);
            if (!stack.isEmpty()) 
            {
                ms.pushPose(); 
                double currentZ = startZ + (i * spacing);
                ms.translate(-0.5, yOffset, currentZ); 
                DiscColorCache.DiscColors colors = DiscColorCache.getColors(stack.getItem());
                renderColoredModel(ms.last(), buffer, model, colors, light, overlay);
                ms.popPose(); 
            }
        }
        ms.popPose(); 
    }

    @SuppressWarnings("deprecation")
    private void renderColoredModel(PoseStack.Pose pose, MultiBufferSource buffer, BakedModel model, DiscColorCache.DiscColors colors, int light, int overlay) 
    {
        for (Direction direction : Direction.values()) 
        {
            renderQuads(pose, buffer, model.getQuads(null, direction, net.minecraft.util.RandomSource.create(42)), colors, light, overlay);
        }
        renderQuads(pose, buffer, model.getQuads(null, null, net.minecraft.util.RandomSource.create(42)), colors, light, overlay);
    }

    private void renderQuads(PoseStack.Pose pose, MultiBufferSource buffer, List<BakedQuad> quads, DiscColorCache.DiscColors colors, int light, int overlay) 
    {
        VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());
        for (BakedQuad quad : quads) 
        {
            int color =  switch (quad.getTintIndex())
            {
                case 0 -> colors.vinylColor();
                case 1 -> colors.labelColor();
                case 2 -> colors.OutlineColor();
                default -> colors.vinylColor();
            };
            
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            float a = ((color >> 24) & 0xFF) / 255.0f;

            consumer.putBulkData(pose, quad, r, g, b, a, light, overlay, true);
        }
    }

}