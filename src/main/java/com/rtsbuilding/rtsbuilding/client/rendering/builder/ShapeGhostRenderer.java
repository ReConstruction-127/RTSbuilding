package com.rtsbuilding.rtsbuilding.client.rendering.builder;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rtsbuilding.rtsbuilding.client.screen.BuilderScreen;
import com.rtsbuilding.rtsbuilding.client.screen.shape.ShapeDataRecords;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * 形状建造预览渲染器
 * 负责在BuilderScreen中渲染快速建造形状的幽灵预览（如墙体、地板等）
 */
public final class ShapeGhostRenderer {

    private static final float AREA_LINE_R = 0.35F;
    private static final float AREA_LINE_G = 0.78F;
    private static final float AREA_LINE_B = 1.0F;
    private static final float AREA_LINE_A = 0.95F;

    private static final float AREA_BLOCK_R = 0.12F;
    private static final float AREA_BLOCK_G = 0.56F;
    private static final float AREA_BLOCK_B = 1.0F;
    private static final float AREA_BLOCK_A = 0.11F;

    /**
     * 私有构造函数，防止实例化
     */
    private ShapeGhostRenderer() {
    }

    /**
     * 渲染形状建造的幽灵预览
     *
     * @param minecraft Minecraft客户端实例
     * @param poseStack 姿势栈，用于坐标变换
     * @param lineBuffer 线条缓冲区
     * @param fillBuffer 填充缓冲区
     */
    public static void renderShapeGhostPreview(Minecraft minecraft, PoseStack poseStack, VertexConsumer lineBuffer,
            VertexConsumer fillBuffer) {
        // 仅在BuilderScreen中渲染
        if (!(minecraft.screen instanceof BuilderScreen builderScreen)) {
            return;
        }

        ShapeDataRecords.GhostPreview preview = builderScreen.getShapeGhostPreview();
        if (preview.blocks().isEmpty()) {
            return;
        }

        // 范围破坏高度选择阶段：使用蓝图捕获风格的渲染（边界框+方块高亮）
        if (builderScreen.isAreaMineHeightPreview()) {
            renderAreaMineGhostPreview(poseStack, lineBuffer, fillBuffer, preview.blocks());
            return;
        }

        // 根据是否可以确认来选择颜色
        // 可确认：绿色系；不可确认：青色系
        float lineR = preview.readyConfirm() ? 0.45F : 0.30F;
        float lineG = preview.readyConfirm() ? 0.95F : 0.75F;
        float lineB = preview.readyConfirm() ? 0.45F : 1.00F;
        float fillR = preview.readyConfirm() ? 0.24F : 0.16F;
        float fillG = preview.readyConfirm() ? 0.72F : 0.55F;
        float fillB = preview.readyConfirm() ? 0.24F : 0.90F;
        float fillA = preview.readyConfirm() ? 0.22F : 0.16F;

        // 绘制所有方块的半透明填充
        for (BlockPos pos : preview.blocks()) {
            double minX = pos.getX() + 0.03D;
            double minY = pos.getY() + 0.03D;
            double minZ = pos.getZ() + 0.03D;
            double maxX = pos.getX() + 0.97D;
            double maxY = pos.getY() + 0.97D;
            double maxZ = pos.getZ() + 0.97D;

            LevelRenderer.addChainedFilledBoxVertices(
                    poseStack,
                    fillBuffer,
                    minX, minY, minZ,
                    maxX, maxY, maxZ,
                    fillR, fillG, fillB, fillA);
        }

        // 绘制所有方块的边框线
        for (BlockPos pos : preview.blocks()) {
            double minX = pos.getX() + 0.03D;
            double minY = pos.getY() + 0.03D;
            double minZ = pos.getZ() + 0.03D;
            double maxX = pos.getX() + 0.97D;
            double maxY = pos.getY() + 0.97D;
            double maxZ = pos.getZ() + 0.97D;

            LevelRenderer.renderLineBox(
                    poseStack,
                    lineBuffer,
                    minX, minY, minZ,
                    maxX, maxY, maxZ,
                    lineR, lineG, lineB,
                    0.95F);
        }
    }

    /**
     * 以蓝图捕获风格渲染范围破坏预览：边界框 + 方块填充高亮
     */
    private static void renderAreaMineGhostPreview(PoseStack poseStack, VertexConsumer lineBuffer,
            VertexConsumer fillBuffer, List<BlockPos> blocks) {
        // 计算包围盒
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : blocks) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX() + 1);
            maxY = Math.max(maxY, pos.getY() + 1);
            maxZ = Math.max(maxZ, pos.getZ() + 1);
        }

        // 渲染每个方块的小尺寸半透明蓝色填充
        for (BlockPos pos : blocks) {
            LevelRenderer.addChainedFilledBoxVertices(
                    poseStack,
                    fillBuffer,
                    pos.getX() + 0.04D, pos.getY() + 0.04D, pos.getZ() + 0.04D,
                    pos.getX() + 0.96D, pos.getY() + 0.96D, pos.getZ() + 0.96D,
                    AREA_BLOCK_R, AREA_BLOCK_G, AREA_BLOCK_B, AREA_BLOCK_A);
        }

        // 渲染整体边界框
        double boxMinX = minX - 0.01D;
        double boxMinY = minY - 0.01D;
        double boxMinZ = minZ - 0.01D;
        double boxMaxX = maxX + 0.01D;
        double boxMaxY = maxY + 0.01D;
        double boxMaxZ = maxZ + 0.01D;

        LevelRenderer.renderLineBox(
                poseStack,
                lineBuffer,
                boxMinX, boxMinY, boxMinZ,
                boxMaxX, boxMaxY, boxMaxZ,
                AREA_LINE_R, AREA_LINE_G, AREA_LINE_B, AREA_LINE_A);
    }
}
