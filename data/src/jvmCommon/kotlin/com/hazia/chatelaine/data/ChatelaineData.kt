package com.hazia.chatelaine.data

import com.hazia.chatelaine.core.sync.HybridClock
import com.hazia.chatelaine.data.backup.Backup
import com.hazia.chatelaine.data.backup.ZipBackup
import com.hazia.chatelaine.data.blob.BlobSync
import com.hazia.chatelaine.data.blob.BlobStore
import com.hazia.chatelaine.data.sync.FolderSync
import com.hazia.chatelaine.data.sync.ImportLock
import com.hazia.chatelaine.data.sync.PlainLines
import com.hazia.chatelaine.data.sync.SyncFolder

/** An item as the debug harnesses list it: its names that a later edit overwrote ([lost]) are what History holds. */
data class HarnessItem(val id: String, val name: String, val lost: List<String>)

/**
 * The wiring of one device's data layer (database, clock, folder sync, photo files, backup) behind the five actions
 * of the debug harnesses of stage 1. PROVISIONAL: it exists so the harnesses, which live in other modules, can reach
 * the internal `FolderSync` and `BlobSync`; stage 2 replaces it with the app's real composition.
 */
class ChatelaineData(
    private val open: DatabaseOpen<ChatelaineDatabase>,
    val deviceId: String,
    private val photos: BlobStore,
    private val wallMillis: () -> Long = System::currentTimeMillis,
) : AutoCloseable {
    private val db = open.database
    private val clock = HybridClock(deviceId, wallMillis)
    private val writes = LocalWrites(db, clock)
    private val inventory = Inventory(db, writes)
    private val history = History(db, writes)
    private val lock = ImportLock()
    private var current: Pair<SyncFolder, FolderSync>? = null
    private val backup = Backup(
        db, photos, clock, ZipBackup, wallMillis, readingOwnLog = { current?.second?.readingOwnLog == true }, importLock = lock,
    )

    suspend fun add(name: String): String = inventory.addItem(name)

    suspend fun edit(id: String, name: String) = inventory.rename(id, name)

    suspend fun list(): List<HarnessItem> =
        inventory.items().sortedBy { it.name }.map { item ->
            HarnessItem(item.id, item.name, history.overwritten(ITEM, item.id, NAME).mapNotNull { it.values["name"] as? String })
        }

    /** Exports, imports and exchanges photo files with [folder], until an import changes nothing; returns the rounds. */
    suspend fun sync(folder: SyncFolder): Int {
        // One FolderSync per folder: it remembers what it read and whether it still re-reads its own log.
        val s = current?.takeIf { it.first === folder }?.second ?: FolderSync(
            db, folder, deviceId, clock, db.syncedTables(), PlainLines, wallMillis, recovered = open.recovered, importLock = lock,
        ).also { current = folder to it }
        var rounds = 0
        do {
            rounds++
            s.export()
            val changed = s.import().changed
            BlobSync(db, folder, photos, wallMillis).exchange()
        } while (changed > 0 && rounds < MAX_ROUNDS)
        return rounds
    }

    /** Today's backup in [folder], the last seven kept; its name, or null while the database re-reads its own log. */
    suspend fun backup(folder: SyncFolder): String? = backup.daily(folder, wallMillis())

    override fun close() = db.close()

    private companion object {
        const val MAX_ROUNDS = 10
    }
}
