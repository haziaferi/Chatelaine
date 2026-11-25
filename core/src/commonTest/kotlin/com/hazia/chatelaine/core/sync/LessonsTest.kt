package com.hazia.chatelaine.core.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Two engine lessons: a replay changes nothing, and a purged row stays purged. */
class LessonsTest {

    @Test
    fun replayedVersionNoNewerThanBaseIsIgnored() {
        val a = Device("A")
        val b = Device("B")
        a.create("x", 0)
        sync(listOf(a, b), 1)
        val older = b.store.rows.getValue("x")
        b.edit("x", 10, STATUS, "done" to true)
        val newer = b.store.rows.getValue("x")

        a.merger.import(a.store, listOf(newer), emptyMap())
        val afterFirst = a.store.rows.getValue("x")
        a.merger.import(a.store, listOf(newer), emptyMap())
        assertEquals(afterFirst, a.store.rows.getValue("x"))
        a.merger.import(a.store, listOf(older), emptyMap())
        assertEquals(afterFirst, a.store.rows.getValue("x"))
        assertEquals(true, a.view("x")?.done)
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
        assertEquals(true, a.store.purge("x") != null)
    }
}
