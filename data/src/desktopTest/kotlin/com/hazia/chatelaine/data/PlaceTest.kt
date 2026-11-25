package com.hazia.chatelaine.data

import com.hazia.chatelaine.core.sync.HybridClock
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaceTest {

    private val dir: File = Files.createTempDirectory("chatelaine-places").toFile().also { it.deleteOnExit() }
    private val open = openChatelaineDatabase(File(dir, DATABASE_NAME))
    private val db = open.database
    private var now = 1_000L
    private val clock = HybridClock("device-a", { now++ })
    private val writes = LocalWrites(db, clock)
    private val inventory = Inventory(db, writes)

    @AfterTest
    fun close() = db.close()

    private fun <T> run(block: suspend () -> T): T = runBlocking { block() }

    /** R32: house > floor > bedroom > chest > drawer > partition. */
    @Test
    fun placesNestToAnyDepth() = run {
        val names = listOf("house", "floor", "bedroom", "chest", "drawer", "partition")
        var parent: String? = null
        var last = ""
        for (name in names) {
            last = inventory.addPlace(name, parent)
            parent = last
        }
        assertEquals(names, inventory.path(last))
        assertEquals(6, inventory.path(last).size)
    }

    @Test
    fun placeCannotMoveIntoItsDescendant() = run {
        val house = inventory.addPlace("house")
        val floor = inventory.addPlace("floor", house)
        val drawer = inventory.addPlace("drawer", floor)
        assertFailsWith<IllegalArgumentException> { inventory.movePlace(house, drawer) }
        assertFailsWith<IllegalArgumentException> { inventory.movePlace(house, house) }
        assertNull(inventory.getPlace(house)?.parentId)
        inventory.movePlace(drawer, house)
        assertEquals(house, inventory.getPlace(drawer)?.parentId)
    }

    /** R111: an item is listed under its box; an item is never a place. */
    @Test
    fun itemInBoxIsListedUnderTheBox() = run {
        val box = inventory.addPlace("box")
        val clip = inventory.addItem("paper clip")
        assertEquals(ItemState.UNPLACED, inventory.state(clip))

        inventory.placeItem(clip, box)

        assertEquals(listOf(clip), inventory.contents(box).items.map { it.id })
        assertEquals(ItemState.PLACED, inventory.state(clip))
        val other = inventory.addItem("stapler")
        assertFailsWith<IllegalArgumentException> { inventory.placeItem(clip, other) }
        assertFailsWith<IllegalArgumentException> { inventory.placeItem(clip, "no-such-place") }
        assertFailsWith<IllegalArgumentException> { inventory.addPlace("drawer", other) }
        assertFailsWith<IllegalArgumentException> { inventory.movePlace(box, other) }
        assertEquals(box, inventory.getItem(clip)?.placeId)

        inventory.placeItem(clip, null)
        assertEquals(ItemState.UNPLACED, inventory.state(clip))
        assertTrue(inventory.contents(box).items.isEmpty())
    }

    /** R31: moving an item into or out of a place raises the flag on the item and on the place. Any place counts as having a photo until Task 8. */
    @Test
    fun movingIntoOrOutOfPlaceRaisesOutdated() = run {
        val a = inventory.addPlace("box a")
        val b = inventory.addPlace("box b")
        val clip = inventory.addItem("paper clip")
        assertFalse(inventory.getItem(clip)!!.photoOutdated)
        assertFalse(inventory.getPlace(a)!!.photoOutdated)

        inventory.placeItem(clip, a)
        assertTrue(inventory.getItem(clip)!!.photoOutdated)
        assertTrue(inventory.getPlace(a)!!.photoOutdated)
        assertFalse(inventory.getPlace(b)!!.photoOutdated)

        // A new photo clears both (Task 8); here the flags are cleared by hand, then the item moves out of a and into b.
        writes.edit(ITEM, clip, PHOTO_OUTDATED) { mapOf("outdated" to false) }
        writes.edit(PLACE_TABLE, a, PHOTO_OUTDATED) { mapOf("outdated" to false) }
        inventory.placeItem(clip, b)
        assertTrue(inventory.getPlace(a)!!.photoOutdated, "moving out raises the old place")
        assertTrue(inventory.getPlace(b)!!.photoOutdated, "moving in raises the new place")
        assertTrue(inventory.getItem(clip)!!.photoOutdated)
    }

    /** R152: the drawer's items and partitions go up to the chest. */
    @Test
    fun deletingPlaceMovesContentsUp() = run {
        val chest = inventory.addPlace("chest")
        val drawer = inventory.addPlace("drawer", chest)
        val partition = inventory.addPlace("partition", drawer)
        val socks = inventory.addItem("socks")
        inventory.placeItem(socks, drawer)
        val tag = inventory.addTag("bedroom")
        inventory.tagPlace(drawer, tag)
        val link = db.placeTagDao().linksOfPlace(drawer).single().id
        assertTrue(link != linkId(drawer, tag), "a place link never takes an item link's id")

        inventory.deletePlace(drawer)

        assertNull(inventory.getPlace(drawer))
        assertEquals(chest, inventory.getItem(socks)?.placeId)
        assertEquals(chest, inventory.getPlace(partition)?.parentId)
        assertEquals(listOf(socks), inventory.contents(chest).items.map { it.id })
        assertEquals(listOf(partition), inventory.contents(chest).places.map { it.id })
        val purged = db.syncDao().purges().map { it.id }
        assertTrue(drawer in purged)
        assertTrue(link in purged)
        assertTrue(db.placeTagDao().allLinks().isEmpty())
    }

    /** R152: a site's items become unplaced and its places stay, with no parent. */
    @Test
    fun deletingSiteLeavesItemsUnplacedKeepsPlaces() = run {
        val site = inventory.addPlace("flat", isSite = true)
        val room = inventory.addPlace("kitchen", site)
        val kettle = inventory.addItem("kettle")
        inventory.placeItem(kettle, site)

        inventory.deletePlace(site)

        assertNull(inventory.getPlace(site))
        assertEquals(ItemState.UNPLACED, inventory.state(kettle))
        assertNull(inventory.getItem(kettle)?.placeId)
        assertNotNull(inventory.getPlace(room))
        assertNull(inventory.getPlace(room)?.parentId)
    }

    @Test
    fun deletingPlaceWithThousandItemsIsOneTransaction() = run {
        val chest = inventory.addPlace("chest")
        val drawer = inventory.addPlace("drawer", chest)
        val ids = (1..1000).map { inventory.addItem("item $it") }
        inventory.moveItems(ids, drawer)
        assertEquals(1000, inventory.contents(drawer).items.size)
        db.syncDao().clearOutbox(Long.MAX_VALUE)

        inventory.deletePlace(drawer)

        assertEquals(1000, inventory.contents(chest).items.size)
        assertTrue(ids.all { inventory.getItem(it)?.placeId == chest })
        // 1,000 items, the chest (its photo flag), the drawer's row and the drawer's purge: one batch.
        assertEquals(1003, db.syncDao().outbox().size)
        assertTrue(drawer in db.syncDao().purges().map { it.id })
    }

    /** R36: what carries the tag, or a sub-tag, and is not in the home or one of its places is away. */
    @Test
    fun keptTogetherTagListsItemsAwayFromHome() = run {
        val winter = inventory.addTag("winter")
        val wool = inventory.addTag("wool", winter)
        val box = inventory.addPlace("storage box")
        val partition = inventory.addPlace("partition", box)
        val closet = inventory.addPlace("closet")
        val blanket = inventory.addItem("blanket")
        val coat = inventory.addItem("coat")
        val scarf = inventory.addItem("scarf")
        val gloves = inventory.addItem("gloves")
        inventory.placeItem(blanket, partition)
        inventory.placeItem(coat, closet)
        inventory.placeItem(scarf, closet)
        inventory.tag(listOf(blanket, coat), winter)
        inventory.tag(listOf(scarf), wool)

        assertTrue(inventory.awayFromHome(winter).isEmpty(), "not kept together: no home to be away from")
        assertFailsWith<IllegalArgumentException> { inventory.setKeptTogether(winter, false, box) }
        assertFailsWith<IllegalArgumentException> { inventory.setKeptTogether(winter, true, blanket) }
        inventory.setKeptTogether(winter, true, box)

        assertEquals(true, inventory.getTag(winter)?.kept)
        assertEquals(box, inventory.getTag(winter)?.homePlaceId)
        assertEquals(listOf(coat, scarf).sorted(), inventory.awayFromHome(winter))
        assertFalse(gloves in inventory.awayFromHome(winter))
    }

    /** Two devices can each make half of a cycle; every walk ends anyway. */
    @Test
    fun cycleFromTwoDevicesCannotLoopTheWalks() = run {
        val a = inventory.addPlace("a")
        val b = inventory.addPlace("b", a)
        writes.edit(PLACE_TABLE, a, PARENT) { mapOf("parentId" to b) } // the other device's move
        inventory.path(b)
        inventory.contents(a)

        val t1 = inventory.addTag("t1")
        val t2 = inventory.addTag("t2", t1)
        writes.edit(TAG, t1, PARENT) { mapOf("parentId" to t2) }
        inventory.setKeptTogether(t1, true, a)
        inventory.awayFromHome(t1)
        assertTrue(inventory.awayFromHome(t2).isEmpty())
    }
}
