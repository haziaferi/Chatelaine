package com.hazia.chatelaine.data

import com.hazia.chatelaine.data.backup.assertExportImportRoundTrip
import com.hazia.chatelaine.data.sync.DirectorySyncFolder
import com.hazia.chatelaine.data.sync.World
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The three round trips of the stage 1 gate (R100), end to end, over the helpers the earlier tasks built: [World]
 * (devices sharing a folder through the Syncthing double), the backup round trip, and the database recovery.
 */
class StageGateTest {

    private val dir = Files.createTempDirectory("chatelaine-gate").toFile().also { it.deleteOnExit() }
    private val world = World(dir, seed = 100)

    @AfterTest
    fun close() = world.close()

    /** A populated inventory exported and imported into an empty database: every table, the purges and every photo equal. */
    @Test
    fun exportImport() = assertExportImportRoundTrip(world, DirectorySyncFolder(Files.createTempDirectory(dir.toPath(), "target")))

    /** Ten names edited on both devices while offline: both converge and the device that lost keeps every overwritten name. */
    @Test
    fun twoDeviceSyncWithConflicts() {
        val a = world.Device("A")
        val b = world.Device("B")
        val items = (1..10).map { a.create("item $it") }
        world.settle(listOf(a, b))
        assertEquals(10, b.rows().size)

        // Offline: each device renames the same ten items; B always edits last, so A is the one that loses.
        val loserNames = mutableMapOf<String, String>()
        val winnerNames = mutableMapOf<String, String>()
        for ((i, item) in items.withIndex()) {
            world.syncthing.now += 1_000
            a.edit(item, NAME, "A$i")
            world.syncthing.now += 1_000
            b.edit(item, NAME, "B$i")
            loserNames[item] = "A$i"
            winnerNames[item] = "B$i"
        }
        world.settle(listOf(a, b))

        world.assertJoined(listOf(a, b))
        assertEquals(a.rows(), b.rows(), "both hold the same rows")
        for (item in items) {
            for (d in listOf(a, b)) assertEquals(winnerNames[item], d.rows().getValue(item).groups.getValue(NAME).values["name"], "${d.name} $item")
            val kept = runBlocking { a.history.overwritten(ITEM, item, NAME) }.map { it.values["name"] }
            assertTrue(loserNames.getValue(item) in kept, "A's overwritten name ${loserNames[item]} is in A's history: $kept")
        }
    }

    /** Garbage as the database file: a fresh database opens, and the garbage stays byte for byte under the set-aside name. */
    @Test
    fun corruptedDatabaseIsKept() {
        val file = File(dir, DATABASE_NAME)
        val garbage = Random(42).nextBytes(8192)
        file.writeBytes(garbage)

        val open = openChatelaineDatabase(file)
        try {
            assertTrue(open.recovered)
            assertContentEquals(garbage, File(dir, "$DATABASE_NAME$UNOPENABLE_SUFFIX").readBytes())
            assertContentEquals(garbage, open.keptAs!!.readBytes())
            assertTrue(runBlocking { Inventory(open.database, LocalWrites(open.database, com.hazia.chatelaine.core.sync.HybridClock("x", System::currentTimeMillis))).items() }.isEmpty())
        } finally {
            open.database.close()
        }
        assertFalse(openChatelaineDatabase(file).also { it.database.close() }.recovered)
    }
}
