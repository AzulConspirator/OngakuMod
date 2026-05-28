package com.azulc.ongakumod.client;

import java.util.OptionalDouble;

import org.joml.Matrix4f;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.block.AutoplayControllerBlock;
import com.azulc.ongakumod.blockentity.AutoplayControllerBlockEntity;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public class AutoplayControllerRenderer implements BlockEntityRenderer<AutoplayControllerBlockEntity> 
{    
    @SuppressWarnings("unused")
    private final net.minecraft.client.renderer.block.BlockRenderDispatcher blockRenderer;
    private static final ResourceLocation STATUS_TEX = ResourceLocation.fromNamespaceAndPath("ongakumod", "textures/block/controller_status.png");
    private static final ResourceLocation PROGRESS_TEX = ResourceLocation.fromNamespaceAndPath("ongakumod", "textures/block/controller_progress.png")
    ;
    public AutoplayControllerRenderer(BlockEntityRendererProvider.Context ctx) 
    {
        this.blockRenderer = ctx.getBlockRenderDispatcher();
    }

    @Override
    public void render(AutoplayControllerBlockEntity controller, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) 
    {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.level() == null) return;

        //Render Connection lines from Rack to Controller
        boolean holdingWrench = player.getMainHandItem().is(OngakuMod.TUNING_WRENCH.get()) || player.getOffhandItem().is(OngakuMod.TUNING_WRENCH.get());
        if (holdingWrench) {
            for (BlockPos rackPos : controller.getLinkedRackPositions()) {
                renderLineBetweenBlocks(controller.getBlockPos(), rackPos, poseStack, bufferSource, 0x00FFFF); // Cyan
            }
        }
        // on Block Face Indicators
        Direction facing = controller.getBlockState().getValue(AutoplayControllerBlock.FACING);
        // Status (0-2) and Progress (0-12)
        int statusFrame = controller.data.get(2); 
        int statusFrameOutput = switch (statusFrame) {
            case -1 -> 0;
            case 0 -> 1;
            case 1 -> 2;
            default -> 0;
        };
        long startTick = controller.getSongStartTick();
        int duration = controller.getSongDurationTicks();
        int progressFrame = 0;

        // Explicitly check if a song is actually supposed to be playing
        if (startTick != -1 && duration > 0) {
            long elapsed = controller.getLevel().getGameTime() - startTick;
            
            // Clamp elapsed so network latency doesn't make it negative or overflow
            elapsed = Math.max(0, Math.min(elapsed, duration));
            
            float progress = (float) elapsed / duration;
            progressFrame = Math.round(progress * 12);
        }
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        float degrees = -facing.toYRot() + 180f; 
        poseStack.mulPose(Axis.YP.rotationDegrees(degrees));
        poseStack.translate(-0.5, -0.5, -0.5);
        float zFront = 1.95f / 16f;
        // Render Status
        renderFace(poseStack, bufferSource.getBuffer(RenderType.entityCutout(STATUS_TEX)), 
            5/16f, 12/16f, 7/16f, 14/16f, zFront, statusFrameOutput, 3, packedLight, packedOverlay);
        // Render Progress
        renderFace(poseStack, bufferSource.getBuffer(RenderType.entityCutout(PROGRESS_TEX)), 
            3/16f, 6/16f, 13/16f, 7/16f, zFront, progressFrame, 13, packedLight, packedOverlay);
        poseStack.popPose();
    }
    
    private void renderFace(PoseStack poseStack, VertexConsumer consumer, float minX, float minY, float maxX, float maxY, float z, int frame, int totalFrames, int light, int overlay) {
        Matrix4f matrix = poseStack.last().pose();
        float fH = 1.0f / totalFrames;
        float minV = frame * fH;
        float maxV = minV + fH;

        // --- SIDE A (Counter-Clockwise) ---
        consumer.addVertex(matrix, minX, minY, z).setColor(1f, 1f, 1f, 1f).setUv(0, maxV).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        consumer.addVertex(matrix, maxX, minY, z).setColor(1f, 1f, 1f, 1f).setUv(1, maxV).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        consumer.addVertex(matrix, maxX, maxY, z).setColor(1f, 1f, 1f, 1f).setUv(1, minV).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        consumer.addVertex(matrix, minX, maxY, z).setColor(1f, 1f, 1f, 1f).setUv(0, minV).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);

        // --- SIDE B (Clockwise / Flipped) ---
        // We reverse the X order so the "back" becomes the "front"
        consumer.addVertex(matrix, maxX, minY, z).setColor(1f, 1f, 1f, 1f).setUv(1, maxV).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix, minX, minY, z).setColor(1f, 1f, 1f, 1f).setUv(0, maxV).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix, minX, maxY, z).setColor(1f, 1f, 1f, 1f).setUv(0, minV).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix, maxX, maxY, z).setColor(1f, 1f, 1f, 1f).setUv(1, minV).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
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

    private static final RenderType LINES = RenderType.create("ongaku_lines",
    DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 256, false, false,
    RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
            .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
            .setCullState(RenderStateShard.NO_CULL)
            .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(2.0D))) 
            .createCompositeState(false));

    @Override
    public boolean shouldRenderOffScreen(AutoplayControllerBlockEntity be) {
        return true;
    }
}