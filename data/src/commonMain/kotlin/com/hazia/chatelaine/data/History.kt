package com.hazia.chatelaine.data

import com.hazia.chatelaine.core.sync.Stamp
import com.hazia.chatelaine.data.sync.RecordCodec

/**
 * A version of one field group that a later one replaced in a merge (R54), and where it stood. [lostId] names the
 * entry in the sync tables that holds it. [values] are the group's fields, as the table's row names them.
 */
data class LostValue(
    val lostId: Long,
    val table: String,
    val id: String,
    val group: String,
    val values: Map<String, Any?>,
    val stamp: Stamp,
)

/** The versions the merge replaced, and the way back to one (R54, R55). */
class History internal constructor(private val db: ChatelaineDatabase, private val writes: LocalWrites) {

    private val sync get() = db.syncDao()

    /** What [group] of the row [id] in [table] held before a later version replaced it, oldest entry first. */
    suspend fun overwritten(table: String, id: String, group: String): List<LostValue> =
        sync.lost().filter { it.tbl == table && it.rowId == id && it.grp == group }.map {
            val g = RecordCodec.decodeGroup(it.groupJson)
            LostValue(it.id, it.tbl, it.rowId, it.grp, g.values, g.stamp)
        }

    /**
     * Writes [lost] back as a new local edit, so it wins everywhere. The version it replaces is kept in turn, and
     * [lost] is no longer listed as overwritten. The row must still be here.
     */
    suspend fun restore(lost: LostValue) {
        require(lost.table in writes.tables) { "no table ${lost.table}" }
        writes.write(mapOf(lost.table to listOf(lost.id))) { store ->
            val row = requireNotNull(store.row(lost.id)) { "no ${lost.table} row ${lost.id}" }
            val current = row.groups[lost.group]
            if (current != null && current.values != lost.values) store.lost(row, lost.group, current)
            store.put(row.set(lost.group, writes.clock.tick(), lost.values))
        }
        sync.clearLost(listOf(lost.lostId))
    }
}
