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
        assertEquals(3, db.itemTagDao().allLinks().size)

        inventory.untag(ids, redSkirt)
        for (id in ids) assertTrue(inventory.tagsOf(id).isEmpty())
        assertEquals(0, db.itemTagDao().allLinks().size)
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
     * R152, R151. Deleting a module's tag purges the tag and its links and nothing else: a value set through a
     * field attached to that tag stays in `field_value`, and reads as hidden, since the item no longer reaches the field.
     */
    @Test
    fun deletingModuleTagHidesNotDeletesValues() = run {
        val pantry = inventory.addModuleTag("pantry", "pantry")
        assertEquals("pantry", inventory.getTag(pantry)?.moduleKey)
        val other = inventory.addTag("kitchen")
        val item = inventory.addItem("rice")
        inventory.tag(listOf(item), pantry)
        inventory.tag(listOf(item), other)
        val fields = Fields(db, LocalWrites(db, clock))
        val shelf = fields.define("shelf life", FieldType.TEXT)
        fields.attachToTag(shelf, pantry)
        fields.set(item, shelf, FieldInput.Text("two years"))
        assertEquals(listOf(shelf), fields.fieldsFor(item).map { it.id })
        val linkIds = db.itemTagDao().linksOfItem(item).map { it.id }.toSet()
        val before = db.syncDao().purges().map { it.id }.toSet()
        val attachment = db.fieldAttachDao().allAttaches().single().id

        inventory.deleteTag(pantry)

        val kept = db.fieldValueDao().allValues().single()
        assertEquals(item, kept.ownerId)
        assertEquals(shelf, kept.defId)
        assertTrue(fields.fieldsFor(item).isEmpty(), "the field is hidden once its tag is gone")
        assertEquals(FieldInput.Text("two years"), fields.get(item, shelf))

        val purged = db.syncDao().purges().map { it.id }.toSet() - before
        val remaining = db.itemTagDao().linksOfItem(item).single().id
        assertEquals(2, linkIds.size)
        // The tag's attachment goes with it, on every device; the values stay.
        assertEquals(setOf(pantry, attachment) + (linkIds - remaining), purged)
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

    /** R39, R06: a suggestion belongs to one item's link; accepting it makes the link like one the person added. */
    @Test
    fun acceptedSuggestionLooksLikeAUserTag() = run {
        val item = inventory.addItem("laptop")
        val suggested = inventory.suggestTag(item, "PC")
        assertTrue(inventory.isSuggested(item, suggested))
        assertEquals(listOf(suggested), inventory.tagsOf(item).map { it.id })

        inventory.acceptSuggestion(item, suggested)

        val other = inventory.addItem("desktop")
        val own = inventory.addTag("own")
        inventory.tag(listOf(other), own)
        assertFalse(inventory.isSuggested(item, suggested))
        assertFalse(inventory.isSuggested(other, own))
        assertEquals(listOf(suggested), inventory.tagsOf(item).map { it.id })
    }

    @Test
    fun suggestingAnExistingTagNameRecordsASuggestionAndAcceptingStaysPerItem() = run {
        val pc = inventory.addTag("PC")
        val a = inventory.addItem("laptop")
        val b = inventory.addItem("tower")

        assertEquals(pc, inventory.suggestTag(a, "pc"))
        assertEquals(pc, inventory.suggestTag(b, "PC"))
        assertTrue(inventory.isSuggested(a, pc))
        assertTrue(inventory.isSuggested(b, pc))
        assertEquals(1, inventory.tags().size)

        inventory.acceptSuggestion(a, pc)

        assertFalse(inventory.isSuggested(a, pc))
        assertTrue(inventory.isSuggested(b, pc))
    }

    @Test
    fun untaggingASuggestedLinkLeavesNothingBehind() = run {
        val item = inventory.addItem("laptop")
        val pc = inventory.suggestTag(item, "PC")
        inventory.untag(listOf(item), pc)
        assertTrue(inventory.tagsOf(item).isEmpty())
        assertFalse(inventory.isSuggested(item, pc))
        assertEquals(0, db.itemTagDao().allLinks().size)
    }

    /** One row per pair, on every device and after an untag: the link's id comes from the pair. */
    @Test
    fun oneLinkRowPerPairEvenAfterUntagAndTagAgain() = run {
        val item = inventory.addItem("kettle")
        val tag = inventory.addTag("kitchen")
        inventory.tag(listOf(item), tag)
        val id = db.itemTagDao().linksOfItem(item).single().id
        assertEquals(id, linkId(item, tag)) // what another device computes for the same pair

        inventory.untag(listOf(item), tag)
        assertTrue(id in db.syncDao().purges().map { it.id })
        inventory.tag(listOf(item), tag)
        inventory.tag(listOf(item), tag)

        assertEquals(listOf(id), db.itemTagDao().allLinks().map { it.id })
        assertFalse(id in db.syncDao().purges().map { it.id }, "the purge of the earlier link must go")
    }

    @Test
    fun nameBasedIdMatchesTheKnownVersion5Value() {
        // RFC 4122 namespace DNS, name "python.org".
        assertEquals("886313e1-3b8a-5372-9b90-0c9aee199e5d", uuidV5("6ba7b810-9dad-11d1-80b4-00c04fd430c8", "python.org"))
    }

    /** A peer's first edit of a default kind is no clash: nothing is kept as lost. */
    @Test
    fun peerRenameOfADefaultKindMergesWithoutALostVersion() = run {
        val theirs = Row(TAG_KIND, "kind-function", mapOf(
            NAME to Group(Stamp(5_000, "device-b"), mapOf("name" to "purpose")),
            ORDER to Group(NEVER_WRITTEN, mapOf("position" to 0L)),
        ))
        val writes = LocalWrites(db, clock)
        writes.write(mapOf(TAG_KIND to listOf("kind-function"))) { store ->
            writes.merger.import(store, listOf(theirs), emptyMap())
        }
        assertEquals("purpose", inventory.kinds().first().name)
        assertTrue(db.syncDao().lost().isEmpty())
    }

    @Test
    fun readsMapAMissingParentOrKindToNull() = run {
        val tag = inventory.addTag("blue")
        val writes = LocalWrites(db, clock)
        writes.edit(TAG, tag, PARENT) { mapOf("parentId" to "no-such-tag") }
        writes.edit(TAG, tag, KIND) { mapOf("kind" to "no-such-kind") }

        assertNull(inventory.getTag(tag)?.parentId)
        assertNull(inventory.getTag(tag)?.kind)
        assertNull(inventory.tags().single().parentId)
    }

    @Test
    fun keptTogetherAndHomeAreGroupsOfTheirOwn() = run {
        val tag = inventory.addTag("tools")
        val row = db.syncedTables().getValue(TAG).load(listOf(tag)).single()
        assertEquals(setOf(NAME, PARENT, KIND, KEPT_TOGETHER, HOME, MODULE), row.groups.keys)
        assertEquals(setOf("kept"), row.groups.getValue(KEPT_TOGETHER).values.keys)
        assertEquals(setOf("homePlaceId"), row.groups.getValue(HOME).values.keys)
        assertTrue(HOME in KEEP_LOSER_GROUPS && KEPT_TOGETHER in KEEP_LOSER_GROUPS)
    }

    @Test
    fun tagNamesAreTrimmed() = run {
        val tag = inventory.addTag("  sewing  ")
        assertEquals("sewing", inventory.getTag(tag)?.name)
    }

    @Test
    fun approximateQuantityNeedsACount() = run {
        val id = inventory.addItem("screws")
        assertFailsWith<IllegalArgumentException> { inventory.setQuantity(id, null, approx = true) }
        inventory.setQuantity(id, 50, approx = true)
        assertEquals(50L, inventory.getItem(id)?.count)
        assertTrue(inventory.getItem(id)?.approx == true)
    }
}
