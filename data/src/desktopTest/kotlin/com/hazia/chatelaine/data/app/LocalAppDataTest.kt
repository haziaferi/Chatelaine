package com.hazia.chatelaine.data.app

import com.hazia.chatelaine.data.DATABASE_NAME
import com.hazia.chatelaine.data.ITEM
import com.hazia.chatelaine.data.UNOPENABLE_SUFFIX
import com.hazia.chatelaine.data.openChatelaineDatabase
import com.hazia.chatelaine.data.sync.ConflictCopies
import com.hazia.chatelaine.data.sync.DirectorySyncFolder
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** The app's data composition on one device, across restarts, and two devices through one folder. */
class LocalAppDataTest {

    @get:Rule val tmp = TemporaryFolder()

    private val kept = "$DATABASE_NAME$UNOPENABLE_SUFFIX"
    private val now = 1_800_000_000_000L

    private fun device(dir: File): LocalAppData {
        val file = File(dir, DATABASE_NAME)
        return LocalAppData.open(file, dir, open = { openChatelaineDatabase(file) })
    }

    private fun corrupt(dir: File) = File(dir, DATABASE_NAME).writeBytes(Random(7).nextBytes(4096))

    private fun names(app: LocalAppData) = runBlocking { app.inventory.items() }.map { it.name }.sorted()

    private fun conflictCopies(folder: File) = folder.walk().filter { ConflictCopies.isCopy(it.name) }.map { it.name }.toList()

    @Test
    fun sharedOpensOnePerProcess() {
        val dir = tmp.newFolder("shared")
        val file = File(dir, DATABASE_NAME)
        var opens = 0
        LocalAppData.shared(file, dir) { opens++; openChatelaineDatabase(file) }.use { first ->
            assertSame(first, LocalAppData.shared(file, dir) { opens++; openChatelaineDatabase(file) })
            assertEquals(1, opens)
            val other = File(tmp.newFolder("other"), DATABASE_NAME)
            assertFailsWith<IllegalStateException> { LocalAppData.shared(other, other.parentFile) { openChatelaineDatabase(other) } }
            assertEquals(1, opens)
        }
    }

    /** A closed shared instance is not handed out again: the next call opens the database anew. */
    @Test
    fun aClosedSharedInstanceIsOpenedAgain() {
        val dir = tmp.newFolder("shared")
        val file = File(dir, DATABASE_NAME)
        val first = LocalAppData.shared(file, dir) { openChatelaineDatabase(file) }
        runBlocking { first.inventory.addItem("pins") }
        first.close()

        LocalAppData.shared(file, dir) { openChatelaineDatabase(file) }.use { again ->
            assertNotSame(first, again)
            assertEquals(listOf("pins"), names(again))
        }
    }

    @Test
    fun changesEmitAfterAnItemIsAdded() {
        device(tmp.newFolder("a")).use { app ->
            var seen = 0
            runBlocking {
                withTimeout(10_000) {
                    app.changes(ITEM).take(2).collect {
                        seen++
                        if (seen == 1) app.inventory.addItem("pins")
                    }
                }
            }
            assertEquals(2, seen, "the start emission, then one after the add")
        }
    }

    @Test
    fun recoveryNoticeKeysOnTheKeptFile() {
        val dir = tmp.newFolder("a")
        corrupt(dir)

        device(dir).use { app ->
            val state = runBlocking { app.recovery() }
            assertEquals(listOf(kept), state.keptFiles)
            assertTrue(state.due)
            assertTrue(state.backupsHeld)
        }
        device(dir).use { app ->
            assertEquals(listOf(kept), runBlocking { app.recovery() }.keptFiles, "still listed after a restart")
        }
    }

    /**
     * A kept file beside a healthy database whose own log was read back (no own-log row), such as one left from
     * stage 1: the notice is due on the file alone, and the device keeps its id and its backups, after a reopen too.
     */
    @Test
    fun keptFileShowsEvenWithoutTheMark() {
        val dir = tmp.newFolder("a")
        val folder = DirectorySyncFolder(tmp.newFolder("sync").toPath())
        val id = device(dir).use { app ->
            runBlocking { app.inventory.addItem("pins"); app.sync(folder) }
            app.deviceId
        }
        File(dir, kept).writeBytes(Random(7).nextBytes(4096))

        repeat(2) {
            device(dir).use { app ->
                val state = runBlocking { app.recovery() }
                assertTrue(state.due)
                assertEquals(listOf(kept), state.keptFiles)
                assertFalse(state.backupsHeld)
                assertFalse(state.waitingForOwnLog)
                assertEquals(id, app.deviceId)
            }
        }
    }

    /** A new database that has not read its own log back, beside a kept file, as a crash before marking leaves it. */
    @Test
    fun keptFileBesideANewDatabaseRenews() {
        val dir = tmp.newFolder("a")
        val id = device(dir).use { it.deviceId }
        File(dir, kept).writeBytes(Random(7).nextBytes(4096))

        device(dir).use { app ->
            assertNotEquals(id, app.deviceId)
            assertTrue(runBlocking { app.recovery() }.backupsHeld)
        }
    }

    /** R61: a restored backup lets backups go on. */
    @Test
    fun restoreLiftsBackupsHeld() {
        val backupsDir = tmp.newFolder("backups")
        val backups = DirectorySyncFolder(backupsDir.toPath())
        val name = device(tmp.newFolder("b")).use { b ->
            runBlocking { b.inventory.addItem("pins"); b.backup(backups, now) }
        }
        val zip = File(backupsDir, assertNotNull(name)).readBytes()
        val dir = tmp.newFolder("a")
        corrupt(dir)

        device(dir).use { a ->
            assertTrue(runBlocking { a.recovery() }.backupsHeld)
            runBlocking { a.restore(zip) }
            assertFalse(runBlocking { a.recovery() }.backupsHeld)
            assertEquals(listOf("pins"), names(a))
        }
    }

    @Test
    fun twoDevicesSyncThroughAFolder() {
        val folder = DirectorySyncFolder(tmp.newFolder("sync").toPath())
        device(tmp.newFolder("a")).use { a ->
            device(tmp.newFolder("b")).use { b ->
                assertNotEquals(a.deviceId, b.deviceId)
                runBlocking {
                    a.inventory.addItem("pins")
                    a.sync(folder)
                    b.sync(folder)
                }
                assertEquals(listOf("pins"), names(b))
            }
        }
    }

    /** An import queues no export the next local write would sync for nothing. */
    @Test
    fun importDoesNotRetrigger() {
        val folder = DirectorySyncFolder(tmp.newFolder("sync").toPath())
        device(tmp.newFolder("a")).use { a ->
            device(tmp.newFolder("b")).use { b ->
                runBlocking {
                    a.inventory.addItem("pins")
                    a.sync(folder)
                    b.sync(folder)
                }
                assertEquals(listOf("pins"), names(b))
                assertEquals(0, runBlocking { b.pendingExport() })
            }
        }
    }

    /** Decision 8: the recovered device takes a new id and reads its old id's log as a peer's. */
    @Test
    fun recoveredDeviceGetsBackItsOldItems() {
        val dir = tmp.newFolder("a")
        val syncDir = tmp.newFolder("sync")
        val folder = DirectorySyncFolder(syncDir.toPath())
        val old = device(dir).use { a ->
            runBlocking { listOf("pins", "thread spool", "tape measure").forEach { a.inventory.addItem(it) }; a.sync(folder) }
            a.deviceId
        }
        corrupt(dir)

        device(dir).use { a ->
            assertNotEquals(old, a.deviceId)
            runBlocking { a.sync(folder) }
            assertEquals(listOf("pins", "tape measure", "thread spool"), names(a))
            assertEquals(emptyList(), conflictCopies(syncDir))
            assertFalse(runBlocking { a.recovery() }.backupsHeld)
        }
    }

    @Test
    fun editBeforeTheOldLogArrivesMakesNoClash() {
        val dir = tmp.newFolder("a")
        val syncDir = tmp.newFolder("sync")
        val folder = DirectorySyncFolder(syncDir.toPath())
        device(tmp.newFolder("b")).use { b ->
            val old = device(dir).use { a ->
                runBlocking { listOf("pins", "thread spool", "tape measure").forEach { a.inventory.addItem(it) }; a.sync(folder) }
                a.deviceId
            }
            runBlocking { b.sync(folder) }
            corrupt(dir)
            val oldDir = File(syncDir, "devices/$old")
            val away = File(tmp.root, "away")
            assertTrue(oldDir.renameTo(away))

            device(dir).use { a ->
                runBlocking { a.inventory.addItem("zip"); a.sync(folder) }
                assertTrue(runBlocking { a.recovery() }.backupsHeld, "the old log is not here yet")

                assertTrue(away.renameTo(oldDir))
                runBlocking {
                    a.sync(folder)
                    b.sync(folder)
                    a.sync(folder)
                }
                val all = listOf("pins", "tape measure", "thread spool", "zip")
                assertEquals(all, names(a))
                assertEquals(all, names(b))
                assertEquals(emptyList(), conflictCopies(syncDir))
            }
        }
    }

    @Test
    fun backupsStayHeldUntilTheOldLogIsRead() {
        val dir = tmp.newFolder("a")
        val syncDir = tmp.newFolder("sync")
        val folder = DirectorySyncFolder(syncDir.toPath())
        val backups = DirectorySyncFolder(tmp.newFolder("backups").toPath())
        val old = device(dir).use { a ->
            runBlocking { a.inventory.addItem("pins"); a.sync(folder) }
            a.deviceId
        }
        corrupt(dir)
        val oldDir = File(syncDir, "devices/$old")
        val away = File(tmp.root, "away")
        assertTrue(oldDir.renameTo(away))

        val renewed = device(dir).use { a ->
            runBlocking { a.sync(folder) }
            assertNull(runBlocking { a.backup(backups, now) }, "no backup of the near-empty database")
            a.deviceId
        }
        device(dir).use { a ->
            assertEquals(renewed, a.deviceId, "a restart keeps the renewed id")
            assertNull(runBlocking { a.backup(backups, now) }, "nor in the next process")

            assertTrue(away.renameTo(oldDir))
            runBlocking { a.sync(folder) }
            assertNotNull(runBlocking { a.backup(backups, now) }, "backups resume once the old log was read")
        }
    }
}
