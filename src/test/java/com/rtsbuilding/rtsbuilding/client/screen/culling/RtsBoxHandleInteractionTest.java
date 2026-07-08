package com.rtsbuilding.rtsbuilding.client.screen.culling;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RtsBoxHandleInteractionTest {
    @Test
    void clickingActiveHandleAgainReleasesWheelOwnership() {
        RtsBoxHandleInteraction interaction = new RtsBoxHandleInteraction();
        RtsCullingBox box = singleBlockBox();
        Vec3 origin = new Vec3(10.5D, 67.0D, 10.5D);
        Vec3 direction = new Vec3(0.0D, -1.0D, 0.0D);

        RtsBoxHandleInteraction.ClickResult first = interaction.clickHandle(box, origin, direction);
        assertEquals(RtsBoxHandleInteraction.ClickKind.SELECTED, first.kind());
        assertEquals(Direction.UP, interaction.activeDirection());

        RtsBoxHandleInteraction.ClickResult second = interaction.clickHandle(box, origin, direction);
        assertEquals(RtsBoxHandleInteraction.ClickKind.RELEASED, second.kind());
        assertEquals(Direction.UP, second.direction());
        assertNull(interaction.activeDirection());
        assertFalse(interaction.handleScroll(1.0D, false, (handle, delta) -> true));
    }

    @Test
    void draggingAlongProjectedAxisEmitsAccumulatedResizeSteps() {
        RtsBoxHandleInteraction interaction = new RtsBoxHandleInteraction();
        RtsCullingBox box = singleBlockBox();
        Vec3 origin = new Vec3(10.5D, 67.0D, 10.5D);
        Vec3 direction = new Vec3(0.0D, -1.0D, 0.0D);
        List<Integer> deltas = new ArrayList<>();

        interaction.clickHandle(box, origin, direction);

        assertTrue(interaction.handleDrag(0.0D, -17.0D, 0.0D, -1.0D,
                (handle, delta) -> {
                    deltas.add(delta);
                    return true;
                }));
        assertTrue(deltas.isEmpty(), "不足一格的拖拽只累计，不应立刻改尺寸");

        assertTrue(interaction.handleDrag(0.0D, -2.0D, 0.0D, -1.0D,
                (handle, delta) -> {
                    deltas.add(delta);
                    return true;
                }));
        assertEquals(List.of(1), deltas);

        assertTrue(interaction.handleDrag(0.0D, 19.0D, 0.0D, -1.0D,
                (handle, delta) -> {
                    deltas.add(delta);
                    return true;
                }));
        assertEquals(List.of(1, -1), deltas);
    }

    private static RtsCullingBox singleBlockBox() {
        return new RtsCullingBox(1, new BlockPos(10, 64, 10), new BlockPos(10, 64, 10));
    }
}
