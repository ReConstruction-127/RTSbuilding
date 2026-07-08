package com.rtsbuilding.rtsbuilding.client.rendering.blueprint;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rtsbuilding.rtsbuilding.client.screen.culling.RtsCullingAxisHandle;
import com.rtsbuilding.rtsbuilding.client.screen.culling.RtsCullingBox;
import com.rtsbuilding.rtsbuilding.client.screen.blueprint.BlueprintPanel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Blueprint capture box renderer.
 * Renders the selection box, included block highlights,
 * and excluded block markers during blueprint recording.
 */
public final class BlueprintCaptureRenderer {
    // Max number of included block highlights to prevent performance issues
    private static final int CAPTURE_BLOCK_HIGHLIGHT_LIMIT = 8192;
    // Max number of excluded block highlights
    private static final int CAPTURE_EXCLUDED_HIGHLIGHT_LIMIT = 1024;

    // Optimisation: extracted colour constants for easy adjustment
    private static final float INCLUDED_BLOCK_R = 0.12F;
    private static final float INCLUDED_BLOCK_G = 0.56F;
    private static final float INCLUDED_BLOCK_B = 1.0F;
    private static final float INCLUDED_BLOCK_A = 0.11F;

    private static final float EXCLUDED_BLOCK_R = 1.0F;
    private static final float EXCLUDED_BLOCK_G = 0.36F;
    private static final float EXCLUDED_BLOCK_B = 0.12F;
    private static final float EXCLUDED_BLOCK_LINE_A = 0.95F;
    private static final float EXCLUDED_BLOCK_FILL_A = 0.24F;
    private static final float EXCLUDED_BLOCK_MARK_A = 0.72F;

    private static final float BOUNDARY_BOX_R = 0.35F;
    private static final float BOUNDARY_BOX_G = 0.78F;
    private static final float BOUNDARY_BOX_B = 1.0F;
    private static final float BOUNDARY_BOX_A = 0.95F;
    private static final float HANDLE_X_R = 1.00F;
    private static final float HANDLE_X_G = 0.34F;
    private static final float HANDLE_X_B = 0.32F;
    private static final float HANDLE_Y_R = 0.36F;
    private static final float HANDLE_Y_G = 1.00F;
    private static final float HANDLE_Y_B = 0.42F;
    private static final float HANDLE_Z_R = 0.38F;
    private static final float HANDLE_Z_G = 0.64F;
    private static final float HANDLE_Z_B = 1.00F;
    private static final float ACTIVE_R = 1.00F;
    private static final float ACTIVE_G = 0.78F;
    private static final float ACTIVE_B = 0.18F;

    /**
     * Private constructor to prevent instantiation.
     */
    private BlueprintCaptureRenderer() {
    }

    /**
     * Renders the blueprint capture selection box and highlights.
     *
     * @param poseStack  Pose stack for coordinate transforms
     * @param lineBuffer Line vertex buffer
     * @param fillBuffer Fill vertex buffer
     */
    public static void renderBlueprintCaptureBox(PoseStack poseStack, VertexConsumer lineBuffer, VertexConsumer fillBuffer,
            VertexConsumer handleLineBuffer, VertexConsumer handleFillBuffer) {
        RtsCullingBox box = BlueprintPanel.getCapturePreviewBoxForRender();
        if (box == null) {
            return;
        }

        // Compute bounding box edges (expand 0.01 units to prevent Z-fighting)
        double minX = box.min().getX() - 0.01D;
        double minY = box.min().getY() - 0.01D;
        double minZ = box.min().getZ() - 0.01D;
        double maxX = box.max().getX() + 1.01D;
        double maxY = box.max().getY() + 1.01D;
        double maxZ = box.max().getZ() + 1.01D;

        // Get the list of included blocks (subject to limit)
        List<BlockPos> includedBlocks = BlueprintPanel.getCaptureIncludedBlocksForRender(CAPTURE_BLOCK_HIGHLIGHT_LIMIT);

        // Render a translucent blue fill when not showing individual highlights
        // Render blue highlights for each included block
        for (BlockPos pos : includedBlocks) {
            LevelRenderer.addChainedFilledBoxVertices(
                    poseStack,
                    fillBuffer,
                    pos.getX() + 0.04D, pos.getY() + 0.04D, pos.getZ() + 0.04D,
                    pos.getX() + 0.96D, pos.getY() + 0.96D, pos.getZ() + 0.96D,
                    INCLUDED_BLOCK_R, INCLUDED_BLOCK_G, INCLUDED_BLOCK_B, INCLUDED_BLOCK_A);
        }

        // Render red wireframe for each excluded block
        for (BlockPos pos : BlueprintPanel.getCaptureExcludedBlocksForRender(CAPTURE_EXCLUDED_HIGHLIGHT_LIMIT)) {
            LevelRenderer.addChainedFilledBoxVertices(
                    poseStack,
                    fillBuffer,
                    pos.getX() + 0.07D, pos.getY() + 0.07D, pos.getZ() + 0.07D,
                    pos.getX() + 0.93D, pos.getY() + 0.93D, pos.getZ() + 0.93D,
                    EXCLUDED_BLOCK_R, EXCLUDED_BLOCK_G, EXCLUDED_BLOCK_B, EXCLUDED_BLOCK_FILL_A);
            LevelRenderer.addChainedFilledBoxVertices(
                    poseStack,
                    fillBuffer,
                    pos.getX() + 0.18D, pos.getY() + 0.91D, pos.getZ() + 0.18D,
                    pos.getX() + 0.82D, pos.getY() + 0.99D, pos.getZ() + 0.82D,
                    EXCLUDED_BLOCK_R, EXCLUDED_BLOCK_G, EXCLUDED_BLOCK_B, EXCLUDED_BLOCK_MARK_A);
            LevelRenderer.renderLineBox(
                    poseStack,
                    lineBuffer,
                    pos.getX() + 0.06D, pos.getY() + 0.06D, pos.getZ() + 0.06D,
                    pos.getX() + 0.94D, pos.getY() + 0.94D, pos.getZ() + 0.94D,
                    EXCLUDED_BLOCK_R, EXCLUDED_BLOCK_G, EXCLUDED_BLOCK_B, EXCLUDED_BLOCK_LINE_A);
        }

        // Render the blue bounding box outline for the entire selection
        LevelRenderer.renderLineBox(
                poseStack,
                lineBuffer,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                BOUNDARY_BOX_R, BOUNDARY_BOX_G, BOUNDARY_BOX_B, BOUNDARY_BOX_A);
        if (BlueprintPanel.isCaptureSelectionComplete()) {
            renderAxisHandles(
                    poseStack,
                    handleLineBuffer,
                    handleFillBuffer,
                    box,
                    BlueprintPanel.getCaptureHoveredHandleDirection(),
                    BlueprintPanel.getCaptureActiveHandleDirection());
        }
    }

    private static void renderAxisHandles(PoseStack poseStack, VertexConsumer lineBuffer, VertexConsumer fillBuffer,
            RtsCullingBox box, Direction hoveredDirection, Direction activeDirection) {
        for (RtsCullingAxisHandle.Handle handle : RtsCullingAxisHandle.handles(box)) {
            boolean hovered = handle.direction() == hoveredDirection;
            boolean active = handle.direction() == activeDirection;
            AxisColor color = active ? new AxisColor(ACTIVE_R, ACTIVE_G, ACTIVE_B) : color(handle.axis());
            float fillAlpha = active ? 0.58F : hovered ? 0.42F : 0.22F;
            float lineAlpha = active ? 1.00F : hovered ? 0.95F : 0.70F;
            if (active) {
                renderHandleBox(poseStack, lineBuffer, fillBuffer, handle.shaft().inflate(0.06D),
                        color, 0.16F, 0.42F);
                renderHandleBox(poseStack, lineBuffer, fillBuffer, handle.head().inflate(0.08D),
                        color, 0.20F, 0.54F);
            }
            renderHandleBox(poseStack, lineBuffer, fillBuffer, handle.shaft(), color, fillAlpha, lineAlpha);
            renderHandleBox(poseStack, lineBuffer, fillBuffer, handle.head(), color, fillAlpha, lineAlpha);
        }
    }

    private static void renderHandleBox(PoseStack poseStack, VertexConsumer lineBuffer, VertexConsumer fillBuffer,
            AABB box, AxisColor color, float fillAlpha, float lineAlpha) {
        LevelRenderer.addChainedFilledBoxVertices(poseStack, fillBuffer,
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                color.r(), color.g(), color.b(), fillAlpha);
        LevelRenderer.renderLineBox(poseStack, lineBuffer,
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                color.r(), color.g(), color.b(), lineAlpha);
    }

    private static AxisColor color(Direction.Axis axis) {
        return switch (axis) {
            case X -> new AxisColor(HANDLE_X_R, HANDLE_X_G, HANDLE_X_B);
            case Y -> new AxisColor(HANDLE_Y_R, HANDLE_Y_G, HANDLE_Y_B);
            case Z -> new AxisColor(HANDLE_Z_R, HANDLE_Z_G, HANDLE_Z_B);
        };
    }

    private record AxisColor(float r, float g, float b) {
    }
}
