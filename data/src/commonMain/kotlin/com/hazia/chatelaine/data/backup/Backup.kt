package com.hazia.chatelaine.data.backup

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
import com.hazia.chatelaine.data.sync.ImportReport
import com.hazia.chatelaine.data.sync.PlainLines
import com.hazia.chatelaine.data.sync.PurgeRecord
import com.hazia.chatelaine.data.sync.RecordCodec
import com.hazia.chatelaine.data.sync.RecordImport
import com.hazia.chatelaine.data.sync.RowRecord
import com.hazia.chatelaine.data.sync.StagedStore
import com.hazia.chatelaine.data.sync.SyncFolder
import com.hazia.chatelaine.data.sync.completeLines
import com.hazia.chatelaine.data.sync.plus
import com.hazia.chatelaine.data.sync.repairParentCycles
import com.hazia.chatelaine.data.sync.stamp
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** The zip of a backup as a set of named entries: `records.jsonl`, `purges.jsonl` and `blobs/<hash>`. Platform code. */
interface BackupArchive {
    /** The zip holding [entries]. */
    fun pack(entries: Map<String, ByteArray>): ByteArray

    /** The entries of [zip], by name; empty when [zip] is not a zip file. */
    fun unpack(zip: ByteArray): Map<String, ByteArray>
}

/**
 * Backup, export, restore and roll back (R60, R61).
 *
 * A backup is one zip, `chatelaine-backup-YYYY-MM-DD.zip` (the UTC date of `now`), holding
 * - `records.jsonl`: every row of every synced table as one [RecordCodec] line with all its groups and their sync
 *   stamps, parents before children, the very lines a sync folder holds;
 * - `purges.jsonl`: the purge registry, as [RecordCodec] purge lines;
 * - `blobs/<sha256>`: the photo file of each hash a photo row names, when this device holds it undamaged.
 *
 * The manual export is [write] to a folder the user picks; the daily backup is the same call, then [prune].
 *
 * [restore] merges the zip like a peer's lines ([RecordImport], the path a sync import takes), so what was edited
 * after the backup survives it; [rollBack] is the deliberate way back to the backup's values.
 *
 * Reads for [write] are not one transaction: an edit made while it runs can be in some tables and not others, which
 * a restore tolerates (a child whose parent is missing waits for it). Whole photo files are held in memory, one at a
 * time to read and all together in the zip.
 *
 * @param archive packs the entries into a zip (`ZipBackup`).
 * @param wallMillis this device's wall clock in epoch milliseconds; a restored record stamped more than a day past it
 *   is held back ([com.hazia.chatelaine.data.sync.LineGuard]) and counted in [ImportReport.waiting].
 * @param afterImport runs at the end of every [restore], as `FolderSync`'s step of that name does after an import.
 */
class Backup(
    private val db: ChatelaineDatabase,
    private val blobs: BlobStore,
    private val clock: HybridClock,
    private val archive: BackupArchive,
    private val wallMillis: () -> Long,
    private val afterImport: suspend () -> Unit = {},
) {
    private val dao = db.syncDao()
    private val writes = LocalWrites(db, clock)
    private val tables = writes.tables
    private val importer = RecordImport(db, tables, clock, Merger(clock, ASK_GROUPS, KEEP_LOSER_GROUPS), PlainLines, wallMillis)
    private val lock = Mutex()

    /**
     * Writes the backup of everything held now to [target] as `chatelaine-backup-<date of [now]>.zip` and returns its
     * name. The file takes its name only through [SyncFolder.replace], so a crash leaves the old file of that name
     * or the whole new one. Writing on the same day again replaces the day's file.
     */
    suspend fun write(target: SyncFolder, now: Long): String {
        val entries = LinkedHashMap<String, ByteArray>()
        val rows = StringBuilder()
        for (table in tables.values) for (row in table.all()) rows.append(RecordCodec.encode(RowRecord(row))).append('\n')
        val purges = StringBuilder()
        for (p in dao.purges().sortedBy { it.id }) purges.append(RecordCodec.encode(PurgeRecord(p.id, p.stamp()))).append('\n')
        entries[RECORDS] = rows.toString().encodeToByteArray()
        entries[PURGES] = purges.toString().encodeToByteArray()
        for (hash in db.photoDao().photoHashes().filter { BlobName.isValid(it) }.sorted()) {
            val bytes = blobs.read(hash) ?: continue
            if (sha256Hex(bytes) == hash) entries["$BLOBS$hash"] = bytes
        }
        val name = fileName(now)
        target.replace(name, archive.pack(entries))
        return name
    }

    /** Deletes all but the newest [keep] backups in [target], newest by the date in the name. Other files are left alone. */
    fun prune(target: SyncFolder, keep: Int = 7) {
        require(keep >= 1) { "keep at least one backup" }
        target.list("").filter { NAME.matches(it) }.sortedDescending().drop(keep).forEach { target.delete(it) }
    }

    /** [write], then [prune]: what the daily backup does. */
    suspend fun daily(target: SyncFolder, now: Long, keep: Int = 7): String = write(target, now).also { prune(target, keep) }

    /**
     * Merges the backup in [zip] into this database as a peer's changes: per group the newer stamp wins, and a version
     * that loses is kept in History, so an edit made after the backup survives, here and, through sync, on every
     * device. Rows the backup lacks are kept. A row purged here stays purged unless the backup holds a newer edit of
     * it. Photo files the backup holds and this device lacks are stored (each checked against its hash).
     *
     * @throws IllegalArgumentException when [zip] is not a backup.
     */
    suspend fun restore(zip: ByteArray): ImportReport = lock.withLock {
        val entries = unpack(zip)
        val lines = (entries[PURGES]?.let { completeLines(it, 0) }.orEmpty() + completeLines(entries.getValue(RECORDS), 0))
        importer.undidPurge = false
        var report = importer.merge(lines, path = null, from = 0)
        if (importer.undidPurge) {
            // A purge taken back: rows under it went with it here, and are in the same backup. Read it all again.
            val again = importer.merge(lines, path = null, from = 0)
            report = ImportReport(report.changed + again.changed, again.skipped, again.waiting)
        }
        report = report + importer.retryWaiting()
        importer.finish(writes)
        afterImport()
        for ((name, bytes) in entries) {
            val hash = name.removePrefix(BLOBS).takeIf { name.startsWith(BLOBS) && BlobName.isValid(it) } ?: continue
            if (sha256Hex(bytes) != hash) continue
            if (blobs.read(hash)?.let { sha256Hex(it) } != hash) blobs.write(hash, bytes)
        }
        report
    }

    /**
     * Brings the backup's values back as new local edits: each group whose value in [zip] differs from the one held
     * now is written with a fresh stamp (so it wins on every device), and the value it replaced goes to History, from
     * where it can be restored. One transaction.
     *
     * Not undone by this: rows that exist now and not in the backup are left alone, not deleted; a row purged since
     * the backup is not brought back; a value is skipped when it names a row that is gone, or one whose owner was
     * purged. Photo files are not touched.
     *
     * @throws IllegalArgumentException when [zip] is not a backup.
     */
    suspend fun rollBack(zip: ByteArray): Unit = lock.withLock {
        val backed = rowsOf(unpack(zip))
        var plan = emptyList<RowEdit>()
        writes.write({
            plan = planFor(backed)
            plan.groupBy({ it.table }, { it.id })
        }) { store, _ ->
            for (edit in plan) {
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
    }

    private class RowEdit(val table: String, val id: String, val groups: Map<String, Map<String, Any?>>)

    /** The groups of [backed] that differ from the rows held now, for rows still here, not purged and not left without a parent. */
    private suspend fun planFor(backed: List<Row>): List<RowEdit> {
        val ids = backed.map { it.id }.distinct()
        val purged = ids.chunked(StagedStore.CHUNK).flatMap { dao.purges(it) }.map { it.id }.toSet()
        val now = mutableMapOf<String, Row>()
        for ((name, rows) in backed.groupBy { it.table }) {
            rows.map { it.id }.filter { it !in purged }.chunked(StagedStore.CHUNK)
                .forEach { chunk -> tables.getValue(name).load(chunk).forEach { now[it.id] = it } }
        }
        val edits = mutableListOf<RowEdit>()
        for (row in backed) {
            var held = now[row.id] ?: continue
            val table = tables.getValue(row.table)
            val changed = mutableMapOf<String, Map<String, Any?>>()
            for ((group, theirs) in row.groups) {
                val mine = held.groups[group] ?: continue
                if (mine.values == theirs.values) continue
                val candidate = held.set(group, mine.stamp, theirs.values)
                val needed = table.parents(candidate).map { it.first to it.second }
                val missing = needed.any { (t, id) -> tables.getValue(t).load(listOf(id)).isEmpty() }
                val ownerPurged = table.owners(candidate).let { owners -> owners.isNotEmpty() && dao.purges(owners).isNotEmpty() }
                if (missing || ownerPurged) continue
                held = candidate
                changed[group] = theirs.values
            }
            if (changed.isNotEmpty()) edits += RowEdit(row.table, row.id, changed)
        }
        return edits
    }

    /** The rows of a backup, in this version's groups, those this version can read. */
    private fun rowsOf(entries: Map<String, ByteArray>): List<Row> =
        completeLines(entries.getValue(RECORDS), 0).mapNotNull { line ->
            val record = try {
                RecordCodec.decode(line.text)
            } catch (_: IllegalArgumentException) {
                return@mapNotNull null
            }
            if (record !is RowRecord) return@mapNotNull null
            val table = tables[record.row.table] ?: return@mapNotNull null
            table.upgrade(record.row).takeIf { table.fits(it) }
        }

    private fun unpack(zip: ByteArray): Map<String, ByteArray> {
        val entries = archive.unpack(zip)
        require(RECORDS in entries) { "not a Chatelaine backup: no $RECORDS" }
        return entries
    }

    private fun fileName(now: Long): String =
        "chatelaine-backup-${Instant.fromEpochMilliseconds(now).toLocalDateTime(TimeZone.UTC).date}.zip"

    private companion object {
        const val RECORDS = "records.jsonl"
        const val PURGES = "purges.jsonl"
        const val BLOBS = "blobs/"
        val NAME = Regex("""chatelaine-backup-\d{4}-\d{2}-\d{2}\.zip""")
    }
}
