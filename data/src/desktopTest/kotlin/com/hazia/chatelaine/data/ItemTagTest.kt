package com.hazia.chatelaine.data

import com.hazia.chatelaine.core.sync.HybridClock
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ItemTagTest {

    private val dir: File = Files.createTempDirectory("chatelaine-items").toFile().also { it.deleteOnExit() }
    private val open = openChatelaineDatabase(File(dir, DATABASE_NAME))
    private val db = open.database
    private var now = 1_000L
    private val clock = HybridClock("device-a", { now++ })
    private val inventory = Inventory(db, LocalWrites(db, clock))

    @AfterTest
    fun close() = db.close()

    private fun <T> run(block: suspend () -> T): T = runBlocking { block() }

    /** R25, R29, R30: a name is all an item needs, and without a place it is unplaced. */
    @Test
    fun itemExistsWithNameOnlyAndIsUnplaced() = run {
        val id = inventory.addItem("scale remover")
        val item = assertNotNull(inventory.getItem(id))
        assertEquals("scale remover", item.name)
        assertEquals("", item.notes)
        assertNull(item.count)
        assertNull(item.placeId)
        assertEquals(ItemState.UNPLACED, inventory.state(id))
    }

    /** R28: three identical headphones become three items of one. */
    @Test
    fun splitThreeHeadphonesIntoThree() = run {
        val id = inventory.addItem("headphones")
        inventory.setQuantity(id, 3, approx = false)
        val tag = inventory.addTag("audio")
        inventory.tag(listOf(id), tag)

        val parts = inventory.splitItem(id, 3)

        assertEquals(3, parts.size)
        assertEquals(3, parts.toSet().size)
        for (part in parts) {
            val item = assertNotNull(inventory.getItem(part))
            assertEquals("headphones", item.name)
            assertEquals(1L, item.count)
            assertEquals(listOf(tag), inventory.tagsOf(part).map { it.id })
        }
    }

    /** R35: one parent, no cycles. */
    @Test
    fun tagHasOneParentAndNoCycles() = run {
        val a = inventory.addTag("a")
        val b = inventory.addTag("b")
        inventory.setParent(a, b)
        assertEquals(b, inventory.getTag(a)?.parentId)
        assertFailsWith<IllegalArgumentException> { inventory.setParent(b, a) }
        assertFailsWith<IllegalArgumentException> { inventory.setParent(a, a) }
        assertNull(inventory.getTag(b)?.parentId)
    }

    /** R05: many items at once. */
    @Test
    fun bulkTagAndUntagInOneCall() = run {
        val ids = listOf("skirt one", "skirt two", "skirt three").map { inventory.addItem(it) }
        val redSkirt = inventory.addTag("red skirt")

        inventory.tag(ids, redSkirt)
        inventory.tag(ids, redSkirt) // again: still one link each
        for (id in ids) assertEquals(listOf(redSkirt), inventory.tagsOf(id).map { it.id })

        inventory.untag(ids, redSkirt)
        for (id in ids) assertTrue(inventory.tagsOf(id).isEmpty())
    }

    /** R152: sub-tags move to the parent; items lose only that tag. */
    @Test
    fun deletingTagMovesSubTagsUp() = run {
        val crafts = inventory.addTag("crafts")
        val sewing = inventory.addTag("sewing", crafts)
        val redSkirt = inventory.addTag("red skirt", sewing)
        val season = inventory.addTag("summer")
        val item = inventory.addItem("skirt")
        inventory.tag(listOf(item), sewing)
        inventory.tag(listOf(item), redSkirt)
        inventory.tag(listOf(item), season)

        inventory.deleteTag(sewing)

        assertNull(inventory.getTag(sewing))
        assertEquals(crafts, inventory.getTag(redSkirt)?.parentId)
        assertEquals(setOf(redSkirt, season), inventory.tagsOf(item).map { it.id }.toSet())
        assertTrue(sewing in db.syncDao().purges().map { it.id })

        // A top-level tag's sub-tags become top-level.
        inventory.deleteTag(crafts)
        assertNull(inventory.getTag(redSkirt)?.parentId)
    }

    /**
     * R152, R151. Field values arrive with the modules (Task 9), so this checks what exists: deleting a module's
     * tag purges the tag and its links and nothing else, which leaves a value table untouched.
     */
    @Test
    fun deletingModuleTagHidesNotDeletesValues() = run {
        val pantry = inventory.addTag("pantry", moduleKey = "pantry")
        val other = inventory.addTag("kitchen")
        val item = inventory.addItem("rice")
        inventory.tag(listOf(item), pantry)
        inventory.tag(listOf(item), other)
        val linkIds = db.itemTagDao().linksOfItem(item).map { it.id }.toSet()
        val before = db.syncDao().purges().map { it.id }.toSet()

        inventory.deleteTag(pantry)

        val purged = db.syncDao().purges().map { it.id }.toSet() - before
        val remaining = db.itemTagDao().linksOfItem(item).single().id
        assertEquals(2, linkIds.size)
        assertEquals(setOf(pantry) + (linkIds - remaining), purged)
        assertNotNull(inventory.getItem(item))
        assertEquals(listOf(other), inventory.tagsOf(item).map { it.id })
    }

    @Test
    fun deletedItemIsPurged() = run {
        val tag = inventory.addTag("tools")
        val id = inventory.addItem("drill")
        inventory.tag(listOf(id), tag)
        val link = db.itemTagDao().linksOfItem(id).single().id

        inventory.deleteItem(id)

        assertNull(inventory.getItem(id))
        val purged = db.syncDao().purges().map { it.id }
        assertTrue(id in purged)
        assertTrue(link in purged)
        assertNotNull(inventory.getTag(tag))
        assertTrue(db.itemTagDao().linksOfItem(id).isEmpty())
    }

    /** R38, R47. */
    @Test
    fun newDatabaseHasTheFiveDefaultKinds() = run {
        val kinds = inventory.kinds()
        assertEquals(listOf("function", "project", "season", "place-like", "other"), kinds.map { it.name })

        inventory.renameKind(kinds[0].id, "purpose")
        val extra = inventory.addKind("colour")
        inventory.deleteKind(kinds[4].id)
        assertEquals(listOf("purpose", "project", "season", "place-like", "colour"), inventory.kinds().map { it.name })

        // A tag of a deleted kind keeps no kind.
        val tag = inventory.addTag("blue")
        inventory.setKind(tag, extra)
        inventory.deleteKind(extra)
        assertNull(inventory.getTag(tag)?.kind)
        assertTrue(inventory.kinds().none { it.id == extra })
    }

    /** R39. */
    @Test
    fun acceptedSuggestionLooksLikeAUserTag() = run {
        val item = inventory.addItem("laptop")
        val suggested = inventory.suggestTag(item, "PC")
        assertTrue(assertNotNull(inventory.getTag(suggested)).suggested)
        assertEquals(listOf(suggested), inventory.tagsOf(item).map { it.id })

        inventory.acceptSuggestion(item, suggested)

        val own = inventory.addTag("PC")
        val accepted = assertNotNull(inventory.getTag(suggested))
        assertFalse(accepted.suggested)
        assertEquals(inventory.getTag(own)?.copy(id = accepted.id), accepted)
        assertContentEquals(listOf(suggested), inventory.tagsOf(item).map { it.id })
    }
}
