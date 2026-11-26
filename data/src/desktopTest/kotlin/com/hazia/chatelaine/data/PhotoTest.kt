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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhotoTest {

    private val dir: File = Files.createTempDirectory("chatelaine-photos").toFile().also { it.deleteOnExit() }
    private val open = openChatelaineDatabase(File(dir, DATABASE_NAME))
    private val db = open.database
    private var now = 1_000L
    private val clock = HybridClock("device-a", { now++ })
    private val writes = LocalWrites(db, clock)
    private val inventory = Inventory(db, writes)
    private val fields = Fields(db, writes)
    private val hash = "a".repeat(64)

    @AfterTest
    fun close() = db.close()

    private fun <T> run(block: suspend () -> T): T = runBlocking { block() }

    /** R41: a photo is on an item or on a place, never both and never neither. */
    @Test
    fun photoBelongsToItemOrPlaceNotBoth() = run {
        val item = inventory.addItem("lamp")
        val place = inventory.addPlace("shelf")
        assertFailsWith<IllegalArgumentException> { inventory.addPhoto(item, place, hash) }
        assertFailsWith<IllegalArgumentException> { inventory.addPhoto(null, null, hash) }
        assertFailsWith<IllegalArgumentException> { inventory.addPhoto("no-such-item", null, hash) }
        assertFailsWith<IllegalArgumentException> { inventory.addPhoto(null, "no-such-place", hash) }
        assertTrue(db.photoDao().allPhotos().isEmpty())

        val onItem = inventory.addPhoto(item, null, hash)
        val onPlace = inventory.addPhoto(null, place, hash)
        assertEquals(item, db.photoDao().photo(onItem)?.itemId)
        assertNull(db.photoDao().photo(onItem)?.placeId)
        assertEquals(place, db.photoDao().photo(onPlace)?.placeId)
        assertNull(db.photoDao().photo(onPlace)?.itemId)
        assertEquals(hash, db.photoDao().photo(onItem)?.hash)
    }

    /** R56: a photo points to its content by a 64-digit lower-case hex hash. */
    @Test
    fun hashMustBeSixtyFourHexDigits() = run {
        val item = inventory.addItem("lamp")
        for (bad in listOf("", "a".repeat(63), "a".repeat(65), "A".repeat(64), "g".repeat(64), "a".repeat(63) + "\n")) {
            assertFailsWith<IllegalArgumentException>(bad) { inventory.addPhoto(item, null, bad) }
        }
        assertTrue(inventory.addPhoto(item, null, "0123456789abcdef".repeat(4)).isNotEmpty())
    }

    /** R41: a region on a place photo opens the item it points to. */
    @Test
    fun regionOpensItsItem() = run {
        val place = inventory.addPlace("shelf")
        val item = inventory.addItem("lamp")
        val photo = inventory.addPhoto(null, place, hash)
        inventory.addRegion(photo, 0.1, 0.1, 0.2, 0.2, item)

        assertEquals(item, inventory.itemAt(photo, 0.15, 0.15))
        assertEquals(item, inventory.itemAt(photo, 0.1, 0.3))
        assertNull(inventory.itemAt(photo, 0.9, 0.9))
        assertNull(inventory.itemAt(photo, 0.31, 0.15))
        assertNull(inventory.itemAt("no-such-photo", 0.15, 0.15))
    }

    @Test
    fun overlappingRegionsGiveTheSmallest() = run {
        val place = inventory.addPlace("shelf")
        val big = inventory.addItem("box")
        val small = inventory.addItem("key")
        val photo = inventory.addPhoto(null, place, hash)
        inventory.addRegion(photo, 0.0, 0.0, 0.8, 0.8, big)
        inventory.addRegion(photo, 0.2, 0.2, 0.1, 0.1, small)

        assertEquals(small, inventory.itemAt(photo, 0.25, 0.25))
        assertEquals(big, inventory.itemAt(photo, 0.5, 0.5))
    }

    @Test
    fun regionAreaIsAFractionOfThePhoto() = run {
        val place = inventory.addPlace("shelf")
        val item = inventory.addItem("lamp")
        val photo = inventory.addPhoto(null, place, hash)
        val bad = listOf(
            listOf(-0.1, 0.1, 0.2, 0.2), listOf(0.1, -0.1, 0.2, 0.2),
            listOf(0.1, 0.1, 0.0, 0.2), listOf(0.1, 0.1, 0.2, 0.0), listOf(0.1, 0.1, -0.2, 0.2),
            listOf(0.9, 0.1, 0.2, 0.2), listOf(0.1, 0.9, 0.2, 0.2),
            listOf(Double.NaN, 0.1, 0.2, 0.2), listOf(0.1, 0.1, Double.POSITIVE_INFINITY, 0.2),
        )
        for ((x, y, w, h) in bad) {
            assertFailsWith<IllegalArgumentException>("$x $y $w $h") { inventory.addRegion(photo, x, y, w, h, item) }
        }
        assertFailsWith<IllegalArgumentException> { inventory.addRegion("no-such-photo", 0.1, 0.1, 0.2, 0.2, item) }
        assertFailsWith<IllegalArgumentException> { inventory.addRegion(photo, 0.1, 0.1, 0.2, 0.2, "no-such-item") }
        assertTrue(db.photoRegionDao().allRegions().isEmpty())
        assertTrue(inventory.addRegion(photo, 0.0, 0.0, 1.0, 1.0, item).isNotEmpty())
    }

    /** R31: an edit raises the flag, a new photo clears it. */
    @Test
    fun newPhotoClearsOutdatedFlag() = run {
        val box = inventory.addPlace("box")
        val clip = inventory.addItem("paper clip")
        inventory.addPhoto(clip, null, hash)
        inventory.addPhoto(null, box, hash)

        inventory.placeItem(clip, box)
        assertTrue(inventory.getItem(clip)!!.photoOutdated)
        assertTrue(inventory.getPlace(box)!!.photoOutdated)

        inventory.addPhoto(clip, null, "b".repeat(64))
        assertFalse(inventory.getItem(clip)!!.photoOutdated)
        assertTrue(inventory.getPlace(box)!!.photoOutdated, "the place's own photo is the one that clears it")
        inventory.addPhoto(null, box, "c".repeat(64))
        assertFalse(inventory.getPlace(box)!!.photoOutdated)
    }

    /** R31: editing an item after its last photo raises the flag, here by renaming it; an item with no photo keeps it down. */
    @Test
    fun renamingAPhotographedItemRaisesOutdated() = run {
        val clip = inventory.addItem("paper clip")
        val pin = inventory.addItem("pin")
        inventory.addPhoto(clip, null, hash)

        inventory.rename(clip, "big paper clip")
        inventory.rename(pin, "long pin")

        assertTrue(inventory.getItem(clip)!!.photoOutdated)
        assertFalse(inventory.getItem(pin)!!.photoOutdated, "the item has no photo")
    }

    /** R31: a field value set on a photographed item is an edit of it. */
    @Test
    fun aFieldValueOnAPhotographedItemRaisesOutdated() = run {
        val clip = inventory.addItem("paper clip")
        inventory.addPhoto(clip, null, hash)
        val size = fields.define("size", FieldType.TEXT)

        fields.set(clip, size, FieldInput.Text("big"))

        assertTrue(inventory.getItem(clip)!!.photoOutdated)
    }

    /** R31: the item's other edits raise it too: notes, quantity, taking it out, splitting it. */
    @Test
    fun everyEditOfAPhotographedItemRaisesOutdated() = run {
        val clip = inventory.addItem("paper clip")
        val edits = listOf<suspend () -> Unit>(
            { inventory.setNotes(clip, "the blue one") },
            { inventory.setQuantity(clip, 2, approx = false) },
            { inventory.setAway(clip, true) },
            { inventory.splitItem(clip, 2) },
        )
        for ((i, edit) in edits.withIndex()) {
            inventory.addPhoto(clip, null, "${i + 1}".repeat(64))
            assertFalse(inventory.getItem(clip)!!.photoOutdated)
            edit()
            assertTrue(inventory.getItem(clip)!!.photoOutdated, "edit $i")
        }
    }

    /** Only a photographed item or place can be out of date. */
    @Test
    fun noPhotoNoFlag() = run {
        val box = inventory.addPlace("box")
        val clip = inventory.addItem("paper clip")
        inventory.addPhoto(null, box, hash)
        inventory.placeItem(clip, box)
        assertFalse(inventory.getItem(clip)!!.photoOutdated, "the item has no photo")
        assertTrue(inventory.getPlace(box)!!.photoOutdated)

        inventory.placeItem(clip, null)
        assertFalse(inventory.getItem(clip)!!.photoOutdated)
    }

    /** R152: an item's photos and their regions go with it. */
    @Test
    fun deletingItemDeletesItsPhotos() = run {
        val place = inventory.addPlace("shelf")
        val lamp = inventory.addItem("lamp")
        val other = inventory.addItem("vase")
        val own = inventory.addPhoto(lamp, null, hash)
        val ownRegion = inventory.addRegion(own, 0.1, 0.1, 0.2, 0.2, other)
        val kept = inventory.addPhoto(other, null, hash)
        val shelf = inventory.addPhoto(null, place, hash)
        val pointing = inventory.addRegion(shelf, 0.5, 0.5, 0.2, 0.2, lamp)
        val staying = inventory.addRegion(shelf, 0.1, 0.1, 0.2, 0.2, other)

        inventory.deleteItem(lamp)

        assertEquals(listOf(kept, shelf).sorted(), db.photoDao().allPhotos().map { it.id }.sorted())
        assertEquals(listOf(staying), db.photoRegionDao().allRegions().map { it.id })
        val purged = db.syncDao().purges().map { it.id }
        assertTrue(own in purged)
        assertTrue(ownRegion in purged)
        assertTrue(pointing in purged, "a region whose item is gone is purged")
        assertFalse(staying in purged)
        assertNull(inventory.itemAt(shelf, 0.55, 0.55))
        assertEquals(other, inventory.itemAt(shelf, 0.15, 0.15))
    }

    /** A place's photos belong to the place. */
    @Test
    fun deletingPlaceDeletesItsPhotos() = run {
        val place = inventory.addPlace("shelf")
        val lamp = inventory.addItem("lamp")
        val photo = inventory.addPhoto(null, place, hash)
        val region = inventory.addRegion(photo, 0.1, 0.1, 0.2, 0.2, lamp)

        inventory.deletePlace(place)

        assertTrue(db.photoDao().allPhotos().isEmpty())
        assertTrue(db.photoRegionDao().allRegions().isEmpty())
        val purged = db.syncDao().purges().map { it.id }
        assertTrue(photo in purged)
        assertTrue(region in purged)
        assertEquals(lamp, inventory.getItem(lamp)?.id)
    }
}
