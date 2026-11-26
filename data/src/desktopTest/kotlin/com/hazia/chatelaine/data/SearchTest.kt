package com.hazia.chatelaine.data

import com.hazia.chatelaine.core.sync.HybridClock
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchTest {

    private val dir: File = Files.createTempDirectory("chatelaine-search").toFile().also { it.deleteOnExit() }
    private val open = openChatelaineDatabase(File(dir, DATABASE_NAME))
    private val db = open.database
    private var now = 1_000L
    private val clock = HybridClock("device-a", { now++ })
    private val inventory = Inventory(db, LocalWrites(db, clock))

    @AfterTest
    fun close() = db.close()

    private fun <T> run(block: suspend () -> T): T = runBlocking { block() }

    private suspend fun ids(query: SearchQuery) = inventory.search(query).map { it.id }

    /** R25: an item with only a name is found by it. */
    @Test
    fun itemWithOnlyANameIsFoundByName() = run {
        val id = inventory.addItem("scale remover")
        inventory.addItem("pins")
        assertEquals(listOf(id), ids(SearchQuery("scale")))
    }

    @Test
    fun searchIgnoresCaseAndOuterSpaces() = run {
        val scale = inventory.addItem("scale remover")
        val perche = inventory.addItem("perché")
        assertEquals(listOf(scale), ids(SearchQuery("  SCALE ")))
        assertEquals(listOf(perche), ids(SearchQuery("PERCHÉ")))
        assertEquals(emptyList(), ids(SearchQuery("perche")))
    }

    /** R81: a parent tag's name finds the items of its sub-tags. */
    @Test
    fun parentTagFindsSubTagItems() = run {
        val sewing = inventory.addTag("sewing")
        val red = inventory.addTag("red skirt", sewing)
        val item = inventory.addItem("zip")
        inventory.tag(listOf(item), red)
        inventory.addItem("pins")
        assertEquals(listOf(item), ids(SearchQuery("sewing")))
    }

    @Test
    fun tagFilterIncludesSubTags() = run {
        val sewing = inventory.addTag("sewing")
        val red = inventory.addTag("red skirt", sewing)
        val other = inventory.addTag("garden")
        val a = inventory.addItem("pins")
        val b = inventory.addItem("zip")
        val c = inventory.addItem("rake")
        inventory.addItem("untagged")
        inventory.tag(listOf(a), sewing)
        inventory.tag(listOf(b), red)
        inventory.tag(listOf(c), other)
        assertEquals(setOf(a, b), ids(SearchQuery(tagIds = setOf(sewing))).toSet())
    }

    @Test
    fun twoTagFiltersMeanBoth() = run {
        val sewing = inventory.addTag("sewing")
        val red = inventory.addTag("red skirt", sewing)
        val winter = inventory.addTag("winter")
        val both = inventory.addItem("wool thread")
        val viaChild = inventory.addItem("red scarf")
        val onlySewing = inventory.addItem("pins")
        val onlyWinter = inventory.addItem("boots")
        inventory.tag(listOf(both), sewing)
        inventory.tag(listOf(both, viaChild, onlyWinter), winter)
        inventory.tag(listOf(viaChild), red)
        inventory.tag(listOf(onlySewing), sewing)
        assertEquals(setOf(both, viaChild), ids(SearchQuery(tagIds = setOf(sewing, winter))).toSet())
    }

    @Test
    fun unplacedOnlyLeavesOutPlacedAndAway() = run {
        val place = inventory.addPlace("shelf")
        val placed = inventory.addItem("placed")
        val away = inventory.addItem("away")
        val loose = inventory.addItem("loose")
        inventory.placeItem(placed, place)
        inventory.setAway(away, true)
        assertEquals(listOf(loose), ids(SearchQuery(unplacedOnly = true)))
    }

    @Test
    fun suggestedTagDoesNotMatch() = run {
        val item = inventory.addItem("zip")
        val tag = inventory.suggestTag(item, "sewing")
        assertTrue(ids(SearchQuery("sewing")).isEmpty())
        assertTrue(ids(SearchQuery(tagIds = setOf(tag))).isEmpty())
        inventory.acceptSuggestion(item, tag)
        assertEquals(listOf(item), ids(SearchQuery("sewing")))
    }

    @Test
    fun resultsSortByNameIgnoringCase() = run {
        val b = inventory.addItem("b")
        val a = inventory.addItem("A")
        val c = inventory.addItem("c")
        assertEquals(listOf(a, b, c), ids(SearchQuery()))
    }

    @Test
    fun blankQueryListsEveryItem() = run {
        inventory.addItem("one")
        inventory.addItem("two")
        assertEquals(2, ids(SearchQuery("   ")).size)
    }

    @Test
    fun matchingTagsPutsPrefixFirst() = run {
        val kit = inventory.addTag("hand sewing kit")
        val sewing = inventory.addTag("sewing")
        inventory.addTag("garden")
        assertEquals(listOf(sewing, kit), inventory.matchingTags(" SEW ").map { it.id })
        assertEquals(emptyList(), inventory.matchingTags("  "))
        assertEquals(1, inventory.matchingTags("sew", limit = 1).size)
    }

    @Test
    fun subTagWalkStopsOnACycle() = run {
        val a = inventory.addTag("a")
        val b = inventory.addTag("b", a)
        val rows = db.tagDao().tags(listOf(a)).map { it.copy(parentId = b) }
        db.tagDao().putTags(rows)
        assertEquals(setOf(a, b), inventory.subTagIds(a))
    }

    /** R05 in the data layer: tag a few items with a new tag, then find them by it. */
    @Test
    fun projectFromATag() = run {
        val sewing = inventory.addTag("sewing")
        val items = List(4) { inventory.addItem("item $it") }
        inventory.tag(items, sewing)
        val red = inventory.addTag("red skirt")
        inventory.tag(items.take(2), red)
        assertEquals(items.take(2).toSet(), ids(SearchQuery("red skirt")).toSet())
    }
}
