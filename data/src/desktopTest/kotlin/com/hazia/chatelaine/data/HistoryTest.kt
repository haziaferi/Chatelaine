package com.hazia.chatelaine.data

import com.hazia.chatelaine.core.sync.Group
import com.hazia.chatelaine.core.sync.HybridClock
import com.hazia.chatelaine.core.sync.Row
import com.hazia.chatelaine.core.sync.Stamp
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HistoryTest {

    private val dir: File = Files.createTempDirectory("chatelaine-history").toFile().also { it.deleteOnExit() }
    private val open = openChatelaineDatabase(File(dir, DATABASE_NAME))
    private val db = open.database
    private var now = 1_000L
    private val clock = HybridClock("device-a", { now++ })
    private val writes = LocalWrites(db, clock)
    private val inventory = Inventory(db, writes)
    private val fields = Fields(db, writes)
    private val history = History(db, writes)

    @AfterTest
    fun close() = db.close()

    private fun <T> run(block: suspend () -> T): T = runBlocking { block() }

    /** R54: A names the item "clip", B later "hair clip"; the merge keeps "hair clip" and "clip" can be brought back. */
    @Test
    fun overwrittenValueCanBeRestored() = run {
        val id = inventory.addItem("start")
        inventory.rename(id, "clip")
        val theirs = Row(ITEM, id, mapOf(NAME to Group(Stamp(5_000, "device-b"), mapOf("name" to "hair clip"))))
        writes.write(mapOf(ITEM to listOf(id))) { store -> writes.merger.import(store, listOf(theirs), emptyMap()) }

        assertEquals("hair clip", inventory.getItem(id)?.name)
        val lost = history.overwritten(ITEM, id, NAME).single()
        assertEquals("clip", lost.values["name"])
        assertTrue(history.overwritten(ITEM, id, NOTES).isEmpty())

        history.restore(lost)

        assertEquals("clip", inventory.getItem(id)?.name)
        // The value the restore replaced is kept in turn, and the restored one is no longer listed as lost.
        assertEquals(listOf("hair clip"), history.overwritten(ITEM, id, NAME).map { it.values["name"] })
    }

    /** R55: every group of every synced table keeps the version it loses; no group is left out. */
    @Test
    fun everyUserVisibleGroupKeepsItsLoser() = run {
        // One row in every synced table, so the groups are read from real rows and the check is not empty.
        val item = inventory.addItem("lamp")
        val tag = inventory.addTag("light")
        inventory.tag(listOf(item), tag)
        val place = inventory.addPlace("shelf")
        inventory.tagPlace(place, tag)
        val photo = inventory.addPhoto(item, null, "a".repeat(64))
        inventory.addRegion(photo, 0.1, 0.1, 0.2, 0.2, item)
        val def = fields.define("size", FieldType.TEXT)
        fields.attachToItem(def, item)
        fields.set(item, def, FieldInput.Text("big"))

        // Groups that are not user-visible would be listed here with a reason; there are none.
        val notUserVisible = emptySet<Pair<String, String>>()
        for ((table, rowTable) in db.syncedTables()) {
            val rows = rowTable.all()
            assertTrue(rows.isNotEmpty(), "no $table row to read the groups from")
            for (group in rows.flatMap { it.groups.keys }.toSet()) {
                if (table to group in notUserVisible) continue
                assertTrue(group in KEEP_LOSER_GROUPS, "$table.$group does not keep its losing value")
            }
        }
    }
}
