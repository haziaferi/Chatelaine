package com.hazia.chatelaine.data.backup

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import com.hazia.chatelaine.core.sync.HybridClock
import com.hazia.chatelaine.core.sync.Merger
import com.hazia.chatelaine.core.sync.Row
import com.hazia.chatelaine.data.ASK_GROUPS
import com.hazia.chatelaine.data.ChatelaineDatabase
import com.hazia.chatelaine.data.KEEP_LOSER_GROUPS
import com.hazia.chatelaine.data.LocalWrites
import com.hazia.chatelaine.data.blob.BlobName
import com.hazia.chatelaine.data.blob.BlobStore
import com.hazia.chatelaine.data.blob.sha256Hex
import com.hazia.chatelaine.data.sync.ImportLock
import com.hazia.chatelaine.data.sync.ImportReport
import com.hazia.chatelaine.data.sync.LineGuard
import com.hazia.chatelaine.data.sync.PlainLines
import com.hazia.chatelaine.data.sync.PurgeRecord
import com.hazia.chatelaine.data.sync.RecordCodec
import com.hazia.chatelaine.data.sync.RecordImport
import com.hazia.chatelaine.data.sync.RowRecord
import com.hazia.chatelaine.data.sync.StagedStore
import com.hazia.chatelaine.data.sync.SyncFolder
import com.hazia.chatelaine.data.sync.TEMP_PREFIX
import com.hazia.chatelaine.data.sync.completeLines
import com.hazia.chatelaine.data.sync.plus
import com.hazia.chatelaine.data.sync.repairParentCycles
import com.hazia.chatelaine.data.sync.stamp
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** The zip of a backup as a set of named entries: `records.jsonl`, `purges.jsonl` and `blobs/<hash>`. Platform code. */
interface BackupArchive {
    /** The zip holding [entries]. */
    fun pack(entries: Map<String, ByteArray>): ByteArray

    /** The entries of [zip], by name; empty when [zip] is not a complete zip file. */
    fun unpack(zip: ByteArray): Map<String, ByteArray>
}

/** What [Backup.rollBack] did: groups written back, and values skipped for a parent that is gone or an owner that was purged. */
data class RollBackReport(val written: Int, val missingParent: Int, val purgedOwner: Int)

/**
 * Backup, export, restore and roll back (R60, R61).
 *
 * A backup is one zip, `chatelaine-backup-YYYY-MM-DD.zip` (the date of `now` on this device's calendar, as the person
 * reads it), holding
 * - `records.jsonl`: every row of every synced table as one [RecordCodec] line with all its groups and their sync
 *   stamps, parents before children, the very lines a sync folder holds;
 * - `purges.jsonl`: the purge registry, as [RecordCodec] purge lines;
 * - `blobs/<sha256>`: the photo file of each hash a photo row names, when this device holds it undamaged (stored,
 *   not deflated).
 *
 * The manual export is [write] to a folder the user picks; the daily backup is [daily]: [write], then [prune].
 *
 * [restore] merges the zip like a peer's lines ([RecordImport], the path a sync import takes), so what was edited
 * after the backup survives it; [rollBack] is the deliberate way back to the backup's values.
 *
 * [write], [restore] and [rollBack] share [importLock] with `FolderSync`, so none runs during an import or export.
 * The whole zip, photo files included, is held in memory (a streaming write is a later change).
 *
 * @param archive packs the entries into a zip (`ZipBackup`).
 * @param wallMillis this device's wall clock in epoch milliseconds, against which [restore] checks stamps.
 * @param afterImport runs at the end of every [restore], as `FolderSync`'s step of that name does after an import.
 * @param readingOwnLog true while the database re-reads its own folder files after a recovery
 *   (`FolderSync.readingOwnLog`): [write] makes no backup then, as it would miss what those files hold.
 * @param importLock the database's one lock for imports; pass the one given to its `FolderSync`.
 */
class Backup(
    private val db: ChatelaineDatabase,
    private val blobs: BlobStore,
    private val clock: HybridClock,
    private val archive: BackupArchive,
    private val wallMillis: () -> Long,
    private val afterImport: suspend () -> Unit = {},
    private val readingOwnLog: () -> Boolean = { false },
    private val importLock: ImportLock = ImportLock(),
) {
    private val dao = db.syncDao()
    private val writes = LocalWrites(db, clock)
    private val tables = writes.tables
    private val importer = RecordImport(db, tables, clock, Merger(clock, ASK_GROUPS, KEEP_LOSER_GROUPS), PlainLines, wallMillis)

    /** A test seam: runs inside [write]'s snapshot after each table is read, with its name. */
    internal var afterTableRead: suspend (String) -> Unit = {}

    /**
     * Writes the backup of everything held now to [target] as `chatelaine-backup-<date of [now]>.zip` and returns its
     * name; null, and nothing written, while the database re-reads its own folder files ([readingOwnLog]). Rows,
     * purges and photo hashes are read in one transaction, so the backup is one moment of the database; the photo
     * files are read after it. The file takes its name only through [SyncFolder.replace], so a crash leaves the old
     * file of that name or the whole new one; temp files an earlier crash left in [target] are deleted first.
     * Writing on the same day again replaces the day's file.
     */
    suspend fun write(target: SyncFolder, now: Long): String? = importLock.mutex.withLock {
        if (readingOwnLog()) return@withLock null
        target.list("").filter { it.startsWith(TEMP_PREFIX + FILE_PREFIX) }.forEach { target.delete(it) }

        val rows = StringBuilder()
        val purges = StringBuilder()
        val hashes = db.useWriterConnection { connection ->
            connection.immediateTransaction {
                for ((name, table) in tables) {
                    for (row in table.all()) rows.append(RecordCodec.encode(RowRecord(row))).append('\n')
                    afterTableRead(name)
                }
                for (p in dao.purges().sortedBy { it.id }) purges.append(RecordCodec.encode(PurgeRecord(p.id, p.stamp()))).append('\n')
                db.photoDao().photoHashes()
            }
        }
        val entries = LinkedHashMap<String, ByteArray>()
        entries[RECORDS] = rows.toString().encodeToByteArray()
        entries[PURGES] = purges.toString().encodeToByteArray()
        for (hash in hashes.filter { BlobName.isValid(it) }.sorted()) {
            val bytes = blobs.read(hash) ?: continue
            if (sha256Hex(bytes) == hash) entries["$BLOBS$hash"] = bytes
        }
        val name = fileName(now)
        target.replace(name, archive.pack(entries))
        name
    }

    /**
     * Deletes all but the newest [keep] backups in [target], newest by the date in the name; [protect], the file just
     * written, is never deleted even when its date sorts oldest (it takes one of the [keep] places). Other files are
     * left alone.
     */
    fun prune(target: SyncFolder, keep: Int = 7, protect: String? = null) {
        require(keep >= 1) { "keep at least one backup" }
        val names = target.list("").filter { NAME.matches(it) }.sortedDescending()
        val others = names.filter { it != protect }
        val room = if (protect != null && protect in names) keep - 1 else keep
        others.drop(room).forEach { target.delete(it) }
    }

    /** [write], then [prune]: what the daily backup does. Null when [write] made none. */
    suspend fun daily(target: SyncFolder, now: Long, keep: Int = 7): String? = write(target, now)?.also { prune(target, keep, protect = it) }

    /**
     * Merges the backup in [zip] into this database as a peer's changes: per group the newer stamp wins, and a version
     * that loses is kept in History, so an edit made after the backup survives, here and, through sync, on every
     * device. Rows the backup lacks are kept. A row purged here stays purged unless the backup holds a newer edit of
     * it. Photo files the backup holds and this device lacks are stored first (each checked against its hash).
     *
     * @throws IllegalArgumentException when [zip] is not a complete backup.
     * @throws IllegalStateException before anything is written, when a record is stamped more than a day past this
     *   device's clock ([LineGuard]): applied, it would win every later edit; fix the clock and restore again.
     */
    suspend fun restore(zip: ByteArray): ImportReport = importLock.mutex.withLock {
        val entries = unpack(zip)
        val lines = completeLines(entries.getValue(PURGES), 0) + completeLines(entries.getValue(RECORDS), 0)
        val wall = wallMillis()
        val ahead = lines.count { line -> importer.readable(line.text)?.let { LineGuard.isAhead(it, wall) } == true }
        check(ahead == 0) {
            "the backup holds $ahead records stamped more than a day ahead of this device's clock; set the clock right and restore again"
        }
        for ((name, bytes) in entries) {
            val hash = name.takeIf { it.startsWith(BLOBS) }?.removePrefix(BLOBS)?.takeIf { BlobName.isValid(it) } ?: continue
            if (sha256Hex(bytes) != hash) continue
            if (blobs.read(hash)?.let { sha256Hex(it) } != hash) blobs.write(hash, bytes)
        }
        importer.undidPurge = false
        var report = importer.merge(lines, path = null, from = 0)
        if (importer.undidPurge) {
            // A purge taken back: rows under it went with it here, and are in the same backup. Read it all again, and
            // peers' files too, as FolderSync does.
            val again = importer.merge(lines, path = null, from = 0)
            report = ImportReport(report.changed + again.changed, again.skipped, again.waiting)
            dao.clearReads()
        }
        report = report + importer.retryWaiting()
        importer.finish(writes)
        afterImport()
        report
    }

    /**
     * Brings the backup's values back as new local edits: each group whose value in [zip] differs from the one held
     * now is written with a fresh stamp (so it wins on every device), and the value it replaced goes to History, from
     * where it can be restored. One transaction.
     *
     * Not undone by this: rows that exist now and not in the backup are left alone, not deleted; a row purged since
     * the backup is not brought back; a value is skipped when it names a row that is gone, or one whose owner was
     * purged (counted in the result). Photo files are not touched.
     *
     * @throws IllegalArgumentException when [zip] is not a complete backup.
     */
    suspend fun rollBack(zip: ByteArray): RollBackReport = importLock.mutex.withLock {
        val backed = rowsOf(unpack(zip))
        var plan = Plan(emptyList(), 0, 0)
        writes.write({
            plan = planFor(backed)
            plan.edits.groupBy({ it.table }, { it.id })
        }) { store, _ ->
            for (edit in plan.edits) {
                val row = requireNotNull(store.row(edit.id)) { "no ${edit.table} row ${edit.id}" }
                var next = row
                for ((group, values) in edit.groups) {
                    row.groups[group]?.let { store.lost(row, group, it) }
                    next = next.set(group, clock.tick(), values)
                }
                store.put(next)
            }
        }
        // A restored parent can close a cycle with one a peer made since: end it as an import does.
        repairParentCycles(tables, writes)
        RollBackReport(plan.edits.sumOf { it.groups.size }, plan.missingParent, plan.purgedOwner)
    }

    private class RowEdit(val table: String, val id: String, val groups: Map<String, Map<String, Any?>>)

    private class Plan(val edits: List<RowEdit>, val missingParent: Int, val purgedOwner: Int)

    /** The groups of [backed] that differ from the rows held now, for rows still here and not purged, less those left without a parent or an owner. */
    private suspend fun planFor(backed: List<Row>): Plan {
        val ids = backed.map { it.id }.distinct()
        val purged = ids.chunked(StagedStore.CHUNK).flatMap { dao.purges(it) }.map { it.id }.toSet()
        val now = mutableMapOf<String, Row>()
        for ((name, rows) in backed.groupBy { it.table }) {
            rows.map { it.id }.filter { it !in purged }.chunked(StagedStore.CHUNK)
                .forEach { chunk -> tables.getValue(name).load(chunk).forEach { now[it.id] = it } }
        }
        val edits = mutableListOf<RowEdit>()
        var missingParent = 0
        var purgedOwner = 0
        for (row in backed) {
            var held = now[row.id] ?: continue
            val table = tables.getValue(row.table)
            val changed = mutableMapOf<String, Map<String, Any?>>()
            for ((group, theirs) in row.groups) {
                val mine = held.groups[group] ?: continue
                if (mine.values == theirs.values) continue
                val candidate = held.set(group, mine.stamp, theirs.values)
                if (table.parents(candidate).any { (t, id) -> tables.getValue(t).load(listOf(id)).isEmpty() }) {
                    missingParent++
                    continue
                }
                val owners = table.owners(candidate)
                if (owners.isNotEmpty() && dao.purges(owners).isNotEmpty()) {
                    purgedOwner++
                    continue
                }
                held = candidate
                changed[group] = theirs.values
            }
            if (changed.isNotEmpty()) edits += RowEdit(row.table, row.id, changed)
        }
        return Plan(edits, missingParent, purgedOwner)
    }

    /** The rows of a backup, in this version's groups, those this version can read (the importer's own reading). */
    private fun rowsOf(entries: Map<String, ByteArray>): List<Row> =
        completeLines(entries.getValue(RECORDS), 0).mapNotNull { importer.readable(it.text) }.filterIsInstance<RowRecord>().map { it.row }

    private fun unpack(zip: ByteArray): Map<String, ByteArray> {
        val entries = archive.unpack(zip)
        require(RECORDS in entries && PURGES in entries) { "not a complete Chatelaine backup: it needs $RECORDS and $PURGES" }
        return entries
    }

    private fun fileName(now: Long): String =
        "$FILE_PREFIX${Instant.fromEpochMilliseconds(now).toLocalDateTime(TimeZone.currentSystemDefault()).date}.zip"

    private companion object {
        const val RECORDS = "records.jsonl"
        const val PURGES = "purges.jsonl"
        const val BLOBS = "blobs/"
        const val FILE_PREFIX = "chatelaine-backup-"
        val NAME = Regex("""chatelaine-backup-\d{4}-\d{2}-\d{2}\.zip""")
    }
}
