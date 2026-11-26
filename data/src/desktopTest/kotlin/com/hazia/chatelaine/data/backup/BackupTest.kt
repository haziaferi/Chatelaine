package com.hazia.chatelaine.data.backup

import com.hazia.chatelaine.data.FieldInput
import com.hazia.chatelaine.data.FieldType
import com.hazia.chatelaine.data.ITEM
import com.hazia.chatelaine.data.NAME
import com.hazia.chatelaine.data.PHOTO_REGION
import com.hazia.chatelaine.data.TARGET
import com.hazia.chatelaine.data.SYNCED_TABLES
import com.hazia.chatelaine.data.blob.Blobs
import com.hazia.chatelaine.data.blob.MemoryBlobStore
import com.hazia.chatelaine.data.sync.DirectorySyncFolder
import com.hazia.chatelaine.data.sync.SyncFolder
import com.hazia.chatelaine.data.sync.World
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import java.util.zip.ZipEntry
import java.nio.file.Files
import java.util.zip.ZipInputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackupTest {

    private val dir = Files.createTempDirectory("chatelaine-backup").toFile().also { it.deleteOnExit() }
    private val world = World(dir, seed = 1)
    private val targetDir = Files.createTempDirectory(dir.toPath(), "target")
    private val target: SyncFolder = DirectorySyncFolder(targetDir)
    private val day = 86_400_000L

    @AfterTest
    fun close() = world.close()

    private fun Phone(name: String) = Phone(world, name)

    private fun <T> run(block: suspend () -> T): T = runBlocking { block() }

    /** R60, stage 1 gate: 20 items, 5 places, 6 tags, 3 photos and fields, exported and imported into an empty database. */
    @Test
    fun exportThenImportGivesTheSameInventory() = assertExportImportRoundTrip(world, target)

    /** R61: edits made after the backup survive a restore, on both devices. */
    @Test
    fun restoreKeepsEditsNewerThanTheBackup() {
        val a = Phone("A")
        val b = Phone("B")
        val x = run { a.device.inventory.addItem("x old") }
        val y = run { a.device.inventory.addItem("y old") }
        world.settle(listOf(a.device, b.device))
        val name = run { a.backup.write(target, world.syncthing.now) }!!

        world.syncthing.now += 1_000
        run { a.device.inventory.rename(x, "x on A") }
        world.syncthing.now += 1_000
        run { b.device.inventory.rename(y, "y on B") }
        // The older state comes back on A, and sync goes on: nothing newer is overwritten.
        run { a.backup.restore(target.read(name)!!) }
        world.settle(listOf(a.device, b.device))

        for (d in listOf(a.device, b.device)) {
            assertEquals("x on A", run { d.inventory.getItem(x)?.name }, d.name)
            assertEquals("y on B", run { d.inventory.getItem(y)?.name }, d.name)
        }
    }

    /** R61: a roll back is a change; what it replaced is in History and can be restored again. */
    @Test
    fun rollBackIsAChangeAndCanBeUndone() {
        val a = Phone("A")
        val item = run { a.device.inventory.addItem("clip") }
        val gone = run { a.device.inventory.addItem("short lived") }
        val name = run { a.backup.write(target, world.syncthing.now) }!!

        world.syncthing.now += 1_000
        run { a.device.inventory.rename(item, "hair clip") }
        val editStamp = a.device.rows(ITEM).getValue(item).groups.getValue(NAME).stamp
        run { a.device.inventory.deleteItem(gone) }
        val newer = run { a.device.inventory.addItem("made after the backup") }
        val report = run { a.backup.rollBack(target.read(name)!!) }

        assertEquals(1, report.written)
        assertTrue(a.device.rows(ITEM).getValue(item).groups.getValue(NAME).stamp > editStamp, "the roll back is a newer edit")
        assertEquals("clip", run { a.device.inventory.getItem(item)?.name })
        assertNull(run { a.device.inventory.getItem(gone) }, "a row purged since the backup is not brought back")
        assertEquals("made after the backup", run { a.device.inventory.getItem(newer)?.name }, "a row the backup lacks is left alone")
        val lost = run { a.device.history.overwritten(ITEM, item, NAME) }.single { it.values["name"] == "hair clip" }
        assertNotNull(lost)

        run { a.device.history.restore(lost) }
        assertEquals("hair clip", run { a.device.inventory.getItem(item)?.name })
    }

    /** R60: the eighth daily backup leaves seven. */
    @Test
    fun sevenBackupsAreKept() {
        val a = Phone("A")
        val names = (0 until 8).map { d -> run { a.backup.write(target, 20_000L * day + d * day) }!!.also { a.backup.prune(target) } }
        val left = target.list("").filter { it.startsWith("chatelaine-backup-") }.sorted()
        assertEquals(names.drop(1), left)
        assertFalse(names.first() in left)
    }

    @Test
    fun backupIsDueAfter24Hours() {
        val t = 1_000_000L
        val hour = 3_600_000L
        assertTrue(BackupSchedule.due(null, t))
        assertFalse(BackupSchedule.due(t, t + 23 * hour))
        assertTrue(BackupSchedule.due(t, t + 24 * hour))
    }

    /** The zip holds the three kinds of entry R60 names, and reaches its name only through [SyncFolder.replace]. */
    @Test
    fun zipHoldsRecordsPurgesAndPhotosAndIsWrittenAtomically() {
        val a = Phone("A")
        val hash = a.blobs.add(byteArrayOf(1, 2, 3))
        run {
            val item = a.device.inventory.addItem("lamp")
            a.device.inventory.addPhoto(item, null, hash)
            a.device.inventory.deleteItem(a.device.inventory.addItem("gone"))
        }
        val calls = mutableListOf<String>()
        val watching = object : SyncFolder by target {
            override fun replace(path: String, bytes: ByteArray) { calls += path; target.replace(path, bytes) }
            override fun delete(path: String) { calls += "delete $path"; target.delete(path) }
        }
        val name = run { a.backup.write(watching, 0) }!!
        assertEquals(listOf(name), calls)

        val entries = mutableListOf<String>()
        ZipInputStream(target.read(name)!!.inputStream()).use { z -> generateSequence { z.nextEntry }.forEach { entries += it.name } }
        assertEquals(setOf("records.jsonl", "purges.jsonl", "blobs/$hash"), entries.toSet())
    }

    /** The backup is one snapshot: a row written while it is being read is wholly in it or wholly out of it. */
    @Test
    fun aWriteDuringTheBackupDoesNotTearIt() {
        val a = Phone("A")
        val inv = a.device.inventory
        val tag = run { inv.addTag("t") }
        run { inv.addItem("first") }
        val scope = CoroutineScope(Dispatchers.IO)
        var late: Deferred<Unit>? = null
        a.backup.afterTableRead = { table ->
            if (table == ITEM && late == null) {
                late = scope.async { val n = inv.addItem("late"); inv.tag(listOf(n), tag) }
                // A write from outside waits for the snapshot; torn, it gets in at once.
                withTimeoutOrNull(500) { late!!.await() }
            }
        }
        val name = run { a.backup.write(target, 0) }!!
        run { late!!.await() }

        val b = Phone("B")
        run { b.backup.restore(target.read(name)!!) }
        assertEquals(emptyList(), run { b.device.db.syncDao().waiting() }, "a link whose item is not in the backup")
    }

    /** A backup holding records stamped more than a day ahead of this device is refused whole, and nothing is written. */
    @Test
    fun restoreRefusesFarFutureRecordsBeforeWritingAnything() {
        val a = Phone("A")
        a.device.skew = 3 * day
        val hash = a.blobs.add(byteArrayOf(9, 9))
        run { a.device.inventory.addPhoto(a.device.inventory.addItem("from the future"), null, hash) }
        val name = run { a.backup.write(target, 0) }!!

        val b = Phone("B")
        val e = assertFailsWith<IllegalStateException> { run { b.backup.restore(target.read(name)!!) } }
        assertTrue("ahead" in e.message.orEmpty(), e.message)
        assertTrue(b.device.rows(ITEM).isEmpty())
        assertNull(b.store.read(hash), "no photo file is stored by a refused restore")
    }

    @Test
    fun noBackupWhileTheDatabaseReReadsItsOwnFiles() {
        val a = Phone(world, "A", recovered = true)
        assertNull(run { a.backup.write(target, 0) })
        assertEquals(emptyList(), target.list(""))
    }

    /** A restore that takes back a purge has the next import read every folder file again, this device's own too (R59). */
    @Test
    fun aRestoreThatTakesBackAPurgeReadsTheOwnLogAgain() {
        val a = Phone("A")
        val b = Phone("B")
        val item = run { a.device.inventory.addItem("lamp") }
        a.device.export()
        world.syncthing.session("A", "B")
        b.device.import()
        world.syncthing.now = 10
        run { a.device.inventory.deleteItem(item) }
        world.syncthing.now = 20
        run { b.device.inventory.rename(item, "lamp, edited after the purge") }
        val name = run { b.backup.write(target, world.syncthing.now) }!!
        assertFalse(run { a.device.sync.readingOwnLog() })

        run { a.backup.restore(target.read(name)!!) }

        assertNotNull(run { a.device.inventory.getItem(item) }, "the edit after the purge takes it back")
        assertTrue(run { a.device.sync.readingOwnLog() }, "the own log is read again on the next import")
        a.device.import()
        assertFalse(run { a.device.sync.readingOwnLog() })
    }

    @Test
    fun pruneNeverDeletesTheFileJustWritten() {
        val a = Phone("A")
        (10..12).forEach { run { a.backup.write(target, 20_000L * day + it * day) } }
        val old = run { a.backup.daily(target, 20_000L * day, keep = 3) }!!
        val left = target.list("").sorted()
        assertEquals(3, left.size)
        assertTrue(old in left)
    }

    @Test
    fun leftoverTempFilesAreClearedOnTheNextWrite() {
        val a = Phone("A")
        val temp = targetDir.resolve(".tmp-chatelaine-backup-1999-01-01.zip").also { Files.write(it, byteArrayOf(1)) }
        val name = run { a.backup.write(target, 0) }!!
        assertEquals(listOf(name), target.list(""))
        assertFalse(Files.exists(temp))
    }

    @Test
    fun aTruncatedOrIncompleteZipIsNotABackup() {
        val a = Phone("A")
        run { a.device.inventory.addItem("x") }
        val zip = target.read(run { a.backup.write(target, 0) }!!)!!
        for (cut in listOf(10, 30, zip.size / 2)) {
            assertFailsWith<IllegalArgumentException>("cut $cut") { run { a.backup.restore(zip.copyOf(zip.size - cut)) } }
        }
        val noPurges = ZipBackup.pack(mapOf("records.jsonl" to ByteArray(0)))
        assertFailsWith<IllegalArgumentException> { run { a.backup.restore(noPurges) } }
    }

    /** R152 over R61: a roll back does not put an item back in a place deleted since the backup; its move up stays. */
    @Test
    fun rollBackLeavesAnItemOutOfAPlaceDeletedSinceTheBackup() {
        val a = Phone("A")
        val inv = a.device.inventory
        val room = run { inv.addPlace("room") }
        val drawer = run { inv.addPlace("drawer", room) }
        val item = run { inv.addItem("scissors").also { inv.placeItem(it, drawer) } }
        val zip = target.read(run { a.backup.write(target, 0) }!!)!!
        run { inv.deletePlace(drawer) }

        val report = run { a.backup.rollBack(zip) }

        assertEquals(room, run { inv.getItem(item) }!!.placeId, "the item stays where the delete moved it")
        assertEquals(RollBackReport(written = 0, missingParent = 1, purgedOwner = 0), report)
    }

    /** Photo entries are stored as they are (JPEGs do not deflate). */
    @Test
    fun photoEntriesAreStoredNotDeflated() {
        val a = Phone("A")
        val hash = a.blobs.add(ByteArray(2000))
        run { a.device.inventory.addPhoto(a.device.inventory.addItem("lamp"), null, hash) }
        val zip = target.read(run { a.backup.write(target, 0) }!!)!!
        var seen = 0
        ZipInputStream(zip.inputStream()).use { z ->
            generateSequence { z.nextEntry }.forEach {
                if (it.name.startsWith("blobs/")) { seen++; assertEquals(ZipEntry.STORED, it.method) }
            }
        }
        assertEquals(1, seen)
    }

    @Test
    fun aValueSkippedForAGoneParentIsCounted() {
        val a = Phone("A")
        val inv = a.device.inventory
        val x = run { inv.addItem("x") }
        val y = run { inv.addItem("y") }
        val photo = run { inv.addPhoto(x, null, a.blobs.add(byteArrayOf(5))) }
        val region = run { inv.addRegion(photo, 0.1, 0.1, 0.2, 0.2, y) }
        val name = run { a.backup.write(target, 0) }!!
        // The region is pointed at x, then y is deleted: the backup's target names an item that is gone.
        run {
            a.device.writes.write(mapOf(PHOTO_REGION to listOf(region))) { store ->
                store.put(requireNotNull(store.row(region)).edit(TARGET, a.device.clock.tick(), mapOf("itemId" to x)))
            }
            inv.deleteItem(y)
        }
        val report = run { a.backup.rollBack(target.read(name)!!) }
        assertEquals(RollBackReport(written = 0, missingParent = 1, purgedOwner = 0), report)
        assertEquals(x, a.device.rows(PHOTO_REGION).getValue(region).groups.getValue(TARGET).values["itemId"])
    }

    @Test
    fun aFileThatIsNotABackupIsRefused() {
        val a = Phone("A")
        assertFailsWith<IllegalArgumentException> { run { a.backup.restore(byteArrayOf(1, 2, 3)) } }
    }
}
