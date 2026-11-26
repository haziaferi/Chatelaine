package com.hazia.chatelaine.data.app

import com.hazia.chatelaine.core.sync.HybridClock
import com.hazia.chatelaine.data.ChatelaineDatabase
import com.hazia.chatelaine.data.DEVICE_ID_DIR
import com.hazia.chatelaine.data.DatabaseOpen
import com.hazia.chatelaine.data.Fields
import com.hazia.chatelaine.data.History
import com.hazia.chatelaine.data.Inventory
import com.hazia.chatelaine.data.LocalWrites
import com.hazia.chatelaine.data.UNOPENABLE_SUFFIX
import com.hazia.chatelaine.data.backup.Backup
import com.hazia.chatelaine.data.backup.ZipBackup
import com.hazia.chatelaine.data.blob.BlobStore
import com.hazia.chatelaine.data.blob.BlobSync
import com.hazia.chatelaine.data.blob.DirectoryBlobStore
import com.hazia.chatelaine.data.renewDeviceIdIfRecovered
import com.hazia.chatelaine.data.sync.FolderSync
import com.hazia.chatelaine.data.sync.ImportLock
import com.hazia.chatelaine.data.sync.ImportReport
import com.hazia.chatelaine.data.sync.PlainLines
import com.hazia.chatelaine.data.sync.SyncFolder
import com.hazia.chatelaine.data.sync.loadClock
import com.hazia.chatelaine.data.sync.markRecovered
import com.hazia.chatelaine.data.syncedTables
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * The unopenable database files kept beside [dbFile] (R50): the names in its folder that start with
 * `<name>.unopenable`, sorted. What the recovery notice and the device id renewal key on.
 */
fun keptFilesBeside(dbFile: File): List<String> {
    val prefix = "${dbFile.name}$UNOPENABLE_SUFFIX"
    return dbFile.absoluteFile.parentFile?.list()?.filter { it.startsWith(prefix) }?.sorted().orEmpty()
}

/**
 * The app's data on this device: the database [dbFile], its device id in `identity/` and its photo files in `photos/`
 * under [filesDir], one clock resumed from the database, and one [ImportLock] shared by the folder sync and the
 * backup, so an import and a restore never overlap. The app opens it once per process, through [shared].
 */
class LocalAppData private constructor(
    private val db: ChatelaineDatabase,
    private val dbFile: File,
    override val deviceId: String,
    private val clock: HybridClock,
    private val photos: BlobStore,
    private val wallMillis: () -> Long,
) : AppData, AutoCloseable {
    private val writes = LocalWrites(db, clock)
    override val inventory = Inventory(db, writes)
    override val fields = Fields(db, writes)
    override val history = History(db, writes)
    private val lock = ImportLock()
    private val backups = Backup(db, photos, clock, ZipBackup, wallMillis, importLock = lock)
    private var current: Pair<SyncFolder, FolderSync>? = null

    override fun changes(vararg tables: String): Flow<Unit> = db.invalidationTracker.createFlow(*tables).map { }

    override suspend fun sync(folder: SyncFolder): Int {
        // One FolderSync per folder; what it read, and whether the own log is still to be read back, is in the database.
        val s = current?.takeIf { it.first === folder }?.second ?: FolderSync(
            db, folder, deviceId, clock, db.syncedTables(), PlainLines, wallMillis, importLock = lock,
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

    override suspend fun pendingExport(): Int = db.syncDao().outbox().size

    override suspend fun backup(folder: SyncFolder, now: Long): String? = backups.daily(folder, now)

    override suspend fun restore(zip: ByteArray): ImportReport = backups.restore(zip)

    override suspend fun recovery(): RecoveryState {
        val ownLog = db.syncDao().ownLog()
        return RecoveryState(
            keptFiles = keptFilesBeside(dbFile),
            waitingForOwnLog = ownLog?.recovered == true,
            backupsHeld = ownLog?.backupsHeld == true,
        )
    }

    override fun close() = db.close()

    companion object {
        private const val MAX_ROUNDS = 10
        private val sharedLock = Any()
        private var sharedOne: Pair<File, LocalAppData>? = null

        /**
         * Opens the database through [open] (blocking: call it off the main thread), then takes the device id,
         * renewed when a kept unopenable file beside [dbFile] has no renewal yet (`renewDeviceIdIfRecovered`), and
         * only then resumes the clock under that id.
         */
        fun open(
            dbFile: File,
            filesDir: File,
            open: () -> DatabaseOpen<ChatelaineDatabase>,
            wallMillis: () -> Long = System::currentTimeMillis,
        ): LocalAppData {
            val db = open().database
            try {
                val (deviceId, clock) = runBlocking {
                    val id = renewDeviceIdIfRecovered(
                        File(filesDir, DEVICE_ID_DIR), keptFilesBeside(dbFile), markRecovered = { db.syncDao().markRecovered(it) },
                    )
                    // Resumed from the database: a fresh clock would stamp the next edit below a version already seen
                    // from a peer whose clock runs ahead, and that edit would lose everywhere but here.
                    id to db.syncDao().loadClock(id, wallMillis)
                }
                return LocalAppData(db, dbFile, deviceId, clock, DirectoryBlobStore(File(filesDir, "photos").toPath()), wallMillis)
            } catch (e: Throwable) {
                db.close()
                throw e
            }
        }

        /**
         * The one [LocalAppData] of this process: opened by the first call, the same instance for every later call
         * with the same [dbFile]. Another file is refused (`IllegalStateException`): two openers of one database in
         * one process would each hold a clock and an import lock of their own.
         */
        fun shared(dbFile: File, filesDir: File, open: () -> DatabaseOpen<ChatelaineDatabase>): LocalAppData = synchronized(sharedLock) {
            val key = dbFile.absoluteFile
            sharedOne?.let { (file, app) ->
                check(file == key) { "this process already opened ${file.path}; it cannot open ${key.path} too" }
                return app
            }
            LocalAppData.open(dbFile, filesDir, open).also { sharedOne = key to it }
        }
    }
}
