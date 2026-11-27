package com.hazia.chatelaine.data.app

import com.hazia.chatelaine.data.sync.SyncFolder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Decision 9: a sync this long after the last local change. */
const val SYNC_QUIET_MILLIS = 3_000L

/** Decision 9: a sync this often while the app is open. */
const val SYNC_EVERY_MILLIS = 300_000L

/** Where the sync runner stands: a round [running], when the last one ended well, and why the last one failed (null once a round succeeds). */
data class SyncStatus(val running: Boolean = false, val lastDoneMillis: Long? = null, val failure: String? = null)

/**
 * Runs [sync] with the folder [folder] names (none while it is null): once at [start], [quietMillis] after the last
 * emission of [localWrites] when [pending] finds something to export (an import changes the database too, and must
 * not start another round), every [everyMillis], and on [syncNow]. One coroutine runs the rounds, so never two at
 * once; every request made while a round runs, however many, makes one round after it. A round that throws sets
 * [SyncStatus.failure] and nothing else; the next one tries again. Runs in [scope], whose dispatcher should allow
 * blocking file access.
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

    /** The strongest request not yet taken: [NONE], [IF_PENDING] (after a local write) or [ALWAYS]. */
    private val asked = MutableStateFlow(NONE)
    private var job: Job? = null

    /** Starts the rounds; a runner already started is left as it is. */
    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            launch {
                while (true) {
                    asked.first { it != NONE }
                    round(onlyIfPending = asked.getAndUpdate { NONE } == IF_PENDING)
                }
            }
            launch {
                while (true) {
                    ask(ALWAYS)
                    delay(everyMillis)
                }
            }
            launch { watchWrites() }
        }
    }

    /**
     * Asks for a round after each quiet period of [localWrites]. A failing flow is subscribed again after
     * [quietMillis] (the pause keeps one that fails at once from spinning); it ends no other round.
     */
    private suspend fun watchWrites() {
        while (true) {
            try {
                quiet(localWrites).collect { ask(IF_PENDING) }
                return
            } catch (e: CancellationException) {
                currentCoroutineContext().ensureActive()
            } catch (_: Exception) {
            }
            delay(quietMillis)
        }
    }

    /** A round now, or right after the one running. */
    fun syncNow() = ask(ALWAYS)

    /** Stops the rounds; one running is cancelled. */
    fun stop() {
        job?.cancel()
        job = null
    }

    private fun ask(level: Int) = asked.update { maxOf(it, level) }

    @OptIn(FlowPreview::class)
    private fun quiet(writes: Flow<Unit>) = writes.debounce(quietMillis)

    private suspend fun round(onlyIfPending: Boolean) {
        try {
            val target = folder() ?: return
            // Asked when the round starts: a round that just ran may have exported what the write queued.
            if (onlyIfPending && pending() == 0) return
            state.update { it.copy(running = true) }
            sync(target)
            state.value = SyncStatus(lastDoneMillis = wallMillis())
        } catch (e: CancellationException) {
            // The runner stopped: the cancellation goes on. One the sync threw while the runner runs is a failure.
            currentCoroutineContext().ensureActive()
            failed(e)
        } catch (e: Exception) {
            failed(e)
        } finally {
            state.update { it.copy(running = false) }
        }
    }

    private fun failed(e: Exception) = state.update { it.copy(failure = e.message ?: e.toString()) }

    private companion object {
        const val NONE = 0
        const val IF_PENDING = 1
        const val ALWAYS = 2
    }
}
