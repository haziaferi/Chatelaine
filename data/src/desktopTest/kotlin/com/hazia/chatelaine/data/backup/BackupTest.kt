package com.hazia.chatelaine.data.backup

import com.hazia.chatelaine.data.FieldInput
import com.hazia.chatelaine.data.FieldType
import com.hazia.chatelaine.data.ITEM
import com.hazia.chatelaine.data.NAME
import com.hazia.chatelaine.data.SYNCED_TABLES
import com.hazia.chatelaine.data.blob.Blobs
import com.hazia.chatelaine.data.blob.MemoryBlobStore
import com.hazia.chatelaine.data.sync.DirectorySyncFolder
import com.hazia.chatelaine.data.sync.SyncFolder
import com.hazia.chatelaine.data.sync.World
import kotlinx.coroutines.runBlocking
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
    private val target: SyncFolder = DirectorySyncFolder(Files.createTempDirectory(dir.toPath(), "target"))
    private val day = 86_400_000L

    @AfterTest
    fun close() = world.close()

    /** A device with its photo files in memory, and the [Backup] over both. */
    private inner class Phone(name: String) {
        val device = world.Device(name)
        val store = MemoryBlobStore()
        val blobs = Blobs(device.db, store)
        val backup = Backup(device.db, store, device.clock, ZipBackup, wallMillis = { world.syncthing.now })
    }

    private fun <T> run(block: suspend () -> T): T = runBlocking { block() }

    /** R60, stage 1 gate: 20 items, 5 places, 6 tags, 3 photos and fields, exported and imported into an empty database. */
    @Test
    fun exportThenImportGivesTheSameInventory() {
        val a = Phone("A")
        val inv = a.device.inventory
        val items = run { (1..20).map { inv.addItem("item $it") } }
        val places = run { (1..5).map { inv.addPlace("place $it") } }
        val tags = run { (1..6).map { inv.addTag("tag $it") } }
        val pictures = (1..3).map { n -> a.blobs.add(ByteArray(100) { b -> (b * n).toByte() }) }
        run {
            inv.tag(items.take(5), tags[0])
            inv.tagPlace(places[0], tags[1])
            val photos = pictures.mapIndexed { i, hash -> inv.addPhoto(if (i < 2) items[i] else null, if (i < 2) null else places[0], hash) }
            inv.addRegion(photos[0], 0.1, 0.1, 0.2, 0.2, items[0])
            a.device.fields.define("colour", FieldType.CHOICE, options = listOf("red", "blue"))
            val def = a.device.fields.define("size", FieldType.TEXT)
            a.device.fields.attachToItem(def, items[0])
            a.device.fields.set(items[0], def, FieldInput.Text("big"))
        }
        world.syncthing.now += 5_000
        val name = run { a.backup.write(target, world.syncthing.now) }
        assertEquals("chatelaine-backup-1970-01-01.zip", name)

        val b = Phone("B")
        val report = run { b.backup.restore(target.read(name)!!) }

        assertEquals(0, report.skipped)
        for (table in SYNCED_TABLES) {
            val mine = a.device.rows(table)
            assertTrue(mine.isNotEmpty(), "the workload left $table empty")
            assertEquals(mine, b.device.rows(table), "table $table differs")
        }
        for (hash in pictures) assertEquals(a.store.read(hash)!!.toList(), b.store.read(hash)?.toList(), "the photo $hash")
    }

    /** R61: edits made after the backup survive a restore, on both devices. */
    @Test
    fun restoreKeepsEditsNewerThanTheBackup() {
        val a = Phone("A")
        val b = Phone("B")
        val x = run { a.device.inventory.addItem("x old") }
        val y = run { a.device.inventory.addItem("y old") }
        world.settle(listOf(a.device, b.device))
        val name = run { a.backup.write(target, world.syncthing.now) }

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
        val name = run { a.backup.write(target, world.syncthing.now) }

        world.syncthing.now += 1_000
        run { a.device.inventory.rename(item, "hair clip") }
        run { a.device.inventory.deleteItem(gone) }
        val newer = run { a.device.inventory.addItem("made after the backup") }
        run { a.backup.rollBack(target.read(name)!!) }

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
        val names = (0 until 8).map { d -> run { a.backup.write(target, 20_000L * day + d * day) }.also { a.backup.prune(target) } }
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
        val name = run { a.backup.write(watching, 0) }
        assertEquals(listOf(name), calls)

        val entries = mutableListOf<String>()
        ZipInputStream(target.read(name)!!.inputStream()).use { z -> generateSequence { z.nextEntry }.forEach { entries += it.name } }
        assertEquals(setOf("records.jsonl", "purges.jsonl", "blobs/$hash"), entries.toSet())
    }

    @Test
    fun aFileThatIsNotABackupIsRefused() {
        val a = Phone("A")
        assertFailsWith<IllegalArgumentException> { run { a.backup.restore(byteArrayOf(1, 2, 3)) } }
    }
}
