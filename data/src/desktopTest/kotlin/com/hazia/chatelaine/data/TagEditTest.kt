package com.hazia.chatelaine.data

import com.hazia.chatelaine.core.sync.HybridClock
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class TagEditTest {

    private val dir: File = Files.createTempDirectory("chatelaine-tagedit").toFile().also { it.deleteOnExit() }
    private val open = openChatelaineDatabase(File(dir, DATABASE_NAME))
    private val db = open.database
    private var now = 1_000L
    private val clock = HybridClock("device-a", { now++ })
    private val inventory = Inventory(db, LocalWrites(db, clock))

    @AfterTest
    fun close() = db.close()

    private fun <T> run(block: suspend () -> T): T = runBlocking { block() }

    @Test
    fun renameTagKeepsItsItemsAndChildren() = run {
        val sewing = inventory.addTag("sewing")
        val red = inventory.addTag("red skirt", sewing)
        val item = inventory.addItem("pins")
        inventory.tag(listOf(item), sewing)

        inventory.renameTag(sewing, "  cucito ")

        assertEquals("cucito", assertNotNull(inventory.getTag(sewing)).name)
        assertEquals(listOf(item), inventory.itemsWith(sewing).map { it.id })
        assertEquals(sewing, assertNotNull(inventory.getTag(red)).parentId)
    }

    @Test
    fun renameTagToBlankIsRefused() = run {
        val id = inventory.addTag("sewing")
        assertFailsWith<IllegalArgumentException> { inventory.renameTag(id, "  ") }
        assertEquals("sewing", assertNotNull(inventory.getTag(id)).name)
    }

    /** R151. */
    @Test
    fun renamedModuleTagKeepsItsModule() = run {
        val id = inventory.addModuleTag("consumable", "pantry")
        inventory.renameTag(id, "stock")
        val tag = assertNotNull(inventory.getTag(id))
        assertEquals("stock", tag.name)
        assertEquals("pantry", tag.moduleKey)
    }

    @Test
    fun tagCountsForAThousandItems() = run {
        val a = inventory.addTag("a")
        val b = inventory.addTag("b")
        val items = List(1000) { inventory.addItem("item $it") }
        inventory.tag(items, a)
        inventory.tag(items.filterIndexed { i, _ -> i % 2 == 0 }, b)

        val counts = inventory.tagCounts(items)

        assertEquals(listOf(a to 1000, b to 500), counts.map { it.tag.id to it.items })
    }

    @Test
    fun tagCountsSaySomeOrAll() = run {
        val a = inventory.addTag("a")
        val b = inventory.addTag("b")
        val items = List(3) { inventory.addItem("item $it") }
        inventory.tag(items, a)
        inventory.tag(items.take(1), b)
        inventory.suggestTag(items[1], "c")

        val counts = inventory.tagCounts(items)

        assertEquals(listOf(a to 3, b to 1), counts.map { it.tag.id to it.items })
    }
}
