package com.rtsbuilding.rtsbuilding.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RtsCountUtil}.
 */
class RtsCountUtilTest {

    // ======================================================================
    // sanitizeCount
    // ======================================================================

    @Test
    void sanitizeCount_positive_returnsValue() {
        assertEquals(42L, RtsCountUtil.sanitizeCount(42L));
        assertEquals(1L, RtsCountUtil.sanitizeCount(1L));
        assertEquals(Long.MAX_VALUE, RtsCountUtil.sanitizeCount(Long.MAX_VALUE));
    }

    @Test
    void sanitizeCount_zero_returnsZero() {
        assertEquals(0L, RtsCountUtil.sanitizeCount(0L));
    }

    @Test
    void sanitizeCount_negative_returnsZero() {
        assertEquals(0L, RtsCountUtil.sanitizeCount(-1L));
        assertEquals(0L, RtsCountUtil.sanitizeCount(-Long.MAX_VALUE));
        assertEquals(0L, RtsCountUtil.sanitizeCount(Long.MIN_VALUE));
    }

    // ======================================================================
    // saturatedAdd
    // ======================================================================

    @Test
    void saturatedAdd_normal_addsCorrectly() {
        assertEquals(30L, RtsCountUtil.saturatedAdd(10L, 20L));
        assertEquals(0L, RtsCountUtil.saturatedAdd(0L, 0L));
        assertEquals(100L, RtsCountUtil.saturatedAdd(30L, 70L));
    }

    @Test
    void saturatedAdd_negativeInputs_clampedToZero() {
        assertEquals(10L, RtsCountUtil.saturatedAdd(-5L, 10L));
        assertEquals(20L, RtsCountUtil.saturatedAdd(20L, -3L));
        assertEquals(0L, RtsCountUtil.saturatedAdd(-1L, -1L));
    }

    @Test
    void saturatedAdd_maxValue_returnsMaxValue() {
        assertEquals(Long.MAX_VALUE, RtsCountUtil.saturatedAdd(Long.MAX_VALUE, 1L));
        assertEquals(Long.MAX_VALUE, RtsCountUtil.saturatedAdd(Long.MAX_VALUE, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, RtsCountUtil.saturatedAdd(1L, Long.MAX_VALUE));
    }

    @Test
    void saturatedAdd_overflow_returnsMaxValue() {
        assertEquals(Long.MAX_VALUE, RtsCountUtil.saturatedAdd(Long.MAX_VALUE - 1, 2L));
        assertEquals(Long.MAX_VALUE, RtsCountUtil.saturatedAdd(Long.MAX_VALUE - 1, Long.MAX_VALUE - 1));
    }

    // ======================================================================
    // mergeCount
    // ======================================================================

    @Test
    void mergeCount_normal_mergesCorrectly() {
        Map<String, Long> counts = new HashMap<>();
        RtsCountUtil.mergeCount(counts, "minecraft:diamond", 10L);
        assertEquals(10L, counts.get("minecraft:diamond"));

        RtsCountUtil.mergeCount(counts, "minecraft:diamond", 20L);
        assertEquals(30L, counts.get("minecraft:diamond"));
    }

    @Test
    void mergeCount_nullMap_doesNothing() {
        RtsCountUtil.mergeCount(null, "minecraft:diamond", 10L);
        // Should not throw
    }

    @Test
    void mergeCount_nullKey_doesNothing() {
        Map<String, Long> counts = new HashMap<>();
        RtsCountUtil.mergeCount(counts, null, 10L);
        assertTrue(counts.isEmpty());
    }

    @Test
    void mergeCount_blankKey_doesNothing() {
        Map<String, Long> counts = new HashMap<>();
        RtsCountUtil.mergeCount(counts, "", 10L);
        RtsCountUtil.mergeCount(counts, "  ", 10L);
        assertTrue(counts.isEmpty());
    }

    @Test
    void mergeCount_nonPositiveAmount_doesNothing() {
        Map<String, Long> counts = new HashMap<>();
        RtsCountUtil.mergeCount(counts, "minecraft:diamond", 0L);
        RtsCountUtil.mergeCount(counts, "minecraft:diamond", -1L);
        assertTrue(counts.isEmpty());
    }

    @Test
    void mergeCount_overflow_saturates() {
        Map<String, Long> counts = new HashMap<>();
        RtsCountUtil.mergeCount(counts, "minecraft:diamond", Long.MAX_VALUE);
        RtsCountUtil.mergeCount(counts, "minecraft:diamond", 1L);
        assertEquals(Long.MAX_VALUE, counts.get("minecraft:diamond"));
    }

    @Test
    void mergeCount_multipleKeys_mergesIndependently() {
        Map<String, Long> counts = new HashMap<>();
        RtsCountUtil.mergeCount(counts, "minecraft:diamond", 10L);
        RtsCountUtil.mergeCount(counts, "minecraft:iron_ingot", 20L);
        RtsCountUtil.mergeCount(counts, "minecraft:diamond", 5L);
        assertEquals(15L, counts.get("minecraft:diamond"));
        assertEquals(20L, counts.get("minecraft:iron_ingot"));
    }
}
