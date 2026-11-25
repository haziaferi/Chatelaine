package com.hazia.chatelaine.data.sync

import com.hazia.chatelaine.core.sync.Row
import com.hazia.chatelaine.data.ChatelaineDatabase
import com.hazia.chatelaine.data.LocalWrites
import com.hazia.chatelaine.data.PARENT
import com.hazia.chatelaine.data.PLACE_TABLE
import com.hazia.chatelaine.data.TAG

/**
 * Ends every cycle of tag parents and of place parents. One device can never make a cycle (it refuses to), but two can,
 * each moving one of two tags under the other while apart. In each cycle the parent edit with the newest stamp is reset
 * to none, as a new local edit, so it syncs. Every device finds the same edit, so they end alike; the value it held is
 * kept in history (R54). A cycle of one row (a row under itself) is a cycle too.
 */
internal suspend fun repairParentCycles(tables: Map<String, RowTable>, writes: LocalWrites) {
    for (table in listOf(TAG, PLACE_TABLE)) {
        val rows = (tables[table]?.all() ?: continue).associateBy { it.id }
        val parents = rows.mapValues { (_, row) -> row.parentId() }
        for (cycle in cyclesOf(parents)) {
            val newest = cycle.maxWith(compareBy({ id -> rows.getValue(id).groups.getValue(PARENT).stamp }, { it }))
            writes.write(mapOf(table to listOf(newest))) { store ->
                val row = store.row(newest) ?: return@write
                // Changed since it was read: the next import looks again.
                if (row.parentId() != parents[newest]) return@write
                store.lost(row, PARENT, row.groups.getValue(PARENT))
                store.put(row.edit(PARENT, writes.clock.tick(), mapOf("parentId" to null)))
            }
        }
    }
}

/**
 * Deletes, on this device, the field values of an item or place that was purged and the attachments to an item or tag
 * that was purged (R152). They name their owner by a plain id, so the purge does not cascade to them; every device
 * does this itself when it applies the purge, so no purge record is needed for them.
 */
internal suspend fun sweepPurgedOwners(db: ChatelaineDatabase) {
    db.fieldValueDao().deleteOfPurgedOwners()
    db.fieldAttachDao().deleteOfPurgedTargets()
}

private fun Row.parentId(): String? = groups[PARENT]?.values?.get("parentId") as String?

/** The cycles of [parents] (id to parent id): a parent that is no id here ends the walk, as it reads as none. */
private fun cyclesOf(parents: Map<String, String?>): List<List<String>> {
    val state = mutableMapOf<String, Int>() // 1 on the walk being made, 2 done
    val cycles = mutableListOf<List<String>>()
    for (start in parents.keys) {
        if (start in state) continue
        val walk = mutableListOf<String>()
        var at: String? = start
        while (at != null && at in parents && at !in state) {
            state[at] = 1
            walk += at
            at = parents[at]
        }
        if (at != null && state[at] == 1) cycles += walk.subList(walk.indexOf(at), walk.size)
        walk.forEach { state[it] = 2 }
    }
    return cycles
}
