package com.azulc.ongakumod.client;

import java.util.List;

import com.azulc.ongakumod.OngakuModClient;
import com.azulc.ongakumod.block.DiscRackBoxBlock;
import com.azulc.ongakumod.block.DiscRackWallBlock;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
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
        Block block = rack.getBlockState().getBlock();
        boolean isWall = block instanceof DiscRackWallBlock;
        boolean isBox = block instanceof DiscRackBoxBlock;
        Direction dir = rack.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        BakedModel _Vinylmodel = blockRenderer.getBlockModelShaper().getModelManager().getModel(OngakuModClient.VinylModel);
        BakedModel _Sleevemodel = blockRenderer.getBlockModelShaper().getModelManager().getModel(OngakuModClient.SleeveModel);

        ms.pushPose(); 
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(-dir.toYRot()));

        if (isWall) 
        {
            // WALL RENDER LOGIC (1 Slot)
            float scale = 0.9f; // Make it bigger for the wall
            ms.scale(scale, scale, scale);
            
            ItemStack stack = rack.getItem(0);
            if (!stack.isEmpty()) {
                DiscColorCache.DiscColors colors = DiscColorCache.getColors(stack.getItem());
                // load Sleeve
                ms.pushPose();
                ms.scale(scale, scale, scale);
                ms.translate(-0.5, -0.35, -0.55);
                //ms.mulPose(Axis.YP.rotationDegrees(180));
                renderColoredModel(ms.last(), buffer, _Sleevemodel,true, colors, light, overlay);
                ms.popPose();
                ms.pushPose();
                // load Vinyl
                ms.translate(0.45, -0.4, -0.35);
                ms.mulPose(Axis.YP.rotationDegrees(180));
                ms.mulPose(Axis.XP.rotationDegrees(5));
                ms.scale(scale, scale, scale);
                renderColoredModel(ms.last(), buffer, _Vinylmodel,false, colors, light, overlay);
                ms.popPose();
            }
        }
        else if(isBox) 
        {
            // BOX RENDER LOGIC (8 Slots)
            float scale = 0.7f;
            ms.scale(scale, scale, scale);
            
            double startZ = -0.3 / scale; 
            double spacing = 0.09 / scale;
            double yOffset = -0.4 / scale;
            for (int i = 0; i < rack.getContainerSize(); i++) 
            {
                ItemStack stack = rack.getItem(i);
                if (!stack.isEmpty()) 
                {
                    ms.pushPose();
                    double currentZ = startZ + (i * spacing);
                    ms.translate(-0.5, yOffset, currentZ);
                    ms.mulPose(Axis.XP.rotationDegrees(-10)); // Slight backward tilt
                    DiscColorCache.DiscColors colors = DiscColorCache.getColors(stack.getItem());
                    renderColoredModel(ms.last(), buffer, _Sleevemodel,true, colors, light, overlay);
                    ms.popPose(); 
                }
            }
        }
        else 
        {
            // RACK RENDER LOGIC (8 Slots)
            float scale = 0.7f;
            ms.scale(scale, scale, scale);
            double startZ = -0.47 / scale; 
            double spacing = 0.125 / scale;
            double yOffset = -0.5 / scale + 0.05;
            for (int i = 0; i < rack.getContainerSize(); i++) 
            {
                ItemStack stack = rack.getItem(i);
                if (!stack.isEmpty()) 
                {
                    ms.pushPose();
                    double currentZ = startZ + (i * spacing);
                    ms.translate(-0.5, yOffset, currentZ); 
                    DiscColorCache.DiscColors colors = DiscColorCache.getColors(stack.getItem());
                    renderColoredModel(ms.last(), buffer, _Vinylmodel,false, colors, light, overlay);
                    ms.popPose(); 
                }
            }
        }
        ms.popPose();
    }

    @SuppressWarnings("deprecation")
    private void renderColoredModel(PoseStack.Pose pose, MultiBufferSource buffer, BakedModel model,Boolean IsSleeve, DiscColorCache.DiscColors colors, int light, int overlay) 
    {
        for (Direction direction : Direction.values()) 
        {
            renderQuads(pose, buffer, model.getQuads(null, direction, net.minecraft.util.RandomSource.create(42)),IsSleeve, colors, light, overlay);
        }
        renderQuads(pose, buffer, model.getQuads(null, null, net.minecraft.util.RandomSource.create(42)),IsSleeve, colors, light, overlay);
    }

    private void renderQuads(PoseStack.Pose pose, MultiBufferSource buffer, List<BakedQuad> quads, Boolean IsSleeve, DiscColorCache.DiscColors colors, int light, int overlay) 
    {
        ResourceLocation customVinylTex = colors.customVinylTex();
        ResourceLocation customSleeveTex = colors.customSleeveTex();
        VertexConsumer consumer;
        if (customVinylTex != null && customSleeveTex != null) 
        {
            consumer = (IsSleeve = true) ? buffer.getBuffer(RenderType.entityCutout(customSleeveTex)) : buffer.getBuffer(RenderType.entityCutout(customVinylTex));
        }
        consumer = buffer.getBuffer(RenderType.cutout());
        for (BakedQuad quad : quads) 
        {
            int color;
            if (customVinylTex != null && customSleeveTex != null) 
            {
                color = 0xFFFFFFFF; // Don't tint if using a custom texture
            } else 
            {
                color =  switch (quad.getTintIndex())
                {
                    case 1 -> colors.Index1Color(); // Vinyl
                    case 2 -> colors.Index2Color(); // Vinyl 1
                    case 3 -> colors.Index3Color(); // Center
                    case 4 -> colors.Index4Color(); // Center
                    case 5 -> colors.Index5Color(); // Center
                    case 6 -> colors.Index6Color(); // Outline
                    case 7 -> colors.Index7Color(); // Outline 1
                    case 8 -> colors.Index8Color(); // Outline 2
                    default -> colors.Index1Color();
                };
            }
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            float a = ((color >> 24) & 0xFF) / 255.0f;
            consumer.putBulkData(pose, quad, r, g, b, a, light, overlay, true);
        }
    }
}