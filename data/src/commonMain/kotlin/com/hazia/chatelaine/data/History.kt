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
        sync.lostFor(table, id, group).map {
            val g = RecordCodec.decodeGroup(it.groupJson)
            LostValue(it.id, it.tbl, it.rowId, it.grp, g.values, g.stamp)
        }

    /**
     * Writes [lost] back as a new local edit, so it wins everywhere. The version it replaces is kept in turn, and
     * [lost] stops being listed, in the same transaction. An entry already restored is refused
     * (`IllegalArgumentException`); so is one whose row, or a row its value names, is gone
     * (`IllegalStateException`). A restored parent that makes a cycle of tags or places is not refused here: the
     * walks over those trees survive a cycle and a repair step ends it.
     */
    suspend fun restore(lost: LostValue) {
        require(lost.table in writes.tables) { "no table ${lost.table}" }
        writes.write({
            val entry = sync.lostById(lost.lostId)
            require(entry != null && entry.tbl == lost.table && entry.rowId == lost.id && entry.grp == lost.group) {
                "the earlier version is not in history any more: it was restored already"
            }
            val table = writes.tables.getValue(lost.table)
            val row = table.load(listOf(lost.id)).singleOrNull()
                ?: throw IllegalStateException("cannot restore: the ${lost.table} row ${lost.id} is gone")
            for ((parentTable, parentId) in table.parents(row.set(lost.group, lost.stamp, lost.values))) {
                if (writes.tables.getValue(parentTable).load(listOf(parentId)).isEmpty()) {
                    throw IllegalStateException(
                        "cannot restore: it names the $parentTable row $parentId, which is gone",
                    )
                }
            }
            sync.clearLost(listOf(lost.lostId))
            mapOf(lost.table to listOf(lost.id))
        }) { store, _ ->
            val row = requireNotNull(store.row(lost.id)) { "no ${lost.table} row ${lost.id}" }
            val current = row.groups[lost.group]
            if (current != null && current.values != lost.values) store.lost(row, lost.group, current)
            store.put(row.set(lost.group, writes.clock.tick(), lost.values))
        }
    }
}
