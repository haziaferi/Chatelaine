package com.hazia.chatelaine.data

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import java.io.File
import java.nio.file.Files
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DatabaseRecoveryTest {

    private fun tempDir(): File = Files.createTempDirectory("chatelaine-db").toFile().also { it.deleteOnExit() }

    private fun SQLiteConnection.names(sql: String): List<String> =
        prepare(sql).use { s -> buildList { while (s.step()) add(s.getText(0)) } }

    /** R50: a corrupt file is set aside whole, never deleted, and a new empty database opens. */
    @Test
    fun corruptFileIsSetAsideAndKept() {
        val dir = tempDir()
        val file = File(dir, DATABASE_NAME)
        val garbage = Random(7).nextBytes(4096)
        file.writeBytes(garbage)

        val open = openChatelaineDatabase(file)
        try {
            assertTrue(open.recovered)
            assertTrue(file.exists(), "a new database was not created")
            val aside = File(dir, "chatelaine.db$UNOPENABLE_SUFFIX")
            assertTrue(aside.exists(), "the corrupt file was not kept")
            assertContentEquals(garbage, aside.readBytes())
        } finally {
            open.database.close()
        }

        val again = openChatelaineDatabase(file)
        try {
            assertFalse(again.recovered, "the new file must open as it is")
        } finally {
            again.database.close()
        }
    }

    @Test
    fun aSecondCorruptFileDoesNotReplaceTheFirstOneKept() {
        val dir = tempDir()
        val file = File(dir, DATABASE_NAME)
        val first = Random(1).nextBytes(4096)
        val second = Random(2).nextBytes(4096)

        file.writeBytes(first)
        openChatelaineDatabase(file).database.close()
        file.writeBytes(second)
        val open = openChatelaineDatabase(file)
        open.database.close()

        assertTrue(open.recovered)
        val kept = dir.listFiles { f -> f.name.startsWith("chatelaine.db$UNOPENABLE_SUFFIX") }!!.map { it.readBytes().toList() }
        assertEquals(setOf(first.toList(), second.toList()), kept.toSet())
    }

    /** The outbox: one insert, update and delete trigger per synced table, and a purge queues too. */
    @Test
    fun outboxTriggersExistAfterOpen() {
        val dir = tempDir()
        val file = File(dir, DATABASE_NAME)
        openChatelaineDatabase(file).database.close()

        val driver = BundledSQLiteDriver()
        val real = driver.open(file.path)
        try {
            val triggers = real.names("SELECT name FROM sqlite_master WHERE type = 'trigger'").toSet()
            assertEquals(setOf("purge_outbox_insert", "purge_outbox_update"), triggers)
        } finally {
            real.close()
        }

        // The registry is empty until data tables exist: a table registered here gets all three.
        val scratch = driver.open(File(dir, "registry.db").path)
        try {
            scratch.execSQL("CREATE TABLE sync_base (id TEXT, grp TEXT)")
            scratch.execSQL("CREATE TABLE sync_ask (id TEXT, grp TEXT)")
            scratch.execSQL("CREATE TABLE sync_outbox (n INTEGER PRIMARY KEY AUTOINCREMENT, id TEXT, tbl TEXT)")
            scratch.execSQL("CREATE TABLE purge_registry (id TEXT)")
            scratch.execSQL("CREATE TABLE widget (id TEXT PRIMARY KEY)")
            SchemaTriggers(listOf("widget")).onOpen(scratch)
            val widget = scratch.names("SELECT name FROM sqlite_master WHERE type = 'trigger' AND tbl_name = 'widget'").toSet()
            assertEquals(setOf("widget_outbox_insert", "widget_outbox_update", "widget_outbox_delete"), widget)

            scratch.execSQL("INSERT INTO widget(id) VALUES ('a')")
            assertEquals(listOf("widget"), scratch.names("SELECT tbl FROM sync_outbox"))
        } finally {
            scratch.close()
        }
    }

    /** A fake database whose probe fails for the first [failures] builds. */
    private class Fixture(
        private val failures: Int,
        private val failure: (Int) -> Throwable = { IllegalStateException("generation $it will not open") },
        private val closeThrows: Boolean = false,
        private val isUnopenable: (Throwable) -> Boolean = { true },
    ) {
        val events = mutableListOf<String>()
        private var builds = 0

        fun open() = openOrRecover(
            build = { events += "build"; ++builds },
            probe = { generation ->
                events += "probe($generation)"
                if (generation <= failures) throw failure(generation)
            },
            close = { events += "close($it)"; if (closeThrows) error("already closed") },
            setAside = { events += "setAside"; File("aside") },
            isUnopenable = isUnopenable,
        )
    }

    @Test
    fun aDatabaseThatOpensIsUsedAndNothingIsSetAside() {
        val f = Fixture(failures = 0)
        val result = f.open()

        assertFalse(result.recovered)
        assertEquals(1, result.database)
        assertEquals(listOf("build", "probe(1)"), f.events)
    }

    @Test
    fun aFailedProbeClosesBeforeSettingAsideThenRebuilds() {
        val f = Fixture(failures = 1)
        val result = f.open()

        assertTrue(result.recovered)
        assertEquals(2, result.database)
        assertEquals(listOf("build", "probe(1)", "close(1)", "setAside", "build", "probe(2)"), f.events)
    }

    @Test
    fun aFreshDatabaseThatStillFailsIsRethrownNotRecoveredAgain() {
        val f = Fixture(failures = 2)

        assertIs<IllegalStateException>(runCatching { f.open() }.exceptionOrNull())
        assertEquals(1, f.events.count { it == "setAside" })
        assertEquals(2, f.events.count { it == "build" })
    }

    @Test
    fun aFailureThatIsNotCorruptionIsRethrownWithTheFileLeftAlone() {
        val busy = IllegalStateException("database is locked")
        val f = Fixture(failures = 1, failure = { busy }, isUnopenable = { it !== busy })

        assertSame(busy, runCatching { f.open() }.exceptionOrNull())
        assertEquals(listOf("build", "probe(1)", "close(1)"), f.events)
    }

    @Test
    fun aDatabaseThatCannotBeClosedIsStillRecovered() {
        val f = Fixture(failures = 1, closeThrows = true)
        val result = f.open()

        assertTrue(result.recovered)
        assertEquals(2, result.database)
        assertEquals(listOf("build", "probe(1)", "close(1)", "setAside", "build", "probe(2)"), f.events)
    }
}
