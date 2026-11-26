package com.hazia.chatelaine.data.sync

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.hazia.chatelaine.core.sync.HybridClock
import com.hazia.chatelaine.core.sync.Merger
import com.hazia.chatelaine.core.sync.Stamp
import com.hazia.chatelaine.data.ASK_GROUPS
import com.hazia.chatelaine.data.ChatelaineDatabase
import com.hazia.chatelaine.data.KEEP_LOSER_GROUPS
import com.hazia.chatelaine.data.LocalWrites
import com.hazia.chatelaine.data.isConstraintViolation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * What one import did: rows and purges it changed, and folder lines it skipped: unreadable, for a table this version
 * lacks (read again once the synced tables change), refused by a table's rules (a photo with two owners, a region
 * outside its photo), or naming a purged parent or owner. [waiting] counts lines set aside because they are stamped
 * too far ahead ([LineGuard]). They are read again on the next import, and a conflict copy holding one is kept until
 * then. (A line that names a parent not here yet is held in the database and retried after every import; it is not
 * counted here.)
 */
data class ImportReport(val changed: Int, val skipped: Int, val waiting: Int = 0)

/**
 * Folder sync (R51): each device writes only `devices/<device>/`, log segments it appends to and, every
 * [snapshotEvery] segments, a snapshot of its whole state that replaces them. It reads every other device's files
 * from where it last stopped, and merges then deletes any conflict copy of a file-sync tool ([ConflictCopies]),
 * wherever it is. No database file is ever in the folder (R53).
 *
 * Every line it writes is a [RecordCodec] record passed through [seal], and every line it reads is opened by it
 * first. A record stamped more than a day past [wallMillis] waits ([LineGuard]).
 *
 * After every import it ends the parent cycles that two devices made together ([repairParentCycles]) and runs
 * [afterImport].
 *
 * Import and export are serialised: neither runs while the other does.
 */
internal class FolderSync(
    private val db: ChatelaineDatabase,
    private val folder: SyncFolder,
    private val device: String,
    private val clock: HybridClock,
    private val tables: Map<String, RowTable>,
    private val seal: LineSeal = PlainLines,
    /** This device's wall clock, in epoch milliseconds: what [LineGuard] measures a stamp against. */
    private val wallMillis: () -> Long,
    private val segmentBytes: Int = 16 * 1024,
    private val snapshotEvery: Int = 64,
    /**
     * Runs after every import and the cycle repair, inside the import's lock: rules that follow from what peers
     * wrote. It runs whether or not this import changed anything, so a step a crash cut short is done next time.
     */
    private val afterImport: suspend () -> Unit = {},
    /**
     * The database was just made anew after a corrupt one was set aside (`DatabaseOpen.recovered`):
     * the first import reads this device's own files too, so what it wrote comes
     * back from the folder even with no peer.
     */
    recovered: Boolean = false,
) {
    private var readOwn = recovered
    private var undidPurge = false
    private val dao = db.syncDao()
    private val merger = Merger(clock, ASK_GROUPS, KEEP_LOSER_GROUPS)
    private val writes = LocalWrites(db, clock)
    private val lock = Mutex()

    /**
     * Reads what peers wrote since the last import. When it takes back a purge (a row edited after
     * it), the rows under that row, which the purge took here through the foreign keys, are
     * in files already read: every file, this device's own too, is read again from its start, which
     * the merge takes without change for what it already holds.
     */
    suspend fun import(): ImportReport = lock.withLock {
        undidPurge = false
        var report = readFiles()
        if (undidPurge) {
            dao.clearReads()
            readOwn = true
            // The second pass reads every line again: what it skipped or set aside is the import's
            // count, not an addition to the first pass's.
            val again = readFiles()
            report = ImportReport(report.changed + again.changed, again.skipped, again.waiting)
        }
        (report + retryWaiting()).also {
            sweepPurgedOwners(db)
            repairParentCycles(tables, writes)
            afterImport()
        }
    }

    private suspend fun readFiles(): ImportReport {
        forgetReadsIfTablesChanged()
        val held = dao.reads().associate { it.path to it.readBytes }
        val present = mutableListOf<String>()
        var report = ImportReport(0, 0)
        var ownFiles = 0
        var ownMissed = false
        for (owner in folder.list(FolderLayout.DEVICES)) {
            val dir = FolderLayout.dir(owner)
            val names = folder.list(dir)
            // A clash in this device's own files means another device writes under the same id: the
            // winning side of the clash is then not this device's writing, so read all of it.
            val shared = owner == device && names.any(ConflictCopies::isCopy)
            for (name in names) {
                val path = "$dir/$name"
                val copy = ConflictCopies.isCopy(name)
                val data = FolderLayout.segmentSeq(name) != null || FolderLayout.snapshotSeq(name) != null
                if (owner == device && data) ownFiles++
                if (copy || (data && shared)) {
                    val merged = merge(completeLines(folder.read(path) ?: run { if (owner == device) ownMissed = true; null } ?: continue, 0), path = null, from = 0)
                    report += merged
                    // A copy holding a line set aside (too far ahead) is kept, and read whole again next
                    // time, until that line applies.
                    if (copy && merged.waiting == 0) folder.delete(path)
                } else if (data && (owner != device || readOwn)) {
                    present += path
                    val from = held[path] ?: 0
                    // Unchanged since the last read: a peer's snapshot is read once, not on every import.
                    if (folder.size(path) == from) continue
                    val bytes = folder.read(path) ?: run { if (owner == device) ownMissed = true; null } ?: continue
                    // Shorter than what was read: rewritten under us (a torn SAF write); read it again.
                    val start = if (bytes.size < from) 0 else from.toInt()
                    report += merge(completeLines(bytes, start), path, start)
                }
            }
        }
        dao.keepReads(present)
        // The own log counts as read back only when some of it was there and every file of it opened: an empty or
        // partial listing (not arrived yet, a storage hiccup) keeps the device reading it, and compaction waiting.
        if (ownFiles > 0 && !ownMissed) readOwn = false
        return report
    }

    /** True while a database made anew has not yet read its own log back ([recovered]); compaction waits meanwhile. */
    val readingOwnLog: Boolean get() = readOwn

    private suspend fun forgetReadsIfTablesChanged() {
        val names = tables.keys.sorted().joinToString(",")
        if (dao.knownTables() == names) return
        dao.clearReads()
        dao.saveKnownTables(KnownTablesEntity(names = names))
    }

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
    private suspend fun merge(lines: List<Line>, path: String?, from: Int): ImportReport {
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
    private suspend fun retryWaiting(): ImportReport {
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

    /** Appends every row and purge in the outbox to this device's log, then compacts it if due. */
    suspend fun export(): Unit = lock.withLock {
        val pending = dao.outbox()
        if (pending.isEmpty()) return
        val dir = FolderLayout.dir(device)
        val names = folder.list(dir)
        names.filter { it.startsWith(TEMP_PREFIX) }.forEach { folder.delete("$dir/$it") }
        val segments = names.mapNotNull(FolderLayout::segmentSeq).sorted()
        val snapshots = names.mapNotNull(FolderLayout::snapshotSeq)

        val first = segments.lastOrNull() ?: snapshots.maxOrNull()?.plus(1) ?: 0
        var seq = first
        val segment = mutableListOf(folder.read(FolderLayout.segment(device, seq)) ?: ByteArray(0))
        var size = segment.single().size
        for (line in encode(pending)) {
            if (size > 0 && size + line.size > segmentBytes) {
                folder.replace(FolderLayout.segment(device, seq++), concat(segment))
                segment.clear()
                size = 0
            }
            segment += line
            size += line.size
        }
        folder.replace(FolderLayout.segment(device, seq), concat(segment))
        // Only once the lines are in the folder: a crash before here exports them again, harmlessly.
        dao.clearOutbox(upTo = pending.maxOf { it.n })

        val held = segments.toSet() + (first..seq)
        // A database made anew has not read its own files back yet ([readOwn]): a snapshot of it now would
        // replace the segments that hold its history, so compaction waits for the first import.
        if (held.size >= snapshotEvery && !readOwn) {
            folder.replace(FolderLayout.snapshot(device, seq), snapshotOfEverything())
            held.forEach { folder.delete(FolderLayout.segment(device, it)) }
            // Not [seq] itself: after a crash mid-compaction the new snapshot can take the old one's name.
            snapshots.filter { it != seq }.forEach { folder.delete(FolderLayout.snapshot(device, it)) }
        }
    }

    /** One line per id: its row if a table holds it, else its purge. An id can be queued with and without its table. */
    private suspend fun encode(pending: List<OutboxEntity>): List<ByteArray> {
        val ids = pending.map { it.id }.distinct()
        val purges = ids.chunked(StagedStore.CHUNK).flatMap { dao.purges(it) }.associate { it.id to it.stamp() }
        val rows = pending.mapNotNull { e -> e.table?.takeIf { it in tables }?.let { it to e.id } }
            .groupBy({ it.first }, { it.second })
            .flatMap { (t, tableIds) -> tableIds.distinct().chunked(StagedStore.CHUNK).flatMap { tables.getValue(t).load(it) } }
            .associateBy { it.id }
        return ids.mapNotNull { id -> rows[id]?.let { RowRecord(it) } ?: purges[id]?.let { PurgeRecord(id, it) } }.map(::line)
    }

    private suspend fun snapshotOfEverything(): ByteArray {
        val lines = mutableListOf<ByteArray>()
        tables.values.forEach { t -> t.all().forEach { lines += line(RowRecord(it)) } }
        dao.purges().forEach { lines += line(PurgeRecord(it.id, it.stamp())) }
        return concat(lines)
    }

    /** One folder line: the record sealed, which holds no newline, then a newline. */
    private fun line(record: Record) = (seal.seal(RecordCodec.encode(record).encodeToByteArray()) + "\n").encodeToByteArray()

    private companion object {
        const val MERGE_LINES = 2_000
    }
}

private operator fun ImportReport.plus(other: ImportReport) =
    ImportReport(changed + other.changed, skipped + other.skipped, waiting + other.waiting)

private fun concat(parts: List<ByteArray>): ByteArray {
    val out = ByteArray(parts.sumOf { it.size })
    var at = 0
    for (p in parts) {
        p.copyInto(out, at)
        at += p.size
    }
    return out
}

/** A line of a folder file, and the byte offset just past its newline. */
internal data class Line(val text: String, val end: Int)

/**
 * The newline-terminated lines of [bytes] from [from]. A last line with no newline yet is still
 * being written (or was torn), so it is left for a later read.
 */
internal fun completeLines(bytes: ByteArray, from: Int): List<Line> {
    val lines = mutableListOf<Line>()
    var start = from
    for (i in from until bytes.size) {
        if (bytes[i] != '\n'.code.toByte()) continue
        if (i > start) lines += Line(bytes.decodeToString(start, i), i + 1)
        start = i + 1
    }
    return lines
}

/** Runs [action] once [touches] has been quiet for [quietMillis], so a burst of writes makes one export. */
@OptIn(FlowPreview::class)
internal fun CoroutineScope.exportAfterQuiet(touches: Flow<Unit>, quietMillis: Long, action: suspend () -> Unit): Job =
    launch { touches.debounce(quietMillis).collect { action() } }
