package com.hazia.chatelaine.data

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.hazia.chatelaine.core.sync.HybridClock
import com.hazia.chatelaine.core.sync.Merger
import com.hazia.chatelaine.core.sync.Row
import com.hazia.chatelaine.core.sync.Stamp
import com.hazia.chatelaine.data.sync.StagedStore
import com.hazia.chatelaine.data.sync.saveClock

/**
 * How every repository writes: one transaction that loads the rows it touches, runs
 * the change through the same staged merge as an import, writes it back, and saves [clock] with it.
 * The triggers then queue the rows for export.
 */
internal class LocalWrites(private val db: ChatelaineDatabase, val clock: HybridClock) {
    val merger = Merger(clock, ASK_GROUPS, KEEP_LOSER_GROUPS)
    private val sync = db.syncDao()
    /** The synced tables, as the merge sees them; a restore checks a backup's rows against them. */
    val tables = db.syncedTables()

    /**
     * Makes a row whose id comes from what it joins ([linkId]), the same on every device. It has no base: two devices
     * that make it first each hold a version the other has not seen, so the peer's first version meets this one as a
     * clash and the earlier of the two goes to history (R54). A purge left by an earlier delete of the same pair ends.
     */
    fun createDerived(store: StagedStore, row: Row) {
        store.removePurge(row.id)
        store.put(row)
    }

    /** Writes the rows [ids] names, table by table, as [block] changes them. */
    suspend fun write(ids: Map<String, List<String>>, block: (StagedStore) -> Unit) = write({ ids }) { store, _ -> block(store) }

    /** Changes [group] of the row [id] in [table], stamped with one clock tick that [changes] can use. */
    suspend fun edit(table: String, id: String, group: String, changes: (Stamp) -> Map<String, Any?>) =
        write(mapOf(table to listOf(id))) { store ->
            val s = clock.tick()
            store.put(requireNotNull(store.row(id)) { "no $table row $id" }.edit(group, s, changes(s)))
        }

    /** As [edit], after [check] inside the transaction: a row that is not live is refused there, not before. */
    suspend fun edit(table: String, id: String, group: String, check: suspend () -> Unit, changes: (Stamp) -> Map<String, Any?>) =
        write({ check(); mapOf(table to listOf(id)) }) { store, _ ->
            val s = clock.tick()
            store.put(requireNotNull(store.row(id)) { "no $table row $id" }.edit(group, s, changes(s)))
        }

    /** As [write], with the ids found by [ids] inside the transaction, so they cannot change before the write. */
    suspend fun write(ids: suspend () -> Map<String, List<String>>, block: (StagedStore, Map<String, List<String>>) -> Unit) {
        db.useWriterConnection { connection ->
            connection.immediateTransaction {
                val targets = ids()
                val store = StagedStore.load(sync, tables, targets)
                block(store, targets)
                store.flush(sync, tables)
                sync.saveClock(clock)
            }
        }
    }
}
