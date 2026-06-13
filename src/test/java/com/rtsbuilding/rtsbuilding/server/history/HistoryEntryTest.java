package com.rtsbuilding.rtsbuilding.server.history;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HistoryEntry}.
 */
class HistoryEntryTest {

    private static final ResourceKey<Level> TEST_DIM = Level.OVERWORLD;

    private static HistoryBlockRecord record(int x, int y, int z) {
        return new HistoryBlockRecord(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState());
    }

    // ======================================================================
    // Construction
    // ======================================================================

    @Test
    void constructor_storesValues() {
        List<HistoryBlockRecord> records = List.of(record(0, 0, 0), record(1, 0, 0));
        HistoryEntry entry = new HistoryEntry(true, records, Direction.UP, TEST_DIM);

        assertTrue(entry.isDestructive());
        assertSame(Direction.UP, entry.getFace());
        assertSame(TEST_DIM, entry.getDimension());
        assertEquals(2, entry.getBlockCount());
        assertNotNull(entry.getEntryId());
        assertTrue(entry.getTimestamp() > 0);
    }

    @Test
    void constructor_immutableBlocksList() {
        List<HistoryBlockRecord> mutable = new ArrayList<>(List.of(record(0, 0, 0)));
        HistoryEntry entry = new HistoryEntry(false, mutable, Direction.UP, TEST_DIM);
        mutable.add(record(1, 0, 0));
        assertEquals(1, entry.getBlockCount());
    }

    @Test
    void construction_blocksCopied() {
        List<HistoryBlockRecord> records = List.of(record(0, 0, 0));
        HistoryEntry entry = new HistoryEntry(false, records, Direction.UP, TEST_DIM);
        assertNotSame(records, entry.getBlocks());
    }

    // ======================================================================
    // Block count
    // ======================================================================

    @Test
    void getBlockCount_zeroBlocks() {
        HistoryEntry entry = new HistoryEntry(true, List.of(), Direction.UP, TEST_DIM);
        assertEquals(0, entry.getBlockCount());
    }

    @Test
    void getBlockCount_manyBlocks() {
        List<HistoryBlockRecord> records = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            records.add(record(i, 0, 0));
        }
        HistoryEntry entry = new HistoryEntry(true, records, Direction.UP, TEST_DIM);
        assertEquals(1000, entry.getBlockCount());
    }

    // ======================================================================
    // Expiry
    // ======================================================================

    @Test
    void isExpired_defaultExpiry_recentEntryNotExpired() {
        HistoryEntry entry = new HistoryEntry(true, List.of(record(0, 0, 0)), Direction.UP, TEST_DIM);
        assertFalse(entry.isExpired());
    }

    @Test
    void isExpired_customExpiry_veryShort_expired() throws Exception {
        HistoryEntry entry = new HistoryEntry(true, List.of(record(0, 0, 0)), Direction.UP, TEST_DIM);
        // Wait 10ms then check with 1ms expiry
        Thread.sleep(10);
        assertTrue(entry.isExpired(1));
    }

    @Test
    void isExpired_customExpiry_withinLimit_notExpired() {
        HistoryEntry entry = new HistoryEntry(true, List.of(record(0, 0, 0)), Direction.UP, TEST_DIM);
        assertFalse(entry.isExpired(Long.MAX_VALUE));
    }

    // ======================================================================
    // removeRestored
    // ======================================================================

    @Test
    void removeRestored_allRestored_returnsNull() {
        List<HistoryBlockRecord> records = List.of(record(0, 0, 0), record(1, 0, 0));
        HistoryEntry entry = new HistoryEntry(false, records, Direction.UP, TEST_DIM);
        assertNull(entry.removeRestored(2));
        assertNull(entry.removeRestored(100));
        assertNull(entry.removeRestored(Integer.MAX_VALUE));
    }

    @Test
    void removeRestored_zeroRestored_returnsSameCount() {
        List<HistoryBlockRecord> records = List.of(record(0, 0, 0), record(1, 0, 0));
        HistoryEntry entry = new HistoryEntry(false, records, Direction.UP, TEST_DIM);
        HistoryEntry remaining = entry.removeRestored(0);
        assertNotNull(remaining);
        assertEquals(2, remaining.getBlockCount());
    }

    @Test
    void removeRestored_partialRestored_returnsRemaining() {
        List<HistoryBlockRecord> records = List.of(
                record(0, 0, 0), record(1, 0, 0), record(2, 0, 0));
        HistoryEntry entry = new HistoryEntry(false, records, Direction.UP, TEST_DIM);
        HistoryEntry remaining = entry.removeRestored(1);
        assertNotNull(remaining);
        assertEquals(2, remaining.getBlockCount());
        assertEquals(new BlockPos(1, 0, 0), remaining.getBlocks().get(0).pos());
        assertEquals(new BlockPos(2, 0, 0), remaining.getBlocks().get(1).pos());
    }

    @Test
    void removeRestored_preservesIsDestructive() {
        List<HistoryBlockRecord> records = List.of(record(0, 0, 0), record(1, 0, 0));
        HistoryEntry destructEntry = new HistoryEntry(true, records, Direction.UP, TEST_DIM);
        assertTrue(destructEntry.removeRestored(1).isDestructive());

        HistoryEntry placeEntry = new HistoryEntry(false, records, Direction.UP, TEST_DIM);
        assertFalse(placeEntry.removeRestored(1).isDestructive());
    }

    @Test
    void removeRestored_preservesFace() {
        List<HistoryBlockRecord> records = List.of(record(0, 0, 0));
        HistoryEntry entry = new HistoryEntry(false, records, Direction.NORTH, TEST_DIM);
        assertEquals(Direction.NORTH, entry.removeRestored(0).getFace());
    }

    // ======================================================================
    // Edge cases
    // ======================================================================

    @Test
    void constructor_nullBlocks_throws() {
        assertThrows(NullPointerException.class,
                () -> new HistoryEntry(true, null, Direction.UP, TEST_DIM));
    }

    @Test
    void getBlocks_returnsImmutableList() {
        HistoryEntry entry = new HistoryEntry(true, List.of(record(0, 0, 0)), Direction.UP, TEST_DIM);
        assertThrows(UnsupportedOperationException.class,
                () -> entry.getBlocks().add(record(1, 0, 0)));
    }

    @Test
    void blocksAreImmutableAfterConstruction() {
        BlockPos pos = new BlockPos(0, 0, 0);
        BlockState state = Blocks.STONE.defaultBlockState();
        HistoryBlockRecord mutableRecord = new HistoryBlockRecord(pos, state);
        HistoryEntry entry = new HistoryEntry(false, List.of(mutableRecord), Direction.UP, TEST_DIM);
        // Check that the block pos was made immutable
        assertSame(pos.immutable(), entry.getBlocks().get(0).pos());
    }
}
