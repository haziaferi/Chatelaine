package com.hazia.chatelaine.data.app

import com.hazia.chatelaine.data.sync.SyncFolder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Decision 9: a sync this long after the last local change. */
const val SYNC_QUIET_MILLIS = 3_000L

/** Decision 9: a sync this often while the app is open. */
const val SYNC_EVERY_MILLIS = 300_000L

/** Where the sync runner stands: a round [running], when the last one ended well, and why the last one failed (null once a round succeeds). */
data class SyncStatus(val running: Boolean = false, val lastDoneMillis: Long? = null, val failure: String? = null)

/**
 * Runs [sync] with the folder [folder] names (none while it is null): once at [start], [quietMillis] after the last
 * emission of [localWrites] when [pending] finds something to export (an import changes the database too, and must
 * not start another round), every [everyMillis], and on [syncNow]. Never two rounds at once: one asked for while
 * another runs follows it. A round that throws sets [SyncStatus.failure] and nothing else; the next one tries again.
 * Runs in [scope], whose dispatcher should allow blocking file access.
 */
class SyncRunner(
    private val sync: suspend (SyncFolder) -> Int,
    private val folder: () -> SyncFolder?,
    private val localWrites: Flow<Unit>,
    private val pending: suspend () -> Int,
    private val scope: CoroutineScope,
    private val wallMillis: () -> Long,
    private val quietMillis: Long = SYNC_QUIET_MILLIS,
    private val everyMillis: Long = SYNC_EVERY_MILLIS,
) {
    private val state = MutableStateFlow(SyncStatus())
    val status: StateFlow<SyncStatus> = state.asStateFlow()

    private val one = Mutex()
    private var job: Job? = null

    /** Starts the rounds; a runner already started is left as it is. */
    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            launch {
                while (true) {
                    round()
                    delay(everyMillis)
                }
            }
            launch { quiet(localWrites).collect { round(onlyIfPending = true) } }
        }
    }

    /** A round now, or right after the one running. */
    fun syncNow() {
        scope.launch { round() }
    }

    /** Stops the rounds; one running is cancelled. */
    fun stop() {
        job?.cancel()
        job = null
    }

    @OptIn(FlowPreview::class)
    private fun quiet(writes: Flow<Unit>) = writes.debounce(quietMillis)

    private suspend fun round(onlyIfPending: Boolean = false) = one.withLock {
        val target = folder() ?: return@withLock
        try {
            // Asked inside the lock: a round that just ran may have exported what the write queued.
            if (onlyIfPending && pending() == 0) return@withLock
            state.update { it.copy(running = true) }
            sync(target)
            state.value = SyncStatus(lastDoneMillis = wallMillis())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            state.update { it.copy(failure = e.message ?: e.toString()) }
        } finally {
            state.update { it.copy(running = false) }
        }
    }
}
