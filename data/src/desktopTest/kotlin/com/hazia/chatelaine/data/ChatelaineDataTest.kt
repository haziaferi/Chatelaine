package com.hazia.chatelaine.data

import com.hazia.chatelaine.data.blob.MemoryBlobStore
import com.hazia.chatelaine.data.sync.DirectorySyncFolder
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** The wiring of one device's data layer, as the debug harnesses use it, across a restart. */
class ChatelaineDataTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun device(file: File) = ChatelaineData(openChatelaineDatabase(file), "device-a", MemoryBlobStore(), wallMillis = { 1_000 })

    /**
     * R59, R60: a database made anew makes no backup before it has read its own log back, neither in the process that
     * set the corrupt file aside nor in the next one; seven such backups would rotate out every good one.
     */
    @Test
    fun aRecoveredDatabaseMakesNoBackupBeforeItsFirstSync() {
        val file = File(tmp.root, DATABASE_NAME)
        val folder = DirectorySyncFolder(tmp.newFolder("sync").toPath())
        val backups = DirectorySyncFolder(tmp.newFolder("backups").toPath())
        device(file).use { runBlocking { it.add("lamp"); it.sync(folder) } }
        file.writeBytes(Random(7).nextBytes(4096))

        val made = mutableListOf<String?>()
        device(file).use { made += runBlocking { it.backup(backups) } } // recovered in this process
        device(file).use { made += runBlocking { it.backup(backups) } } // the next process opens the new file as it is

        assertEquals(listOf<String?>(null, null), made, "no backup of the near-empty database")
        device(file).use { data ->
            runBlocking { data.sync(folder) }
            assertEquals(listOf("lamp"), runBlocking { data.list() }.map { it.name }, "the own log was read back")
            val name: String? = runBlocking { data.backup(backups) }
            assertNotNull(name, "backups resume once it was")
        }
    }
}
