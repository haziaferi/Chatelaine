package com.hazia.chatelaine.core.sync

import kotlin.test.Test
import kotlin.test.assertEquals

/** Keep-loser groups: the later text wins, and every text it replaced reaches [SyncStore.lost] on some device. */
class KeepLoserMergeTest {

    private fun device(name: String) = Merger(HybridClock(name, { 0 }), emptySet(), setOf(TEXT)) to MemoryStore()

    private fun version(stamp: Long, device: String, text: String) = Row("block", "b", mapOf(TEXT to Group(Stamp(stamp, device), mapOf("text" to text))))

    private fun MemoryStore.text() = rows.getValue("b").groups.getValue(TEXT).values["text"]

    @Test
    fun theLaterTextWinsAndTheEarlierIsLostOnlyWhenBothChangedIt() {
        val (merger, store) = device("A")
        merger.created(store, version(1, "A", "start"))
        store.put(version(5, "A", "mine"))

        merger.import(store, listOf(version(7, "B", "theirs")), emptyMap())

        assertEquals("theirs", store.text())
        assertEquals(listOf<Any?>("mine"), store.losses.map { it.second.values["text"] })
        // Only they changed it since, and a replay brings nothing.
        merger.import(store, listOf(version(9, "B", "again"), version(7, "B", "theirs")), emptyMap())
        assertEquals(1, store.losses.size)
    }

    @Test
    fun withThreeDevicesEveryReplacedTextIsLostSomewhereWhateverTheOrder() {
        val texts = mapOf("A" to version(11, "A", "a"), "B" to version(12, "B", "b"), "C" to version(13, "C", "c"))
        for (me in texts.keys) for (first in texts.keys - me) {
            val (merger, store) = device(me)
            merger.created(store, version(1, "A", "start"))
            store.put(texts.getValue(me))
            merger.import(store, listOf(texts.getValue(first)), emptyMap())
            merger.import(store, listOf(texts.getValue((texts.keys - me - first).single())), emptyMap())

            assertEquals("c", store.text())
            // This device's own text, when it lost, is always among what it kept.
            if (me != "C") assertEquals(true, store.losses.any { it.second.values["text"] == me.lowercase() }, "$me after $first")
        }
    }

    /** A tie on the clock, the same hlc on A and B: the device id settles it, B's text on both, and A's is kept on both. */
    @Test
    fun aTieOnTheClockGoesToTheHigherDeviceAndTheOtherTextIsKept() {
        val (onA, a) = device("A")
        val (onB, b) = device("B")
        onA.created(a, version(1, "A", "start"))
        onB.created(b, version(1, "A", "start"))
        val fromA = version(5, "A", "A's text").also { a.put(it) }
        val fromB = version(5, "B", "B's text").also { b.put(it) }

        onA.import(a, listOf(fromB), emptyMap())
        onB.import(b, listOf(fromA), emptyMap())

        for (store in listOf(a, b)) {
            assertEquals("B's text", store.text())
            assertEquals(listOf<Any?>("A's text"), store.losses.map { it.second.values["text"] })
        }
    }

    /** A version this device minted itself, coming back after a later local edit, is not a loser. */
    @Test
    fun aDevicesOwnOlderVersionEchoedBackIsNotLost() {
        val (merger, store) = device("A")
        store.put(version(1, "A", "first")) // a row made without a base, as for a derived id
        store.put(version(5, "A", "second"))

        merger.import(store, listOf(version(1, "A", "first")), emptyMap())

        assertEquals("second", store.text())
        assertEquals(emptyList(), store.losses)
    }

    /** After a clash the winner edits again; its winning version echoing back from the peer records nothing. */
    @Test
    fun theWinnerEchoedBackAfterAClashIsNotLost() {
        val (merger, store) = device("A")
        merger.created(store, version(1, "A", "start"))
        store.put(version(5, "A", "mine"))
        merger.import(store, listOf(version(3, "B", "theirs")), emptyMap())
        assertEquals(listOf<Any?>("theirs"), store.losses.map { it.second.values["text"] })
        store.put(version(9, "A", "mine again"))

        merger.import(store, listOf(version(5, "A", "mine")), emptyMap())

        assertEquals("mine again", store.text())
        assertEquals(1, store.losses.size)
    }

    private companion object {
        const val TEXT = "text"
    }
}
