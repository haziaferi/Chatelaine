package com.hazia.chatelaine.data.sync

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.hazia.chatelaine.core.sync.HybridClock
import com.hazia.chatelaine.core.sync.Merger
import com.hazia.chatelaine.core.sync.Stamp
import com.hazia.chatelaine.data.ChatelaineDatabase
import com.hazia.chatelaine.data.isConstraintViolation

/**
 * The one way record lines enter the database, whether they were read from a peer's file ([FolderSync]) or from a
 * backup (`Backup.restore`): each line is opened with [seal], one stamped too far ahead is held ([LineGuard]), the
 * rest are put in parent order and merged through [Merger.import] in transactions, a line whose parent has not
 * arrived waits in the database and is retried by [retryWaiting], and one whose parent or owner was purged is dropped.
 * [finish] then runs the repairs that follow every import.
 *
 * It holds no lock: the caller serialises its imports.
 */
internal class RecordImport(
    private val db: ChatelaineDatabase,
    private val tables: Map<String, RowTable>,
    private val clock: HybridClock,
    private val merger: Merger,
    private val seal: LineSeal,
    /** This device's wall clock, in epoch milliseconds: what [LineGuard] measures a stamp against. */
    private val wallMillis: () -> Long,
) {
    private val dao = db.syncDao()

    /** Set when a merge took back a purge (a row edited after it); the caller resets it before an import. */
    var undidPurge = false

    /**
     * Merges [lines] in transactions of at most [MERGE_LINES], each one also saving the clock and,
     * for a file read by position, how far into [path] it got. A crash between two only means the
     * rest is read again, and the merge takes a version it already has without change.
     *
     * Sync gives no order between files, so a line can name a parent that is not here yet: it
     * waits for it ([retryWaiting]). One whose parent or owner was purged will never apply, and is dropped.
     *
     * Each line is opened with [seal] first; one that does not open is skipped. A record stamped too far
     * ahead ([LineGuard]) is held: not applied, and the read position of [path] stays just before the
     * first held line, [from] being where this read started. The next import reads it again, with
     * the lines after it, which the merge takes without change.
     */
    suspend fun merge(lines: List<Line>, path: String?, from: Int): ImportReport {
        var report = ImportReport(0, 0)
        val wall = wallMillis()
        var lineStart = from
        var heldAt: Int? = null
        for (chunk in lines.chunked(MERGE_LINES)) {
            val readable = mutableListOf<Pair<String, Record>>()
            var heldLines = 0
            for (line in chunk) {
                val text = seal.open(line.text)?.decodeToString()
                val record = text?.let(::readable)
                val hold = record != null && LineGuard.isAhead(record, wall)
                if (hold) {
                    heldLines++
                    if (heldAt == null) heldAt = lineStart
                } else if (text != null && record != null) {
                    readable += text to record
                }
                lineStart = line.end
            }
            val position = path?.let { ReadEntity(it, (heldAt ?: chunk.last().end).toLong()) }
            val sorted = sortByParents(readable)
            var dropped = sorted.dropped.size
            val changed = try {
                apply(sorted.ready.map { it.second }, position, waiting = sorted.waiting)
            } catch (e: Exception) {
                if (!isConstraintViolation(e)) throw e
                // A backstop: some line was refused although its parents seemed here. Apply the
                // lines one by one; a refused one either lost its parent in this chunk, and waits,
                // or breaks its table's rules, and is dropped.
                var applied = 0
                val refused = mutableListOf<Pair<String, Record>>()
                for (line in sorted.ready) applyOne(line.second)?.let { applied += it } ?: run { refused += line }
                val again = sortByParents(refused)
                dropped += again.dropped.size + again.ready.size
                applied + apply(emptyList(), position, waiting = sorted.waiting + again.waiting)
            }
            report += ImportReport(changed, chunk.size - readable.size - heldLines + dropped, heldLines)
        }
        return report
    }

    /** Lines whose parents are all here or among [lines], lines still waiting for one, and lines whose parent or owner was purged. */
    private class ByParents(val ready: List<Pair<String, Record>>, val waiting: List<String>, val dropped: List<String>)

    private suspend fun sortByParents(lines: List<Pair<String, Record>>): ByParents {
        val parentsOf = lines.associate { (text, r) -> text to (if (r is RowRecord) tables.getValue(r.row.table).parents(r.row) else emptyList()) }
        val inBatch = lines.mapNotNull { (it.second as? RowRecord)?.row?.id }.toSet()
        val wanted = parentsOf.values.flatten().filter { it.second !in inBatch }.distinct()
        val here = wanted.groupBy({ it.first }, { it.second })
            .flatMap { (t, ids) -> ids.chunked(StagedStore.CHUNK).flatMap { tables.getValue(t).load(it) } }
            .map { it.id }.toSet()
        val missing = wanted.map { it.second }.filter { it !in here }
        // An owner has no foreign key, so a row that names a purged one is dropped, and one that names an id not here
        // (yet) is applied: the id reads as none until it arrives.
        val ownersOf = lines.associate { (text, r) -> text to (if (r is RowRecord) tables.getValue(r.row.table).owners(r.row) else emptyList()) }
        // Only purges already in the database count: one in this same batch may still be taken back by a newer edit of its
        // row. A child of an owner purged in this batch is applied, and the sweep after the import removes it.
        val purged = (missing + ownersOf.values.flatten()).distinct().chunked(StagedStore.CHUNK).flatMap { dao.purges(it) }
            .map { it.id }.toSet()
        val ready = mutableListOf<Pair<String, Record>>()
        val waiting = mutableListOf<String>()
        val dropped = mutableListOf<String>()
        for (line in lines) {
            val absent = parentsOf.getValue(line.first).map { it.second }.filter { it !in inBatch && it !in here }
            when {
                ownersOf.getValue(line.first).any { it in purged } || absent.any { it in purged } -> dropped += line.first
                absent.isEmpty() -> ready += line
                else -> waiting += line.first
            }
        }
        return ByParents(ready, waiting, dropped)
    }

    /** One transaction: merges [records], then saves [position], the [waiting] lines and the clock. */
    private suspend fun apply(records: List<Record>, position: ReadEntity?, waiting: List<String> = emptyList()): Int {
        val rows = records.filterIsInstance<RowRecord>().map { it.row }
        val purges = mutableMapOf<String, Stamp>()
        for (p in records.filterIsInstance<PurgeRecord>()) purges[p.id] = minOf(p.stamp, purges[p.id] ?: p.stamp)
        return db.useWriterConnection { connection ->
            connection.immediateTransaction {
                val store = StagedStore.load(dao, tables, rows.groupBy({ it.table }, { it.id }), purges.keys)
                merger.import(store, rows, purges)
                store.flush(dao, tables)
                position?.let { dao.putRead(it) }
                dao.wait(waiting.map { WaitingEntity(line = it) })
                dao.saveClock(clock)
                if (store.undidPurge) undidPurge = true
                store.changed.size
            }
        }
    }

    /** Merges one [record] in its own transaction; null when a constraint refuses it. */
    private suspend fun applyOne(record: Record): Int? = try {
        apply(listOf(record), position = null)
    } catch (e: Exception) {
        if (!isConstraintViolation(e)) throw e
        null
    }

    /**
     * Applies the waiting lines whose parents have arrived, until a pass applies none. A line that
     * can no longer be read, whose parent was purged, or that a rule refuses, stops waiting.
     */
    suspend fun retryWaiting(): ImportReport {
        var changed = 0
        var dropped = 0
        while (true) {
            val waiting = dao.waiting()
            val parsed = waiting.map { it to readable(it.line) }
            val sorted = sortByParents(parsed.mapNotNull { (w, r) -> r?.let { w.line to it } })
            val gone = parsed.filter { it.second == null }.map { it.first.line } + sorted.dropped
            if (sorted.ready.isEmpty() && gone.isEmpty()) break
            val n = waiting.associate { it.line to it.n }
            for ((line, record) in sorted.ready) {
                applyOne(record)?.let { changed += it } ?: dropped++
                dao.stopWaiting(n.getValue(line))
            }
            gone.forEach { dao.stopWaiting(n.getValue(it)) }
            dropped += gone.size
        }
        return ImportReport(changed, dropped)
    }

    /** A record this version can apply, in its groups ([RowTable.upgrade]), or null: unreadable, not a row of any table it has, or a row its table's rules refuse. */
    private fun readable(line: String): Record? {
        val record = try {
            RecordCodec.decode(line)
        } catch (_: IllegalArgumentException) {
            return null
        }
        if (record !is RowRecord) return record
        val table = tables[record.row.table] ?: return null
        return RowRecord(table.upgrade(record.row)).takeIf { table.fits(it.row) }
    }

    /** The rules that follow every import: field values and attachments of purged owners go, and parent cycles end. */
    suspend fun finish(writes: com.hazia.chatelaine.data.LocalWrites) {
        sweepPurgedOwners(db)
        repairParentCycles(tables, writes)
    }

    private companion object {
        const val MERGE_LINES = 2_000
    }
}
