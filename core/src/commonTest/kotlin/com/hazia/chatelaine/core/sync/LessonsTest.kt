package com.hazia.chatelaine.core.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Two engine lessons: a replay of an old version changes nothing, and a purged row stays purged. */
class LessonsTest {

    /**
     * The first import is the older copy of a row both devices started from; the second import
     * replays it. Neither may touch the row or raise a question, though A has edited the
     * schedule since, so that a version no newer than the base would look like a clash.
     */
    @Test
    fun replayedVersionNoNewerThanBaseIsIgnored() {
        val a = Device("A")
        val b = Device("B")
        a.create("x", 0)
        sync(listOf(a, b), 1)
        val older = b.store.rows.getValue("x")
        a.edit("x", 10, SCHEDULE, "due" to "d1")
        val edited = a.store.rows.getValue("x")

        a.merger.import(a.store, listOf(older), emptyMap())
        assertEquals(edited, a.store.rows.getValue("x"))
        assertFalse(a.asked("x"))
        a.merger.import(a.store, listOf(older), emptyMap())
        assertEquals(edited, a.store.rows.getValue("x"))
        assertFalse(a.asked("x"))
    }

    @Test
    fun purgedRowNeverComesBack() {
        val a = Device("A")
        val b = Device("B")
        a.create("x", 0)
        sync(listOf(a, b), 1)
        val older = b.store.rows.getValue("x")

        a.purge("x", 5)
        a.merger.import(a.store, listOf(older), emptyMap())

        assertNull(a.store.row("x"))
        assertNotNull(a.store.purge("x"))
    }
}
