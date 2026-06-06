package com.rtsbuilding.rtsbuilding.client.rendering.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rtsbuilding.rtsbuilding.client.controller.ClientRtsController;
import com.rtsbuilding.rtsbuilding.client.rendering.util.RaycastHelper;
import com.rtsbuilding.rtsbuilding.client.screen.BuilderScreen;
import com.rtsbuilding.rtsbuilding.client.screen.shape.ShapeBuildTypes;
import com.rtsbuilding.rtsbuilding.client.screen.ultimine.UltimineMode;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Renders 3D corner-bracket highlights around the block or entity the player is currently
 * hovering over. The highlight uses thickened quad-based brackets (rather than thin GL_LINES)
 * so it remains visible at a distance and avoids z-fighting with world geometry.
 *
 * <p><b>Rendering pipeline</b>
 * <ol>
 *   <li>Early-exit checks: rotation-capture, null world/camera, UI occlusion</li>
 *   <li>Ray-cast both blocks and entities, pick the nearest hit</li>
 *   <li>Compute the world-space bounding box (respecting multi-block structures)</li>
 *   <li>Draw thickened corner brackets with a breathing colour animation</li>
 * </ol>
 *
 * <p>All methods are static; this class is never instantiated.
 */
public final class InteractionTargetRenderer {

    // ──────────────────────────────────────────────
    //  Constants – Geometry
    // ──────────────────────────────────────────────

    /** Extra inflation applied to entity bounding boxes so the brackets sit outside the model. */
    private static final double INFLATE = 0.03D;

    /** Small outward offset applied to block brackets to prevent z-fighting. */
    private static final double LINE_OFFSET = 0.01D;

    /** Base half-thickness (in world units) of each bracket quad. */
    private static final double BRACKET_THICKNESS = 0.04D;

    /**
     * Distance threshold (in blocks) at which bracket thickness begins to scale up.
     * Beyond this distance the thickness grows proportionally so brackets stay visible.
     */
    private static final double THICKNESS_SCALE_DISTANCE = 16.0D;

    // ──────────────────────────────────────────────
    //  Constants – Animation
    // ──────────────────────────────────────────────

    /** Frequency of the breathing colour oscillation, in cycles per second. */
    private static final float BREATH_SPEED = 0.2F;

    /** Minimum multiplier applied to each colour channel during the breathing cycle (0…1). */
    private static final float BREATH_MIN_FACTOR = 0.7F;

    // ──────────────────────────────────────────────
    //  Constants – Colours (RGB, no alpha)
    // ──────────────────────────────────────────────

    /** Colour used for entity corner brackets, modulated by the breath factor. */
    private static final float ENTITY_COLOR_R = 0.50F;
    private static final float ENTITY_COLOR_G = 0.80F;
    private static final float ENTITY_COLOR_B = 1.00F;

    /** Colour used for block corner brackets (orange-gold), modulated by the breath factor. */
    private static final float BLOCK_COLOR_R = 0.965F;
    private static final float BLOCK_COLOR_G = 0.608F;
    private static final float BLOCK_COLOR_B = 0.192F;

    /** Maximum ray-cast range for cursor-based hit-testing. */
    private static final double MAX_REACH = 128.0D;

    // ──────────────────────────────────────────────
    //  Internal helpers
    // ──────────────────────────────────────────────

    /** Axis along which a bracket segment extends. Used to determine which quad faces to draw. */
    private enum Axis { X, Y, Z }

    /** Utility constructor; class is never instantiated. */
    private InteractionTargetRenderer() {
    }

    // ══════════════════════════════════════════════
    //  Public API
    // ══════════════════════════════════════════════

    /**
     * Entry-point called every frame by {@code RtsVisualOverlayRenderer}.
     * Performs all early-exit checks, ray-casts against blocks and entities, and
     * draws corner-bracket highlights for the nearest target.
     *
     * @param minecraft   the Minecraft client instance
     * @param controller  the client-side RTS controller (used for rotation-capture check)
     * @param poseStack   current transformation stack (already translated to camera space)
     * @param lineBuffer  {@link VertexConsumer} for the bracket-quad render type
     */
    public static void renderHoveredInteractionTarget(Minecraft minecraft, ClientRtsController controller,
            PoseStack poseStack, VertexConsumer lineBuffer) {
        // ── Fast early-exit checks ──
        if (controller.isRotateCaptured() || minecraft.level == null || minecraft.getCameraEntity() == null) {
            return;
        }

        // ── Skip rendering when the cursor is over BuilderScreen UI elements ──
        if (isInteractionBlockedByUI(minecraft)) {
            return;
        }

        // ── Compute breathing colour factor ──
        float breathFactor = getBreathFactor();

        // ── Build ray-cast vectors ──
        Vec3 camPos = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 viewDir = RaycastHelper.computeCursorRayDirection(minecraft);
        Vec3 rayEnd = camPos.add(viewDir.scale(MAX_REACH));

        // ── Ray-cast blocks and entities ──
        BlockHitResult blockHit = RaycastHelper.raycastBlockFromCursor(minecraft, camPos, rayEnd, false);
        EntityHitResult entityHit = RaycastHelper.raycastEntityFromCursor(minecraft, camPos, rayEnd, viewDir, MAX_REACH);

        // ── Pick the nearest hit ──
        double blockDistSq = blockHit != null ? camPos.distanceToSqr(blockHit.getLocation()) : Double.MAX_VALUE;
        double entityDistSq = entityHit != null ? camPos.distanceToSqr(entityHit.getLocation()) : Double.MAX_VALUE;

        if (entityHit != null && entityDistSq <= blockDistSq) {
            // Entity is closer (or equal) – render entity highlight
            Entity entity = entityHit.getEntity();
            double distance = camPos.distanceTo(entity.getBoundingBox().getCenter());
            renderEntityCornerHighlight(poseStack, lineBuffer, entity, distance, breathFactor);
            return;
        }

        if (blockHit == null || blockHit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        // Block is the nearest target – render block highlight
        BlockPos pos = blockHit.getBlockPos();
        double distance = camPos.distanceTo(Vec3.atCenterOf(pos));
        renderBlockCornerHighlight(minecraft, poseStack, lineBuffer, pos, distance, breathFactor);
    }

    // ══════════════════════════════════════════════
    //  GUI Interaction Check
    // ══════════════════════════════════════════════

    /**
     * Returns {@code true} when the cursor is over a BuilderScreen UI element that should
     * prevent world-target highlighting (e.g. floating windows, shape-build confirmation,
     * chain-ultimine mode).
     *
     * @param minecraft  the Minecraft client instance
     * @return {@code true} if world highlighting should be suppressed
     */
    private static boolean isInteractionBlockedByUI(Minecraft minecraft) {
        if (!(minecraft.screen instanceof BuilderScreen builderScreen)) {
            return false;
        }

        // Compute GUI-space cursor coordinates
        var mcWindow = minecraft.getWindow();
        double mouseX = minecraft.mouseHandler.xpos() * mcWindow.getGuiScaledWidth() / (double) mcWindow.getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * mcWindow.getGuiScaledHeight() / (double) mcWindow.getScreenHeight();

        // Blocked when cursor is outside the world-view area
        if (!builderScreen.isWorldArea(mouseX, mouseY)) {
            return true;
        }

        // Blocked when cursor is over an open floating window
        for (var window : builderScreen.getFloatingWindowLayer().frontToBackWindows()) {
            if (window.isOpen() && window.isInsideWindow(mouseX, mouseY)) {
                return true;
            }
        }

        // Blocked during shape-build confirmation phase
        var shapeSession = builderScreen.getShapeController().getShapeBuildSession();
        if (shapeSession != null && shapeSession.phase() == ShapeBuildTypes.Phase.READY_CONFIRM) {
            return true;
        }

        // Blocked in chain-ultimine or area-mine height-preview mode
        if (builderScreen.isUltimineOpen()) {
            return builderScreen.getUltimineMode() == UltimineMode.CHAIN || builderScreen.isAreaMineHeightPreview();
        }

        return false;
    }

    // ══════════════════════════════════════════════
    //  Highlight Rendering – Entity & Block
    // ══════════════════════════════════════════════

    /**
     * Renders corner-bracket highlights around an entity's bounding box.
     *
     * @param poseStack    current transformation stack
     * @param lineBuffer   vertex consumer for bracket quads
     * @param entity       the target entity
     * @param distance     distance from the camera to the entity centre (used for thickness scaling)
     * @param breathFactor current breathing animation multiplier
     */
    private static void renderEntityCornerHighlight(PoseStack poseStack, VertexConsumer lineBuffer,
            Entity entity, double distance, float breathFactor) {
        AABB bounds = entity.getBoundingBox().inflate(INFLATE);
        renderCornerBrackets(
                poseStack, lineBuffer,
                bounds.minX, bounds.minY, bounds.minZ,
                bounds.maxX, bounds.maxY, bounds.maxZ,
                ENTITY_COLOR_R * breathFactor,
                ENTITY_COLOR_G * breathFactor,
                ENTITY_COLOR_B * breathFactor,
                distance);
    }

    /**
     * Renders corner-bracket highlights around a block's world-space bounding box.
     * Multi-block structures (double-plants, beds) are merged into a single bounding box.
     *
     * @param minecraft    the Minecraft client instance
     * @param poseStack    current transformation stack
     * @param lineBuffer   vertex consumer for bracket quads
     * @param pos          block position of the targeted block
     * @param distance     distance from the camera to the block centre
     * @param breathFactor current breathing animation multiplier
     */
    private static void renderBlockCornerHighlight(Minecraft minecraft, PoseStack poseStack,
            VertexConsumer lineBuffer, BlockPos pos, double distance, float breathFactor) {
        if (minecraft.level == null) {
            return;
        }

        AABB bounds = computeWorldBounds(minecraft.level, pos);
        if (bounds == null) {
            return;
        }

        double off = LINE_OFFSET;
        renderCornerBrackets(
                poseStack, lineBuffer,
                bounds.minX - off, bounds.minY - off, bounds.minZ - off,
                bounds.maxX + off, bounds.maxY + off, bounds.maxZ + off,
                BLOCK_COLOR_R * breathFactor,
                BLOCK_COLOR_G * breathFactor,
                BLOCK_COLOR_B * breathFactor,
                distance);
    }

    // ══════════════════════════════════════════════
    //  Breathing Colour Animation
    // ══════════════════════════════════════════════

    /**
     * Computes a time-varying scalar in [{@link #BREATH_MIN_FACTOR}, 1.0] that oscillates
     * sinusoidally at {@link #BREATH_SPEED} Hz. Multiplying colour channels by this factor
     * produces a gentle pulsing effect.
     *
     * @return the current breath factor (always in [0.7, 1.0])
     */
    private static float getBreathFactor() {
        double timeSeconds = System.currentTimeMillis() / 1000.0D;
        double phase = timeSeconds * BREATH_SPEED * 2.0D * Math.PI;
        double sin = Math.sin(phase);
        // Map sin ∈ [-1, 1] → factor ∈ [BREATH_MIN_FACTOR, 1.0]
        return (float) ((sin + 1.0D) * 0.5D * (1.0F - BREATH_MIN_FACTOR) + BREATH_MIN_FACTOR);
    }

    // ══════════════════════════════════════════════
    //  Bounds Computation
    // ══════════════════════════════════════════════

    /**
     * Computes the world-space axis-aligned bounding box for the block at {@code pos}.
     * If the block is part of a multi-block structure (double-plant, bed, etc.) the
     * bounding box encompasses all connected blocks.
     *
     * @param level the world
     * @param pos   the targeted block position
     * @return the merged world-space AABB, or {@code null} if the block has no shape
     */
    private static AABB computeWorldBounds(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (isMultiBlockStructure(state)) {
            return computeMultiBlockBoundsBfs(level, pos);
        }

        return computeSingleBlockAABB(level, pos);
    }

    /**
     * Returns {@code true} if the given block state is part of a multi-block structure
     * (e.g. double-tall plants or beds) that requires BFS-based bounds merging.
     *
     * @param state the block state to test
     * @return {@code true} if the state has a multi-block property
     */
    private static boolean isMultiBlockStructure(BlockState state) {
        return state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
            || state.hasProperty(BlockStateProperties.BED_PART);
    }

    /**
     * Performs a BFS from the starting {@code pos} to discover all connected blocks that
     * belong to the same multi-block structure, then returns the merged world-space AABB.
     * <p>
     * Connection directions are determined by {@link #getConnectionDirections} and depend
     * on the block type (vertical for double-plants, horizontal for beds).
     *
     * @param level the world
     * @param pos   the starting block position
     * @return the merged world-space AABB of the entire structure
     */
    private static AABB computeMultiBlockBoundsBfs(Level level, BlockPos pos) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(pos);
        visited.add(pos);

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            BlockState currentState = level.getBlockState(current);

            AABB aabb = computeSingleBlockAABB(level, current);
            if (aabb != null) {
                minX = Math.min(minX, aabb.minX);
                minY = Math.min(minY, aabb.minY);
                minZ = Math.min(minZ, aabb.minZ);
                maxX = Math.max(maxX, aabb.maxX);
                maxY = Math.max(maxY, aabb.maxY);
                maxZ = Math.max(maxZ, aabb.maxZ);
            }

            for (Direction dir : getConnectionDirections(currentState)) {
                BlockPos neighbor = current.relative(dir);
                if (!visited.add(neighbor)) {
                    continue;
                }

                BlockState neighborState = level.getBlockState(neighbor);
                if (neighborState.is(currentState.getBlock()) && isMultiBlockStructure(neighborState)) {
                    queue.add(neighbor);
                }
            }
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Returns the connection direction(s) for a multi-block structure.
     * <ul>
     *   <li><b>Double-block-half</b> (double-plants): connects vertically (UP from LOWER, DOWN from UPPER)</li>
     *   <li><b>Bed</b>: connects horizontally based on the HEAD/FOOT orientation and facing</li>
     * </ul>
     *
     * @param state the block state
     * @return an array of neighbour directions to traverse during BFS
     */
    private static Direction[] getConnectionDirections(BlockState state) {
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            return new Direction[]{half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN};
        }
        if (state.hasProperty(BlockStateProperties.BED_PART)) {
            BedPart part = state.getValue(BlockStateProperties.BED_PART);
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            // HEAD connects towards the foot (opposite of facing), FOOT connects towards the head
            return new Direction[]{part == BedPart.HEAD ? facing.getOpposite() : facing};
        }
        return new Direction[0];
    }

    /**
     * Computes the world-space AABB for a single block by merging all sub-boxes of its
     * {@link VoxelShape}. If the shape is empty (e.g. air or structure-void), returns {@code null}.
     *
     * @param level the world
     * @param pos   the block position
     * @return the merged world-space AABB, or {@code null} if the block has no shape
     */
    private static AABB computeSingleBlockAABB(Level level, BlockPos pos) {
        VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
        if (shape.isEmpty()) {
            return null;
        }

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

        for (AABB box : shape.toAabbs()) {
            minX = Math.min(minX, pos.getX() + box.minX);
            minY = Math.min(minY, pos.getY() + box.minY);
            minZ = Math.min(minZ, pos.getZ() + box.minZ);
            maxX = Math.max(maxX, pos.getX() + box.maxX);
            maxY = Math.max(maxY, pos.getY() + box.maxY);
            maxZ = Math.max(maxZ, pos.getZ() + box.maxZ);
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    // ══════════════════════════════════════════════
    //  Corner-Bracket Rendering
    // ══════════════════════════════════════════════

    /**
     * Renders thickened corner brackets around an AABB using quad-based geometry.
     * <p>
     * The bracket thickness automatically scales with distance beyond
     * {@link #THICKNESS_SCALE_DISTANCE} so they remain clearly visible far away.
     *
     * @param poseStack current transformation stack
     * @param consumer  vertex consumer for bracket quads
     * @param minX      AABB minimum X (world space)
     * @param minY      AABB minimum Y (world space)
     * @param minZ      AABB minimum Z (world space)
     * @param maxX      AABB maximum X (world space)
     * @param maxY      AABB maximum Y (world space)
     * @param maxZ      AABB maximum Z (world space)
     * @param r         red   colour component [0, 1]
     * @param g         green colour component [0, 1]
     * @param b         blue  colour component [0, 1]
     * @param distance  camera-to-target distance (used for thickness scaling)
     */
    private static void renderCornerBrackets(PoseStack poseStack, VertexConsumer consumer,
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ,
            float r, float g, float b,
            double distance) {

        double scaledThickness = BRACKET_THICKNESS * Math.max(1.0D, distance / THICKNESS_SCALE_DISTANCE);
        double halfThick = scaledThickness * 0.5D;

        // Bottom horizontal ring
        drawHorizontalRing(consumer, poseStack, minX, minZ, maxX, maxZ, minY, r, g, b, halfThick);
        // Top horizontal ring
        drawHorizontalRing(consumer, poseStack, minX, minZ, maxX, maxZ, maxY, r, g, b, halfThick);
        // Vertical edges at the four corners
        drawVerticalEdges(consumer, poseStack, minX, minZ, maxX, maxZ, minY, maxY, r, g, b, halfThick);
    }

    /**
     * Draws the four horizontal bracket segments at a given Y-level, forming a rectangular
     * ring. Each segment is a thickened quad extruded along the plane's dominant axis.
     */
    private static void drawHorizontalRing(VertexConsumer consumer, PoseStack poseStack,
            double minX, double minZ, double maxX, double maxZ,
            double y, float r, float g, float b, double t) {
        // X-aligned segment at Z = minZ  (front)
        drawBracketSegment(consumer, poseStack, minX, y, minZ, maxX, y, minZ, r, g, b, Axis.X, t);
        // Z-aligned segment at X = maxX  (right)
        drawBracketSegment(consumer, poseStack, maxX, y, minZ, maxX, y, maxZ, r, g, b, Axis.Z, t);
        // X-aligned segment at Z = maxZ  (back)
        drawBracketSegment(consumer, poseStack, maxX, y, maxZ, minX, y, maxZ, r, g, b, Axis.X, t);
        // Z-aligned segment at X = minX  (left)
        drawBracketSegment(consumer, poseStack, minX, y, maxZ, minX, y, minZ, r, g, b, Axis.Z, t);
    }

    /**
     * Draws the four vertical bracket segments at the four corners of the AABB.
     */
    private static void drawVerticalEdges(VertexConsumer consumer, PoseStack poseStack,
            double minX, double minZ, double maxX, double maxZ,
            double minY, double maxY, float r, float g, float b, double t) {
        // Y-aligned at (minX, minZ)
        drawBracketSegment(consumer, poseStack, minX, minY, minZ, minX, maxY, minZ, r, g, b, Axis.Y, t);
        // Y-aligned at (maxX, minZ)
        drawBracketSegment(consumer, poseStack, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, Axis.Y, t);
        // Y-aligned at (maxX, maxZ)
        drawBracketSegment(consumer, poseStack, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, Axis.Y, t);
        // Y-aligned at (minX, maxZ)
        drawBracketSegment(consumer, poseStack, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, Axis.Y, t);
    }

    /**
     * Draws a single thickened bracket segment from (x1, y1, z1) to (x2, y2, z2).
     * The segment is rendered as two perpendicular quads that form a cross-shaped cross-section,
     * giving the line visible thickness from any viewing angle.
     * <p>
     * Which face pair is used depends on the dominant {@link Axis} of the segment:
     * <ul>
     *   <li><b>X</b>: one quad expands in Y, the other in Z</li>
     *   <li><b>Y</b>: one quad expands in Z, the other in X</li>
     *   <li><b>Z</b>: one quad expands in X, the other in Y</li>
     * </ul>
     *
     * @param consumer vertex consumer
     * @param poseStack current transformation stack
     * @param x1,y1,z1  start point
     * @param x2,y2,z2  end point
     * @param r,g,b     colour components
     * @param axis      dominant axis of the segment (determines which quads to draw)
     * @param t         half-thickness of the bracket
     */
    private static void drawBracketSegment(VertexConsumer consumer, PoseStack poseStack,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            float r, float g, float b, Axis axis,
            double t) {
        switch (axis) {
            case X -> {
                // Quad expanding in Y: faces YZ-plane
                quad(consumer, poseStack,
                        x1, y1 - t, z1,
                        x1, y1 + t, z1,
                        x2, y2 + t, z2,
                        x2, y2 - t, z2, r, g, b);
                // Quad expanding in Z: faces XY-plane
                quad(consumer, poseStack,
                        x1, y1, z1 - t,
                        x1, y1, z1 + t,
                        x2, y2, z2 + t,
                        x2, y2, z2 - t, r, g, b);
            }
            case Y -> {
                // Quad expanding in Z: faces XZ-plane
                quad(consumer, poseStack,
                        x1, y1, z1 - t,
                        x1, y1, z1 + t,
                        x2, y2, z2 + t,
                        x2, y2, z2 - t, r, g, b);
                // Quad expanding in X: faces YZ-plane
                quad(consumer, poseStack,
                        x1 - t, y1, z1,
                        x1 + t, y1, z1,
                        x2 + t, y2, z2,
                        x2 - t, y2, z2, r, g, b);
            }
            case Z -> {
                // Quad expanding in X: faces YZ-plane
                quad(consumer, poseStack,
                        x1 - t, y1, z1,
                        x1 + t, y1, z1,
                        x2 + t, y2, z2,
                        x2 - t, y2, z2, r, g, b);
                // Quad expanding in Y: faces XZ-plane
                quad(consumer, poseStack,
                        x1, y1 - t, z1,
                        x1, y1 + t, z1,
                        x2, y2 + t, z2,
                        x2, y2 - t, z2, r, g, b);
            }
        }
    }

    /**
     * Emits a single coloured quad to the vertex consumer.
     * The quad is defined by four vertices in clockwise or counter-clockwise order
     * (the winding order is irrelevant because back-face culling is disabled for this
     * render type).
     *
     * @param consumer vertex consumer
     * @param poseStack current transformation stack
     * @param x1,y1,z1  vertex 1
     * @param x2,y2,z2  vertex 2
     * @param x3,y3,z3  vertex 3
     * @param x4,y4,z4  vertex 4
     * @param r,g,b     colour components (alpha is always 1.0)
     */
    private static void quad(VertexConsumer consumer, PoseStack poseStack,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3,
            double x4, double y4, double z4,
            float r, float g, float b) {
        consumer.addVertex(poseStack.last(), (float) x1, (float) y1, (float) z1).setColor(r, g, b, 1.0F);
        consumer.addVertex(poseStack.last(), (float) x2, (float) y2, (float) z2).setColor(r, g, b, 1.0F);
        consumer.addVertex(poseStack.last(), (float) x3, (float) y3, (float) z3).setColor(r, g, b, 1.0F);
        consumer.addVertex(poseStack.last(), (float) x4, (float) y4, (float) z4).setColor(r, g, b, 1.0F);
    }
}
