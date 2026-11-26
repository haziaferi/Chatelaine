package com.hazia.chatelaine.data.app

import com.hazia.chatelaine.data.DATABASE_NAME
import com.hazia.chatelaine.data.openChatelaineDatabase
import com.hazia.chatelaine.data.sync.DirectorySyncFolder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** R60: the daily backup at start, from the settings, once a day however long the device was off. */
class DailyBackupTest {

    @get:Rule val tmp = TemporaryFolder()

    private val hour = 60L * 60 * 1000
    private val now = 1_800_000_000_000L
    private val dir by lazy { tmp.newFolder("device") }
    private val backups by lazy { tmp.newFolder("backups") }
    private val settings by lazy { FileSettingsStore(File(dir, "$LOCAL_DIR/settings.json")) }
    private val folderOf = { path: String -> DirectorySyncFolder(File(path).toPath()) }
    private var opened: LocalAppData? = null

    private fun app(): LocalAppData {
        val file = File(dir, DATABASE_NAME)
        return LocalAppData.open(file, dir, open = { openChatelaineDatabase(file) }).also { opened = it }
    }

    @After
    fun close() {
        opened?.close()
    }

    private fun backupFolder(last: Long?) = runBlocking { settings.update { it.copy(backupFolder = backups.path, lastBackupMillis = last) } }

    private fun backupIfDue(app: LocalAppData) = runBlocking { backupIfDue(app, settings, folderOf, now) }

    private fun written() = backups.list()!!.toList()

    @Test
    fun noBackupFolderNothingWritten() {
        assertNull(backupIfDue(app()))
        assertEquals(emptyList(), written())
        assertNull(settings.settings.value.lastBackupMillis)
    }

    @Test
    fun notDueNothingWritten() {
        backupFolder(last = now - 23 * hour)

        assertNull(backupIfDue(app()))
        assertEquals(emptyList(), written())
        assertEquals(now - 23 * hour, settings.settings.value.lastBackupMillis)
    }

    @Test
    fun dueWritesAndRecordsTime() {
        backupFolder(last = null)

        val name = assertNotNull(backupIfDue(app()))

        assertEquals(listOf(name), written())
        assertEquals(now, settings.settings.value.lastBackupMillis)
    }

    /** A PC that was off for three days makes one backup at start, not three. */
    @Test
    fun dailyBackupAfterThreeDaysIsOne() {
        backupFolder(last = now - 72 * hour)
        val app = app()

        assertNotNull(backupIfDue(app))
        assertNull(backupIfDue(app))
        assertEquals(1, written().size)
    }

    @Test
    fun heldBackupDoesNotRecordTime() {
        File(dir, DATABASE_NAME).writeBytes(Random(7).nextBytes(4096))
        backupFolder(last = now - 48 * hour)
        val app = app()
        assertTrue(runBlocking { app.recovery() }.backupsHeld)

        assertNull(backupIfDue(app))
        assertEquals(emptyList(), written())
        assertEquals(now - 48 * hour, settings.settings.value.lastBackupMillis)
    }
}
