package com.hazia.chatelaine.data.app

import com.hazia.chatelaine.data.sync.SyncFolder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/** Decision 9: on start, 3 s after the last local change while something is to export, and every 5 minutes. */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncRunnerTest {

    private object Folder : SyncFolder {
        override fun list(dir: String) = emptyList<String>()
        override fun size(path: String): Long? = null
        override fun read(path: String): ByteArray? = null
        override fun replace(path: String, bytes: ByteArray) = Unit
        override fun delete(path: String) = Unit
    }

    /** When each sync started, in virtual milliseconds. */
    private val started = mutableListOf<Long>()

    private fun TestScope.runner(
        folder: SyncFolder? = Folder,
        writes: Flow<Unit> = emptyFlow(),
        pending: Int = 1,
        sync: suspend (SyncFolder) -> Int = { started += testScheduler.currentTime; 1 },
    ) = SyncRunner(sync, { folder }, writes, { pending }, backgroundScope, wallMillis = { testScheduler.currentTime })

    @Test
    fun noFolderNoSync() = runTest {
        runner(folder = null).start()
        advanceTimeBy(600_000)
        runCurrent()

        assertEquals(emptyList(), started)
    }

    @Test
    fun syncsAtStart() = runTest {
        val runner = runner()
        runner.start()
        runCurrent()

        assertEquals(listOf(0L), started)
        assertEquals(0L, runner.status.value.lastDoneMillis)
        assertFalse(runner.status.value.running)
    }

    @Test
    fun localWriteSyncsAfterQuietPeriod() = runTest {
        val writes = MutableSharedFlow<Unit>()
        runner(writes = writes).start()
        runCurrent()

        writes.emit(Unit)
        advanceTimeBy(1_000)
        writes.emit(Unit)
        advanceTimeBy(9_000)
        runCurrent()

        assertEquals(listOf(0L, 4_000L), started)
    }

    @Test
    fun nothingPendingNoSync() = runTest {
        val writes = MutableSharedFlow<Unit>()
        runner(writes = writes, pending = 0).start()
        runCurrent()

        writes.emit(Unit)
        advanceTimeBy(10_000)
        runCurrent()

        assertEquals(listOf(0L), started, "only the start sync")
    }

    @Test
    fun syncsEveryFiveMinutes() = runTest {
        runner().start()
        advanceTimeBy(600_000)
        runCurrent()

        assertEquals(listOf(0L, 300_000L, 600_000L), started)
    }

    @Test
    fun failureIsReportedNotThrown() = runTest {
        var calls = 0
        val runner = runner(sync = { if (calls++ == 0) throw IOException("the folder is not there") else 1 })
        runner.start()
        runCurrent()

        assertEquals("the folder is not there", runner.status.value.failure)
        assertFalse(runner.status.value.running)
        assertNull(runner.status.value.lastDoneMillis)

        advanceTimeBy(300_000)
        runCurrent()
        assertEquals(2, calls)
        assertNull(runner.status.value.failure, "the next round cleared it")
        assertEquals(300_000L, runner.status.value.lastDoneMillis)
    }

    @Test
    fun neverTwoAtOnce() = runTest {
        var running = 0
        var most = 0
        var calls = 0
        val writes = MutableSharedFlow<Unit>()
        val runner = runner(writes = writes, sync = {
            calls++
            running++
            most = maxOf(most, running)
            delay(10_000)
            running--
            1
        })
        runner.start()
        runCurrent()
        runner.syncNow()
        runner.syncNow()
        writes.emit(Unit)
        advanceTimeBy(60_000)
        runCurrent()

        assertEquals(1, most)
        assertEquals(2, calls, "the start, then one round for the two asked for during it and the write")
        assertFalse(runner.status.value.running)
    }
}
