package com.rtsbuilding.rtsbuilding.server.history;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ServerHistoryManager}.
 *
 * <p>Tests focus on undo-stack management: push, pop, capacity limits,
 * clearing, and expiry cleanup. World-interaction methods (captureBlock,
 * executeUndo) are tested through mocked {@link ServerLevel}.</p>
 */
class ServerHistoryManagerTest {

    private ServerPlayer player;
    private ServerLevel level;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        player = mock(ServerPlayer.class);
        level = mock(ServerLevel.class);
        playerId = UUID.randomUUID();

        when(player.getUUID()).thenReturn(playerId);
        when(player.serverLevel()).thenReturn(level);

        // Make captureBlock succeed
        when(level.isLoaded(any(BlockPos.class))).thenReturn(true);
        BlockState stone = Blocks.STONE.defaultBlockState();
        when(level.getBlockState(any(BlockPos.class))).thenReturn(stone);
        when(level.getBlockEntity(any(BlockPos.class))).thenReturn(null);

        // Clean state before each test
        ServerHistoryManager.clear(playerId);
    }

    @AfterEach
    void tearDown() {
        ServerHistoryManager.clear(playerId);
    }

    // ======================================================================
    // recordPlacement
    // ======================================================================

    @Test
    void recordPlacement_addsEntry() {
        List<BlockPos> positions = List.of(new BlockPos(0, 0, 0), new BlockPos(1, 0, 0));
        ServerHistoryManager.recordPlacement(player, positions, Direction.UP);

        assertEquals(1, ServerHistoryManager.getUndoSize(playerId));
    }

    @Test
    void recordPlacement_nullPlayer_doesNothing() {
        ServerHistoryManager.recordPlacement(null, List.of(BlockPos.ZERO), Direction.UP);
        // Should not throw
    }

    @Test
    void recordPlacement_emptyPositions_doesNothing() {
        ServerHistoryManager.recordPlacement(player, List.of(), Direction.UP);
        assertEquals(0, ServerHistoryManager.getUndoSize(playerId));
    }

    @Test
    void recordPlacement_nullPositions_doesNothing() {
        ServerHistoryManager.recordPlacement(player, null, Direction.UP);
        assertEquals(0, ServerHistoryManager.getUndoSize(playerId));
    }

    @Test
    void recordPlacement_unloadedPositions_skipped() {
        when(level.isLoaded(any(BlockPos.class))).thenReturn(false);
        ServerHistoryManager.recordPlacement(player, List.of(BlockPos.ZERO), Direction.UP);
        assertEquals(0, ServerHistoryManager.getUndoSize(playerId));
    }

    // ======================================================================
    // recordBreak
    // ======================================================================

    @Test
    void recordBreak_addsEntry() {
        ServerHistoryManager.recordBreak(player, List.of(BlockPos.ZERO), Direction.UP);
        assertEquals(1, ServerHistoryManager.getUndoSize(playerId));
    }

    @Test
    void recordBreak_multiplePositions_addsSingleEntry() {
        ServerHistoryManager.recordBreak(player,
                List.of(new BlockPos(0, 0, 0), new BlockPos(1, 0, 0), new BlockPos(2, 0, 0)),
                Direction.UP);
        assertEquals(1, ServerHistoryManager.getUndoSize(playerId));
    }

    // ======================================================================
    // undo (pop stack)
    // ======================================================================

    @Test
    void undo_emptyStack_returnsNull() {
        assertNull(ServerHistoryManager.undo(player));
    }

    @Test
    void undo_nullPlayer_returnsNull() {
        assertNull(ServerHistoryManager.undo(null));
    }

    @Test
    void undo_popsMostRecent() {
        ServerHistoryManager.recordPlacement(player, List.of(BlockPos.ZERO), Direction.UP);
        ServerHistoryManager.recordBreak(player, List.of(new BlockPos(1, 0, 0)), Direction.DOWN);

        HistoryEntry entry = ServerHistoryManager.undo(player);
        assertNotNull(entry);
        assertTrue(entry.isDestructive()); // Most recent is break
        assertEquals(Direction.DOWN, entry.getFace());
        assertEquals(1, ServerHistoryManager.getUndoSize(playerId)); // One remains
    }

    // ======================================================================
    // Stack capacity limit
    // ======================================================================

    @Test
    void stackRespectsCapacityLimit() {
        // Push many entries to trigger the SHAPE_HISTORY_LIMIT cap
        for (int i = 0; i < 100; i++) {
            ServerHistoryManager.recordPlacement(player,
                    List.of(new BlockPos(i, 0, 0)), Direction.UP);
        }

        int size = ServerHistoryManager.getUndoSize(playerId);
        assertTrue(size <= com.rtsbuilding.rtsbuilding.common.RtsHistoryConstants.SHAPE_HISTORY_LIMIT,
                "Stack size " + size + " exceeds limit " + com.rtsbuilding.rtsbuilding.common.RtsHistoryConstants.SHAPE_HISTORY_LIMIT);
    }

    // ======================================================================
    // clear
    // ======================================================================

    @Test
    void clear_removesPlayerHistory() {
        ServerHistoryManager.recordPlacement(player, List.of(BlockPos.ZERO), Direction.UP);
        assertEquals(1, ServerHistoryManager.getUndoSize(playerId));

        ServerHistoryManager.clear(playerId);
        assertEquals(0, ServerHistoryManager.getUndoSize(playerId));
    }

    @Test
    void clear_unknownPlayer_doesNothing() {
        ServerHistoryManager.clear(UUID.randomUUID());
        // Should not throw
    }

    // ======================================================================
    // getUndoSize
    // ======================================================================

    @Test
    void getUndoSize_unknownPlayer_returnsZero() {
        assertEquals(0, ServerHistoryManager.getUndoSize(UUID.randomUUID()));
    }

    @Test
    void getUndoSize_null_throws() {
        assertThrows(NullPointerException.class,
                () -> ServerHistoryManager.getUndoSize(null));
    }

    // ======================================================================
    // updateUndoEntry (partial restore support)
    // ======================================================================

    @Test
    void updateUndoEntry_replacesLastEntry() {
        ServerHistoryManager.recordPlacement(player, List.of(BlockPos.ZERO), Direction.UP);
        assertEquals(1, ServerHistoryManager.getUndoSize(playerId));

        HistoryEntry oldEntry = ServerHistoryManager.undo(player);
        assertNotNull(oldEntry);

        // Create a "reduced" entry
        HistoryEntry reduced = oldEntry.removeRestored(0);
        assertNotNull(reduced);

        ServerHistoryManager.recordPlacement(player, List.of(BlockPos.ZERO), Direction.UP);
        ServerHistoryManager.updateUndoEntry(player, reduced);

        assertEquals(1, ServerHistoryManager.getUndoSize(playerId));
    }

    @Test
    void updateUndoEntry_nullPlayer_doesNothing() {
        ServerHistoryManager.updateUndoEntry(null, null);
        // Should not throw
    }

    // ======================================================================
    // cleanupIfNeeded
    // ======================================================================

    @Test
    void cleanupIfNeeded_doesNotThrow() {
        ServerHistoryManager.recordPlacement(player, List.of(BlockPos.ZERO), Direction.UP);
        ServerHistoryManager.cleanupIfNeeded();
        // Should not throw, and recent entries should survive
        assertTrue(ServerHistoryManager.getUndoSize(playerId) > 0);
    }

    // ======================================================================
    // captureBlock
    // ======================================================================

    @Test
    void captureBlock_nullLevel_returnsNull() {
        assertNull(ServerHistoryManager.captureBlock(null, BlockPos.ZERO));
    }

    @Test
    void captureBlock_nullPos_returnsNull() {
        assertNull(ServerHistoryManager.captureBlock(level, null));
    }

    @Test
    void captureBlock_unloaded_returnsNull() {
        when(level.isLoaded(any(BlockPos.class))).thenReturn(false);
        assertNull(ServerHistoryManager.captureBlock(level, BlockPos.ZERO));
    }

    @Test
    void captureBlock_airBlock_returnsNull() {
        when(level.getBlockState(any(BlockPos.class))).thenReturn(
                Blocks.AIR.defaultBlockState());
        assertNull(ServerHistoryManager.captureBlock(level, BlockPos.ZERO));
    }

    @Test
    void captureBlock_validBlock_returnsRecord() {
        HistoryBlockRecord record = ServerHistoryManager.captureBlock(level, BlockPos.ZERO);
        assertNotNull(record);
        assertEquals(BlockPos.ZERO, record.pos());
    }
}
