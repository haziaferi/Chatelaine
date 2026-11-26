package com.hazia.chatelaine.core.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ClockAndIdTest {

    // SPEC:R52 every record has a UUID made on the device.
    @Test
    fun idsAreDistinct_SPEC_R52() {
        val ids = List(1_000) { Ids.new() }

        assertEquals(1_000, ids.toSet().size)
        assertTrue(ids.all { it.length == 36 && it[8] == '-' && it[14] == '4' }, "random (version 4) UUIDs")
    }

    @Test
    fun clockNeverGoesBackwardsAndOutranksWhatItHasSeen() {
        var wall = 100L
        val clock = HybridClock("A", { wall })

        val first = clock.tick()
        wall = 50
        val second = clock.tick()
        clock.observe(Stamp(1_000, "B"))
        val third = clock.tick()

        assertEquals(100, first.hlc)
        assertEquals(101, second.hlc)
        assertEquals(1_001, third.hlc)
    }

    @Test
    fun aSeededClockCannotIssueAnOlderStamp() {
        val clock = HybridClock("A", { 10 }, last = 500)

        assertEquals(501, clock.tick().hlc)
    }

    @Test
    fun aGroupRefusesAValueTypeThatWouldCompareUnequalToItsTwin() {
        assertFailsWith<IllegalArgumentException> { Group(Stamp(1, "A"), mapOf("minutes" to 5)) }
        Group(Stamp(1, "A"), mapOf("minutes" to 5L, "note" to null))
    }

    @Test
    fun aGroupRefusesADoubleThatNeverEqualsItself() {
        assertFailsWith<IllegalArgumentException> { Group(Stamp(1, "A"), mapOf("hours" to Double.NaN)) }
        assertFailsWith<IllegalArgumentException> { Group(Stamp(1, "A"), mapOf("hours" to Double.POSITIVE_INFINITY)) }
        Group(Stamp(1, "A"), mapOf("hours" to 1.5))
    }

    @Test
    fun stampsOrderByClockThenDevice() {
        assertTrue(Stamp(1, "B") > Stamp(1, "A"))
        assertTrue(Stamp(2, "A") > Stamp(1, "B"))
    }
}
