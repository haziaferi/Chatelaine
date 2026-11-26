package com.hazia.chatelaine.data.sync

import androidx.sqlite.SQLiteException
import com.hazia.chatelaine.core.sync.Group
import com.hazia.chatelaine.core.sync.Row
import com.hazia.chatelaine.core.sync.Stamp
import com.hazia.chatelaine.data.AREA
import com.hazia.chatelaine.data.BLOB
import com.hazia.chatelaine.data.FIELD_ATTACH
import com.hazia.chatelaine.data.FIELD_DEF
import com.hazia.chatelaine.data.FIELD_VALUE
import com.hazia.chatelaine.data.FieldInput
import com.hazia.chatelaine.data.FieldType
import com.hazia.chatelaine.data.ITEM
import com.hazia.chatelaine.data.ITEM_TAG
import com.hazia.chatelaine.data.NAME
import com.hazia.chatelaine.data.NOTES
import com.hazia.chatelaine.data.OWNER
import com.hazia.chatelaine.data.PHOTO
import com.hazia.chatelaine.data.PHOTO_REGION
import com.hazia.chatelaine.data.PLACE_TAG
import com.hazia.chatelaine.data.TAKEN
import com.hazia.chatelaine.data.TARGET
import com.hazia.chatelaine.data.linkId
import com.hazia.chatelaine.data.openChatelaineDatabase
import com.hazia.chatelaine.data.syncedTables
import com.hazia.chatelaine.core.sync.HybridClock
import com.hazia.chatelaine.data.Inventory
import com.hazia.chatelaine.data.LocalWrites
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The folder sync through the real importer, exporter and tables, between two or three devices that share a folder
 * through a test double of Syncthing (R51, R53, R54, R55, R57, R58, R59).
 */
class FolderSyncTest {

    @get:Rule val tmp = TemporaryFolder()

    private val worlds = mutableListOf<World>()

    private fun world(seed: Int = 1, segmentBytes: Int = 16 * 1024, snapshotEvery: Int = 64) =
        World(tmp.root, seed, segmentBytes, snapshotEvery).also { worlds += it }

    @After fun close() = worlds.forEach { it.close() }

    private val hour = 3_600_000L
    private val day = 24 * hour
    private val hash = "a".repeat(64)

    private fun World.Device.waiting() = runBlocking { db.syncDao().waiting() }

    private fun World.Device.names() = runBlocking { inventory.items() }.map { it.name }

    /** One device sends another everything it has exported so far. */
    private fun World.meet(from: World.Device, to: World.Device) {
        from.export()
        syncthing.session(from.name, to.name)
    }

    /**
     * [days] of a phone and a laptop editing while online and offline: every [stepHours] a device is
     * online with some chance, and two online devices meet in a session that is cut 5% of the time.
     */
    private fun ordinaryUse(w: World, days: Int, editsPerStep: Int, random: Random, stepHours: Int = 1): List<World.Device> {
        val devices = listOf(w.Device("phone"), w.Device("laptop"))
        for (step in 0 until days * 24 / stepHours) {
            w.syncthing.now = step * stepHours * hour
            val online = devices.filter { random.nextDouble() < 0.6 }
            for (d in devices) repeat(random.nextInt(editsPerStep + 1)) {
                val rows = d.rows().keys.toList()
                if (rows.isEmpty() || random.nextDouble() < 0.1) d.create() else d.edit(rows.random(random), GROUPS.random(random))
            }
            online.forEach { it.import() }
            online.forEach { it.export() }
            if (online.size == 2) w.syncthing.session("phone", "laptop", cut = random.nextDouble() < 0.05)
        }
        return devices
    }

    // ---- the plan's eleven (R51, R53, R54, R55, R57, R59) ----

    /** R51 */
    @Test
    fun itemAddedOnAAppearsOnB() {
        val w = world()
        val a = w.Device("A")
        val b = w.Device("B")
        a.create("scale remover")
        w.meet(a, b)

        b.import()

        assertEquals(listOf("scale remover"), b.names())
    }

    /** R54: the later edit wins, and the overwritten value is in history on both devices. */
    @Test
    fun sameFieldEditedOnBothLaterWinsLoserKept() {
        val w = world()
        val a = w.Device("A")
        val b = w.Device("B")
        val item = a.create("lamp")
        w.meet(a, b)
        b.import()

        w.syncthing.now = 1_000
        a.edit(item, NAME, "A's name")
        w.syncthing.now = 2_000
        b.edit(item, NAME, "B's name")
        w.settle(listOf(a, b))

        for (d in listOf(a, b)) {
            assertEquals(listOf("B's name"), d.names(), d.name)
            val history = runBlocking { d.history.overwritten(ITEM, item, NAME) }.map { it.values["name"] }
            assertTrue("A's name" in history, "${d.name} keeps A's name in history: $history")
        }
    }

    /** R55: two devices edit different fields of one item at once; both edits stay. */
    @Test
    fun differentFieldsOnBothBothSurvive() {
        val w = world()
        val a = w.Device("A")
        val b = w.Device("B")
        val item = a.create("lamp")
        w.meet(a, b)
        b.import()

        w.syncthing.now = 1_000
        a.edit(item, NOTES, "brass")
        w.syncthing.now = 1_001
        runBlocking { b.inventory.setQuantity(item, 3, approx = false) }
        w.settle(listOf(a, b))

        for (d in listOf(a, b)) {
            val held = runBlocking { d.inventory.getItem(item) }
            assertEquals("brass", held?.notes, d.name)
            assertEquals(3L, held?.count, d.name)
        }
    }

    /** A link that arrives before the tag and the item it joins waits for them, then applies. */
    @Test
    fun childBeforeParentWaitsThenApplies() {
        val w = world()
        val a = w.Device("A")
        val item = a.create("lamp")
        val tag = runBlocking { a.inventory.addTag("light") }
        runBlocking { a.inventory.tag(listOf(item), tag) }
        a.export()
        val log = FolderLayout.segment("A", 0)
        val link = w.plainLines("A", log).single { "\"t\":\"$ITEM_TAG\"" in it }
        val parents = w.plainLines("A", log).filter { "\"t\":\"$ITEM_TAG\"" !in it }

        val b = w.Device("B")
        val folder = w.syncthing.folder("B")
        folder.replace(FolderLayout.segment("X", 0), w.sealed(link).encodeToByteArray())
        b.import()
        assertEquals(1, b.waiting().size, "the link waits for its item and tag")
        assertTrue(b.rows(ITEM_TAG).isEmpty())

        folder.replace(FolderLayout.segment("Y", 0), parents.joinToString("") { w.sealed(it) }.encodeToByteArray())
        b.import()
        assertEquals(1, b.rows(ITEM_TAG).size)
        assertEquals(emptyList(), b.waiting())
    }

    /**
     * An item's place is an id that names no foreign key (a peer's item may arrive before its place), so such an item
     * does not wait: it applies at once and reads as unplaced until the place is here.
     */
    @Test
    fun anItemPlacedInAPlaceNotYetHereIsNotHeldBackAndIsPlacedWhenItArrives() {
        val w = world()
        val a = w.Device("A")
        val item = a.create("lamp")
        val place = runBlocking { a.inventory.addPlace("shelf") }
        runBlocking { a.inventory.placeItem(item, place) }
        a.export()
        val lines = w.plainLines("A", FolderLayout.segment("A", 0))
        val itemLine = lines.single { "\"id\":\"$item\"" in it }
        val placeLine = lines.single { "\"id\":\"$place\"" in it }

        val b = w.Device("B")
        val folder = w.syncthing.folder("B")
        folder.replace(FolderLayout.segment("X", 0), w.sealed(itemLine).encodeToByteArray())
        val report = b.import()
        assertEquals(ImportReport(changed = 1, skipped = 0, waiting = 0), report)
        assertEquals(emptyList(), b.waiting())
        assertNull(runBlocking { b.inventory.getItem(item) }?.placeId, "the place is not here: the item reads as unplaced")

        folder.replace(FolderLayout.segment("Y", 0), w.sealed(placeLine).encodeToByteArray())
        b.import()
        assertEquals(place, runBlocking { b.inventory.getItem(item) }?.placeId)
    }

    /** R57: a copy a tool leaves beside the log is read, then deleted. */
    @Test
    fun conflictCopyIsMergedAndDeleted() {
        val w = world()
        val c = w.Device("C")
        val item = c.create("kettle")
        c.export()
        val text = w.syncthing.folder("C").read(FolderLayout.segment("C", 0))!!
        val b = w.Device("B")
        val folder = w.syncthing.folder("B")
        val copy = "${FolderLayout.dir("C")}/log-000001.sync-conflict-x.jsonl"
        folder.replace(copy, text)

        val report = b.import()

        assertEquals(1, report.changed)
        assertEquals(setOf(item), b.rows().keys)
        assertTrue(ConflictCopies.isCopy("log-000001.sync-conflict-x.jsonl"))
        assertFalse(copy.substringAfterLast('/') in folder.list(FolderLayout.dir("C")), "the copy is deleted")
    }

    /** A temporary file a crashed export left behind is never read. */
    @Test
    fun tempFilesAreNeverRead() {
        val w = world()
        val a = w.Device("A")
        a.create("kettle")
        a.export()
        val text = w.syncthing.folder("A").read(FolderLayout.segment("A", 0))!!
        val b = w.Device("B")
        w.syncthing.folder("B").replace("${FolderLayout.dir("A")}/${TEMP_PREFIX}log-000000.jsonl", text)

        val report = b.import()

        assertEquals(ImportReport(changed = 0, skipped = 0), report)
        assertTrue(b.rows().isEmpty())
    }

    /** A line stamped two days ahead of this device's clock is set aside, not applied, and read again later. */
    @Test
    fun lineFromTheFarFutureIsHeldBack() {
        val w = world()
        w.syncthing.now = 10 * day
        val a = w.Device("A")
        a.skew = 2 * day
        a.create("kettle")
        a.export()
        a.skew = 0
        val b = w.Device("B")
        w.syncthing.session("A", "B")

        val report = b.import()

        assertEquals(0, report.changed)
        assertEquals(1, report.waiting, "held back, counted as waiting, not as skipped")
        assertTrue(b.rows().isEmpty())

        // Two days on, the line is no longer ahead: the next import applies it.
        w.syncthing.now = 12 * day + hour
        assertEquals(1, b.import().changed)
        assertEquals(1, b.rows().size)
    }

    /** Segments roll at 16 KiB; a snapshot replaces them when 64 are held, and no log before it is kept. */
    @Test
    fun segmentsRollAt16KiBAndSnapshotEvery64() {
        val w = world()
        val a = w.Device("A")
        val dir = FolderLayout.dir("A")
        val folder = w.syncthing.folder("A")
        fun segments() = folder.list(dir).filter { FolderLayout.segmentSeq(it) != null }.sorted()
        fun snapshots() = folder.list(dir).filter { FolderLayout.snapshotSeq(it) != null }

        var made = 0
        var before: List<Long>
        do {
            before = segments().map { folder.size("$dir/$it")!! }
            repeat(100) { runBlocking { a.inventory.addItem("item ${made++}") } }
            a.export()
        } while (snapshots().isEmpty())

        assertTrue(before.size in 1..63, "the segments written before the snapshot: ${before.size}")
        assertTrue(before.all { it <= 16 * 1024 }, "every segment is within 16 KiB: $before")
        assertEquals(1, snapshots().size)
        val seq = FolderLayout.snapshotSeq(snapshots().single())!!
        assertTrue(seq >= 63, "the snapshot closes the 64th segment at the earliest: $seq")
        assertEquals(emptyList(), segments(), "logs before the snapshot are compacted away")

        // The log goes on after the snapshot, and a device that never saw the segments gets everything from it.
        a.create("one more")
        a.export()
        assertEquals(listOf(FolderLayout.segment("A", seq + 1).substringAfterLast('/')), segments())
        val c = w.Device("C")
        w.syncthing.session("A", "C")
        c.import()
        assertEquals(made + 1, c.rows().size)
    }

    /** R53: no database file, and none of its side files, is ever in the folder. */
    @Test
    fun noDatabaseFileIsEverInTheFolder() {
        val w = world()
        val a = w.Device("A")
        val b = w.Device("B")
        a.create("kettle")
        b.create("spoon")
        w.settle(listOf(a, b))
        val all = w.syncthing.paths("A") + w.syncthing.paths("B")
        assertTrue(all.isNotEmpty())
        assertTrue(all.none { it.endsWith(".db") || it.endsWith("-wal") || it.endsWith("-shm") || it.endsWith("-journal") }, "$all")

        // The same through a real directory.
        val shared = File(tmp.root, "shared").also { it.mkdirs() }
        val db = openChatelaineDatabase(File(tmp.root, "real.db")).database
        try {
            val clock = HybridClock("R", System::currentTimeMillis)
            val inventory = Inventory(db, LocalWrites(db, clock))
            val sync = FolderSync(db, DirectorySyncFolder(shared.toPath()), "R", clock, db.syncedTables(), PlainLines, System::currentTimeMillis)
            runBlocking {
                inventory.addItem("kettle")
                sync.export()
            }
            val files = shared.walkTopDown().filter { it.isFile }.map { it.name }.toList()
            assertTrue(files.any { it.startsWith("log-") }, "$files")
            assertTrue(files.none { it.endsWith(".db") || it.endsWith("-wal") || it.endsWith("-shm") || it.endsWith("-journal") }, "$files")
        } finally {
            db.close()
        }
    }

    /** R59: a database made anew, because the old one was corrupt, reads its own files in the folder back. */
    @Test
    fun recoveredDatabaseRereadsItsOwnFolderFiles() {
        val w = world()
        val a = w.Device("A")
        val ids = (1..3).map { a.create("item $it") }.toSet()
        a.export()
        a.db.close()
        File(tmp.root, "A.db").writeBytes(Random(7).nextBytes(4096))

        val again = w.Device("A")

        assertTrue(again.rows().isEmpty(), "the new database starts empty")
        assertTrue(again.sync.readingOwnLog)
        again.import()
        assertEquals(ids, again.rows().keys)
        assertFalse(again.sync.readingOwnLog)
    }

    /** R59: the outbox, not a stamp cursor, says what to export; exporting empties it. */
    @Test
    fun exportReadsTheOutbox() {
        val w = world()
        val a = w.Device("A")
        repeat(3) { a.create("item $it") }
        assertEquals(3, runBlocking { a.db.syncDao().outbox() }.size)

        a.export()

        assertEquals(emptyList(), runBlocking { a.db.syncDao().outbox() })
        assertEquals(3, w.plainLines("A", FolderLayout.segment("A", 0)).size)
    }

    // ---- carried from the earlier reviews ----

    /** a. Two devices each move a tag under the other's: the cycle is repaired the same way on both. */
    @Test
    fun aTagCycleTwoDevicesMadeTogetherIsBrokenTheSameOnBoth() {
        val w = world()
        val a = w.Device("A")
        val b = w.Device("B")
        val one = runBlocking { a.inventory.addTag("one") }
        val two = runBlocking { a.inventory.addTag("two") }
        w.settle(listOf(a, b))

        w.syncthing.now = 1_000
        runBlocking { a.inventory.setParent(one, two) }
        w.syncthing.now = 2_000
        runBlocking { b.inventory.setParent(two, one) }
        w.settle(listOf(a, b))

        // The newer parent edit (two under one) is the one reset.
        for (d in listOf(a, b)) {
            val parents = runBlocking { d.inventory.tags() }.associate { it.id to it.parentId }
            assertEquals(two, parents[one], d.name)
            assertNull(parents[two], d.name)
        }
    }

    /** a. The same for places. */
    @Test
    fun aPlaceCycleTwoDevicesMadeTogetherIsBrokenTheSameOnBoth() {
        val w = world()
        val a = w.Device("A")
        val b = w.Device("B")
        val one = runBlocking { a.inventory.addPlace("one") }
        val two = runBlocking { a.inventory.addPlace("two") }
        w.settle(listOf(a, b))

        w.syncthing.now = 1_000
        runBlocking { a.inventory.movePlace(one, two) }
        w.syncthing.now = 2_000
        runBlocking { b.inventory.movePlace(two, one) }
        w.settle(listOf(a, b))

        for (d in listOf(a, b)) {
            val parents = runBlocking { d.inventory.places() }.associate { it.id to it.parentId }
            assertEquals(two, parents[one], d.name)
            assertNull(parents[two], d.name)
        }
    }

    /** a. A cycle of three, made by three devices, is broken once. */
    @Test
    fun aCycleOfThreeTagsIsBrokenAtItsNewestEdit() {
        val w = world()
        val devices = listOf(w.Device("A"), w.Device("B"), w.Device("C"))
        val (a, b, c) = devices
        val tags = listOf("x", "y", "z").map { runBlocking { a.inventory.addTag(it) } }
        w.settle(devices)

        w.syncthing.now = 1_000
        runBlocking { a.inventory.setParent(tags[0], tags[1]) }
        w.syncthing.now = 2_000
        runBlocking { b.inventory.setParent(tags[1], tags[2]) }
        w.syncthing.now = 3_000
        runBlocking { c.inventory.setParent(tags[2], tags[0]) }
        w.settle(devices)

        for (d in devices) {
            val parents = runBlocking { d.inventory.tags() }.associate { it.id to it.parentId }
            assertEquals(tags[1], parents[tags[0]], d.name)
            assertEquals(tags[2], parents[tags[1]], d.name)
            assertNull(parents[tags[2]], d.name)
        }
    }

    /** b. A child whose parent was purged here is dropped, not applied and not kept waiting. */
    private fun purgedParent(
        table: String,
        setup: (World.Device) -> Unit,
        purge: (World.Device) -> Unit,
        child: (World.Device) -> String,
    ) {
        val w = world()
        val a = w.Device("A")
        val b = w.Device("B")
        setup(a)
        w.meet(a, b)
        b.import()
        purge(a)
        a.export()
        w.syncthing.now += 10
        val childId = child(b)
        w.meet(b, a)

        val report = a.import()

        assertNull(a.rows(table)[childId], "the $table row of a purged parent is not applied")
        assertEquals(emptyList(), a.waiting())
        assertEquals(1, report.skipped, "it is dropped and counted")

        // Both devices end without it: the one that applies the purge removes what it still holds under it.
        w.settle(listOf(a, b))
        assertNull(a.rows(table)[childId], "A: $table")
        assertNull(b.rows(table)[childId], "B: $table")
    }

    private fun World.Device.purgeRow(table: String, id: String) =
        runBlocking { writes.write(mapOf(table to listOf(id))) { writes.merger.purge(it, id) } }

    @Test
    fun anItemTagLinkWhoseItemWasPurgedIsDropped() {
        lateinit var item: String
        lateinit var tag: String
        purgedParent(ITEM_TAG,
            setup = { item = it.create("lamp"); tag = runBlocking { it.inventory.addTag("light") } },
            purge = { it.purge(item) },
            child = { runBlocking { it.inventory.tag(listOf(item), tag) }; linkId(item, tag) },
        )
    }

    @Test
    fun anItemTagLinkWhoseTagWasPurgedIsDropped() {
        lateinit var item: String
        lateinit var tag: String
        purgedParent(ITEM_TAG,
            setup = { item = it.create("lamp"); tag = runBlocking { it.inventory.addTag("light") } },
            purge = { runBlocking { it.inventory.deleteTag(tag) } },
            child = { runBlocking { it.inventory.tag(listOf(item), tag) }; linkId(item, tag) },
        )
    }

    @Test
    fun aPlaceTagLinkWhoseTagWasPurgedIsDropped() {
        lateinit var place: String
        lateinit var tag: String
        purgedParent(PLACE_TAG,
            setup = { place = runBlocking { it.inventory.addPlace("shelf") }; tag = runBlocking { it.inventory.addTag("light") } },
            purge = { runBlocking { it.inventory.deleteTag(tag) } },
            child = { runBlocking { it.inventory.tagPlace(place, tag) }; it.rows(PLACE_TAG).keys.single() },
        )
    }

    @Test
    fun aPlaceTagLinkWhoseTagPlaceWasPurgedIsDropped() {
        lateinit var place: String
        lateinit var tag: String
        purgedParent(PLACE_TAG,
            setup = { place = runBlocking { it.inventory.addPlace("shelf") }; tag = runBlocking { it.inventory.addTag("light") } },
            purge = { runBlocking { it.inventory.deletePlace(place) } },
            child = { runBlocking { it.inventory.tagPlace(place, tag) }; it.rows(PLACE_TAG).keys.single() },
        )
    }

    @Test
    fun aPhotoWhoseItemWasPurgedIsDropped() {
        lateinit var item: String
        purgedParent(PHOTO,
            setup = { item = it.create("lamp") },
            purge = { it.purge(item) },
            child = { runBlocking { it.inventory.addPhoto(item, null, hash) } },
        )
    }

    @Test
    fun aPhotoWhosePlaceWasPurgedIsDropped() {
        lateinit var place: String
        purgedParent(PHOTO,
            setup = { place = runBlocking { it.inventory.addPlace("shelf") } },
            purge = { runBlocking { it.inventory.deletePlace(place) } },
            child = { runBlocking { it.inventory.addPhoto(null, place, hash) } },
        )
    }

    @Test
    fun aRegionWhosePhotoWasPurgedIsDropped() {
        lateinit var photo: String
        lateinit var target: String
        purgedParent(PHOTO_REGION,
            setup = {
                val owner = it.create("shelf item")
                target = it.create("lamp")
                photo = runBlocking { it.inventory.addPhoto(owner, null, hash) }
            },
            purge = { it.purgeRow(PHOTO, photo) },
            child = { runBlocking { it.inventory.addRegion(photo, 0.1, 0.1, 0.5, 0.5, target) } },
        )
    }

    @Test
    fun aRegionWhoseTargetItemWasPurgedIsDropped() {
        lateinit var photo: String
        lateinit var target: String
        purgedParent(PHOTO_REGION,
            setup = {
                val owner = it.create("shelf item")
                target = it.create("lamp")
                photo = runBlocking { it.inventory.addPhoto(owner, null, hash) }
            },
            purge = { it.purge(target) },
            child = { runBlocking { it.inventory.addRegion(photo, 0.1, 0.1, 0.5, 0.5, target) } },
        )
    }

    @Test
    fun aFieldValueWhoseFieldWasPurgedIsDropped() {
        lateinit var item: String
        lateinit var def: String
        purgedParent(FIELD_VALUE,
            setup = { item = it.create("lamp"); def = runBlocking { it.fields.define("colour", FieldType.TEXT) } },
            purge = { it.purgeRow(FIELD_DEF, def) },
            child = { runBlocking { it.fields.set(item, def, FieldInput.Text("brass")) }; it.rows(FIELD_VALUE).keys.single() },
        )
    }

    @Test
    fun anAttachmentWhoseFieldWasPurgedIsDropped() {
        lateinit var item: String
        lateinit var def: String
        purgedParent(FIELD_ATTACH,
            setup = { item = it.create("lamp"); def = runBlocking { it.fields.define("colour", FieldType.TEXT) } },
            purge = { it.purgeRow(FIELD_DEF, def) },
            child = { runBlocking { it.fields.attachToItem(def, item) }; it.rows(FIELD_ATTACH).keys.single() },
        )
    }

    /** The owner of a value is an id with no foreign key: the importer checks it against the purges itself. */
    @Test
    fun aFieldValueWhoseItemWasPurgedIsDropped() {
        lateinit var item: String
        lateinit var def: String
        purgedParent(FIELD_VALUE,
            setup = { item = it.create("lamp"); def = runBlocking { it.fields.define("colour", FieldType.TEXT) } },
            purge = { it.purge(item) },
            child = { runBlocking { it.fields.set(item, def, FieldInput.Text("brass")) }; it.rows(FIELD_VALUE).keys.single() },
        )
    }

    @Test
    fun aFieldValueWhosePlaceWasPurgedIsDropped() {
        lateinit var place: String
        lateinit var def: String
        purgedParent(FIELD_VALUE,
            setup = { place = runBlocking { it.inventory.addPlace("shelf") }; def = runBlocking { it.fields.define("colour", FieldType.TEXT) } },
            purge = { runBlocking { it.inventory.deletePlace(place) } },
            child = { runBlocking { it.fields.set(place, def, FieldInput.Text("brass")) }; it.rows(FIELD_VALUE).keys.single() },
        )
    }

    @Test
    fun anAttachmentToAnItemThatWasPurgedIsDropped() {
        lateinit var item: String
        lateinit var def: String
        purgedParent(FIELD_ATTACH,
            setup = { item = it.create("lamp"); def = runBlocking { it.fields.define("colour", FieldType.TEXT) } },
            purge = { it.purge(item) },
            child = { runBlocking { it.fields.attachToItem(def, item) }; it.rows(FIELD_ATTACH).keys.single() },
        )
    }

    /** R152: a value B made before A deleted the item (A never saw it) is gone from both devices. */
    @Test
    fun aValueMadeBeforeTheItemWasDeletedElsewhereIsGoneFromBoth() {
        val w = world()
        val a = w.Device("A")
        val b = w.Device("B")
        val item = a.create("lamp")
        val def = runBlocking { a.fields.define("colour", FieldType.TEXT) }
        w.settle(listOf(a, b))

        w.syncthing.now = 5
        runBlocking { b.fields.set(item, def, FieldInput.Text("brass")) }
        w.syncthing.now = 10
        a.purge(item)
        w.settle(listOf(a, b))

        for (d in listOf(a, b)) {
            assertTrue(d.rows(FIELD_VALUE).isEmpty(), "${d.name} still holds a value of the deleted item")
            assertTrue(d.rows().isEmpty(), d.name)
        }
    }

    /** B edits a value that A already purged along with its item: both devices end without it. */
    @Test
    fun aValueEditedAfterItsPurgeIsGoneFromBoth() {
        val w = world()
        val a = w.Device("A")
        val b = w.Device("B")
        val item = a.create("lamp")
        val def = runBlocking { a.fields.define("colour", FieldType.TEXT) }
        runBlocking { a.fields.set(item, def, FieldInput.Text("brass")) }
        w.settle(listOf(a, b))
        assertEquals(1, b.rows(FIELD_VALUE).size)

        w.syncthing.now = 10
        a.purge(item)
        w.syncthing.now = 20
        runBlocking { b.fields.set(item, def, FieldInput.Text("gold")) }
        w.settle(listOf(a, b))

        for (d in listOf(a, b)) {
            assertTrue(d.rows(FIELD_VALUE).isEmpty(), "${d.name} still holds the value")
            assertTrue(d.rows().isEmpty(), d.name)
        }
    }

    /** A snapshot can hold a row beside the purge of its owner in one chunk: the row is dropped, not applied. */
    @Test
    fun aRowAndThePurgeOfItsOwnerInOneFileLeaveNoRow() {
        val w = world()
        val a = w.Device("A")
        val def = runBlocking { a.fields.define("colour", FieldType.TEXT) }
        a.export()
        val c = w.Device("C")
        w.syncthing.session("A", "C")
        c.import()
        val attach = Row(FIELD_ATTACH, "x-attach", mapOf(
            TARGET to Group(Stamp(5, "X"), mapOf(
                "defId" to def, "tagId" to null, "itemId" to "gone-item", "wishId" to null, "everyItem" to false,
            )),
        ))
        val purge = PurgeRecord("gone-item", Stamp(4, "X"))
        w.syncthing.folder("C").replace(
            FolderLayout.snapshot("X", 0),
            (w.sealed(RecordCodec.encode(RowRecord(attach))) + w.sealed(RecordCodec.encode(purge))).encodeToByteArray(),
        )

        c.import()

        // Applied with the batch, then removed by the sweep that follows the import.
        assertTrue(c.rows(FIELD_ATTACH).isEmpty())
    }

    /** A purge taken back by a newer edit within one segment: a value on the revived owner, written after, still arrives. */
    @Test
    fun aValueOnAnOwnerWhosePurgeIsTakenBackInTheSameSegmentArrives() {
        val w = world()
        val a = w.Device("A")
        val item = a.create("lamp")
        val def = runBlocking { a.fields.define("colour", FieldType.TEXT) }
        a.export()
        val c = w.Device("C")
        w.syncthing.session("A", "C")
        c.import()
        val back = c.rows().getValue(item).edit(NAME, Stamp(6, "X"), mapOf("name" to "back"))
        val value = Row(FIELD_VALUE, "x-value", mapOf(
            com.hazia.chatelaine.data.VALUE to Group(Stamp(7, "X"), mapOf(
                "ownerId" to item, "defId" to def, "json" to com.hazia.chatelaine.data.encodeInput(FieldInput.Text("brass")),
            )),
        ))
        val lines = listOf(RecordCodec.encode(PurgeRecord(item, Stamp(5, "X"))), RecordCodec.encode(RowRecord(back)), RecordCodec.encode(RowRecord(value)))
        val folder = w.syncthing.folder("C")
        folder.replace(FolderLayout.segment("X", 0), lines.joinToString("") { w.sealed(it) }.encodeToByteArray())

        c.import()

        assertEquals(setOf(item), c.rows().keys)
        assertEquals(setOf("x-value"), c.rows(FIELD_VALUE).keys, "the value on the revived owner is kept")
        c.import()
        assertEquals(setOf("x-value"), c.rows(FIELD_VALUE).keys)
    }

    /** An attachment to a tag that was deleted goes on every device, those made before and after the delete. */
    @Test
    fun attachmentsToADeletedTagAreGoneFromBothDevices() {
        val w = world()
        val a = w.Device("A")
        val b = w.Device("B")
        val tag = runBlocking { a.inventory.addTag("light") }
        val first = runBlocking { a.fields.define("colour", FieldType.TEXT) }
        val second = runBlocking { a.fields.define("weight", FieldType.TEXT) }
        runBlocking { a.fields.attachToTag(first, tag) }
        w.settle(listOf(a, b))
        assertEquals(1, b.rows(FIELD_ATTACH).size)

        w.syncthing.now = 10
        runBlocking { a.inventory.deleteTag(tag) }
        assertTrue(a.rows(FIELD_ATTACH).isEmpty(), "deleting the tag purges its attachments")
        w.syncthing.now = 20
        runBlocking { b.fields.attachToTag(second, tag) }
        w.settle(listOf(a, b))

        for (d in listOf(a, b)) assertTrue(d.rows(FIELD_ATTACH).isEmpty(), "${d.name} still holds an attachment to the tag")
    }

    // ---- held lines, from the earlier app's cases ----

    private fun World.Device.itemName(id: String) = rows().getValue(id).groups.getValue(NAME).values["name"]

    @Test
    fun futureLineWaitsUntilPlausible() {
        val w = world()
        val a = w.Device("A")
        val b = w.Device("B")
        w.syncthing.now = 3 * day
        b.skew = -2 * day
        val id = a.create("lamp")
        val stamp = a.rows().getValue(id).newest
        a.export()
        w.syncthing.session("A", "B")

        assertEquals(ImportReport(changed = 0, skipped = 0, waiting = 1), b.import())
        assertTrue(b.rows().isEmpty())
        assertTrue(b.clock.last < stamp.hlc, "a line set aside must not move this device's clock")

        b.skew = 0
        assertEquals(ImportReport(changed = 1, skipped = 0, waiting = 0), b.import())
        assertEquals("lamp", b.itemName(id))
    }

    @Test
    fun aPlausibleLineAfterAHeldOneAppliesOnTheSameImport() {
        val w = world()
        val a = w.Device("A")
        val plausible = a.create("lamp")
        w.syncthing.now = 3 * day
        val ahead = a.create("kettle")
        a.export()
        val lines = w.plainLines("A", FolderLayout.segment("A", 0))
        val (p, q) = listOf(plausible, ahead).map { id -> lines.single { "\"id\":\"$id\"" in it } }
        val b = w.Device("B")
        b.skew = -2 * day
        // The line stamped ahead comes first in the file.
        w.syncthing.folder("B").replace(FolderLayout.segment("X", 0), (w.sealed(q) + w.sealed(p)).encodeToByteArray())

        assertEquals(ImportReport(changed = 1, skipped = 0, waiting = 1), b.import())
        assertEquals(setOf(plausible), b.rows().keys)

        b.skew = 0
        assertEquals(ImportReport(changed = 1, skipped = 0, waiting = 0), b.import())
        assertEquals(setOf(plausible, ahead), b.rows().keys)
    }

    @Test
    fun aHeldLineSurvivesTheWritersCompaction() {
        // Small segments and early compaction, so A's next exports replace the segment B holds a position in.
        val w = world(segmentBytes = 200, snapshotEvery = 2)
        val a = w.Device("A")
        val b = w.Device("B")
        w.syncthing.now = 3 * day
        b.skew = -2 * day
        val id = a.create("lamp")
        a.export()
        w.syncthing.session("A", "B")
        assertEquals(1, b.import().waiting)

        repeat(3) { a.edit(id, NOTES, "brass$it"); a.export() }
        val paths = w.syncthing.paths("A")
        assertFalse(FolderLayout.segment("A", 0) in paths, "the segment B read was deleted: $paths")
        assertTrue(paths.any { "/snapshot-" in it }, "no snapshot: $paths")
        w.syncthing.session("A", "B")
        b.import()
        assertTrue(b.rows().isEmpty(), "still ahead of B's clock")

        b.skew = 0
        b.import()
        assertEquals("lamp", b.itemName(id))
        assertEquals("brass2", b.rows().getValue(id).groups.getValue(NOTES).values["notes"])
    }

    @Test
    fun aConflictCopyHoldingAHeldLineIsKept() {
        val w = world()
        val a = w.Device("A")
        w.syncthing.now = 3 * day
        val id = a.create("lamp")
        a.export()
        val record = w.plainLines("A", FolderLayout.segment("A", 0)).single()
        val b = w.Device("B")
        b.skew = -2 * day
        val copy = "${FolderLayout.dir("X")}/log-000000.sync-conflict-5-X.jsonl"
        w.syncthing.folder("B").replace(copy, w.sealed(record).encodeToByteArray())

        assertEquals(1, b.import().waiting)
        assertTrue(copy in w.syncthing.paths("B"), "the copy holds a line set aside")

        b.skew = 0
        b.import()
        assertEquals("lamp", b.itemName(id))
        assertFalse(copy in w.syncthing.paths("B"), "applied, the copy goes")
    }

    private fun photoRow(id: String, itemId: String?, placeId: String?, hash: String) = Row(PHOTO, id, mapOf(
        OWNER to Group(Stamp(5, "X"), mapOf("itemId" to itemId, "placeId" to placeId)),
        BLOB to Group(Stamp(5, "X"), mapOf("hash" to hash)),
        TAKEN to Group(Stamp(5, "X"), mapOf("takenAt" to 5L)),
    ))

    private fun regionRow(id: String, photoId: String, itemId: String, x: Double, y: Double, w: Double, h: Double) = Row(PHOTO_REGION, id, mapOf(
        AREA to Group(Stamp(5, "X"), mapOf("photoId" to photoId, "x" to x, "y" to y, "w" to w, "h" to h)),
        TARGET to Group(Stamp(5, "X"), mapOf("itemId" to itemId)),
    ))

    private fun World.put(folder: String, file: String, rows: List<Row>) =
        syncthing.folder(folder).replace(
            FolderLayout.segment(file, 0),
            rows.joinToString("") { sealed(RecordCodec.encode(RowRecord(it))) }.encodeToByteArray(),
        )

    /** c. A peer's photo with two owners, none, or a hash that is not 64 lower-case hex digits is skipped and counted. */
    @Test
    fun aPeersInvalidPhotoIsSkippedAndCounted() {
        val w = world()
        val a = w.Device("A")
        val item = a.create("lamp")
        val place = runBlocking { a.inventory.addPlace("shelf") }
        val b = w.Device("B")
        w.meet(a, b)
        b.import()

        w.put("B", "bad", listOf(
            photoRow("p1", item, place, hash),
            photoRow("p2", null, null, hash),
            photoRow("p3", item, null, "A".repeat(64)),
            photoRow("p4", item, null, "a".repeat(63)),
            photoRow("p5", item, null, "g".repeat(64)),
        ))
        w.put("B", "good", listOf(photoRow("p6", item, null, hash)))

        assertEquals(ImportReport(changed = 1, skipped = 5), b.import())
        assertEquals(setOf("p6"), b.rows(PHOTO).keys)
    }

    /** c. A peer's region whose fractions lie outside the photo, or that has no size, is skipped and counted. */
    @Test
    fun aPeersInvalidRegionIsSkippedAndCounted() {
        val w = world()
        val a = w.Device("A")
        val item = a.create("lamp")
        val photo = runBlocking { a.inventory.addPhoto(item, null, hash) }
        val b = w.Device("B")
        w.meet(a, b)
        b.import()

        w.put("B", "bad", listOf(
            regionRow("r1", photo, item, -0.1, 0.1, 0.5, 0.5),
            regionRow("r2", photo, item, 0.1, -0.1, 0.5, 0.5),
            regionRow("r3", photo, item, 0.1, 0.1, 0.0, 0.5),
            regionRow("r4", photo, item, 0.1, 0.1, 0.5, -0.5),
            regionRow("r5", photo, item, 0.6, 0.1, 0.5, 0.5),
            regionRow("r6", photo, item, 0.1, 0.6, 0.5, 0.5),
        ))
        w.put("B", "good", listOf(regionRow("r8", photo, item, 0.0, 0.0, 1.0, 1.0)))

        assertEquals(ImportReport(changed = 1, skipped = 6), b.import())
        assertEquals(setOf("r8"), b.rows(PHOTO_REGION).keys)
    }

    // ---- the earlier app's folder-sync cases, on items ----

    @Test
    fun stNoConflictCopies_aMonthOfOrdinaryUseMakesNone() {
        val w = world()
        val devices = ordinaryUse(w, days = 30, editsPerStep = 2, Random(7))
        w.settle(devices)

        assertEquals(0, w.syncthing.conflictCopies)
    }

    @Test
    fun ffNothingLost_cutSessionsThenSettlingLeaveEveryDeviceWithTheJoin() {
        val w = world()
        val devices = ordinaryUse(w, days = 30, editsPerStep = 3, Random(11))
        w.settle(devices)

        w.assertJoined(devices)
        assertTrue(w.truth.size > 100, "the month wrote too little to test: ${w.truth.size}")
    }

    @Test
    fun stRetiredLoser_aWipedDevicesChangeSurvives() {
        val w = world()
        val a = w.Device("A")
        val b = w.Device("B")
        val row = a.create()
        a.export()
        w.syncthing.session("A", "B")
        b.import()

        w.syncthing.now = 10
        b.edit(row, NOTES)
        w.syncthing.now = 20
        a.edit(row, NAME)
        b.export()
        a.export()
        w.syncthing.session("A", "B")
        w.syncthing.forget("B")
        val c = w.Device("C")
        w.settle(listOf(a, c))

        w.assertJoined(listOf(a, c))
    }

    @Test
    fun aVersionOutlivesTheLossOfItsAuthorsFiles() {
        val w = world()
        val a = w.Device("A")
        val b = w.Device("B")
        val row = b.create()
        b.edit(row, NAME)
        b.export()
        w.syncthing.session("A", "B")
        a.import()
        a.export()

        // B is wiped, and its files are lost from the folder too: only A's copy of B's versions is left.
        w.syncthing.forget("B")
        val folder = w.syncthing.folder("A")
        folder.list(FolderLayout.dir("B")).forEach { folder.delete("${FolderLayout.dir("B")}/$it") }
        val c = w.Device("C")
        w.settle(listOf(a, c))

        w.assertJoined(listOf(a, c))
    }

    @Test
    fun aYearKeepsEachDeviceToItsSegmentsAndOneSnapshot() {
        val w = world()
        // Steps of 4 hours: the file count follows how much is exported, not how often devices meet.
        val devices = ordinaryUse(w, days = 365, editsPerStep = 4, Random(3), stepHours = 4)
        w.settle(devices)

        val files = devices.flatMap { w.syncthing.paths(it.name) }.toSet()
        assertTrue(files.size < 10_000)
        for (d in devices) {
            val own = files.filter { it.startsWith("devices/${d.id}/") }
            assertTrue(own.size <= 64 + 1, "${d.id} holds ${own.size} files")
            assertTrue(own.any { "/snapshot-" in it }, "a year should have compacted ${d.id}'s log at least once")
        }
        w.assertJoined(devices)
    }

    @Test
    fun aClonedDeviceIdsClashIsMergedAndTheCopyRemoved() {
        val w = world()
        val a = w.Device("A")
        val clone = w.Device("A'", id = "A")
        val mine = a.create()
        w.syncthing.now = 5
        val theirs = clone.create()
        a.export()
        clone.export()
        w.syncthing.session("A", "A'")
        assertEquals(1, w.syncthing.conflictCopies)

        a.import()
        w.syncthing.forget("A'")

        assertNotNull(a.rows()[mine])
        assertNotNull(a.rows()[theirs])
        assertTrue(w.syncthing.paths("A").none { ConflictCopies.isCopy(it) })
    }

    // A database made anew (a corrupt file set aside) does not compact its own log before it has read it back: a
    // snapshot of the new, near-empty database would replace the segments holding its history.
    @Test
    fun aRecoveredDatabaseDoesNotCompactBeforeReadingItsOwnLog() {
        val w = world(segmentBytes = 200, snapshotEvery = 2)
        val before = w.Device("A")
        val old = before.create()
        repeat(2) { before.edit(old, NAME); before.export() }
        val after = w.Device("A-new", id = "A", recovered = true)
        w.syncthing.session("A", "A-new")
        val fresh = after.create()
        repeat(3) { after.edit(fresh, NAME); after.export() } // written before the first import, as a person may
        after.import()

        assertEquals(setOf(old, fresh), after.rows().keys, "the history written before the database was made anew is back")
    }

    // A database made anew keeps waiting for its own log while none of it is in the folder (not arrived, or the
    // listing came back empty): only an import that read it says so, and compaction waits until then.
    @Test
    fun aRecoveredDatabaseWaitsUntilItsOwnLogIsThere() {
        val w = world(segmentBytes = 200, snapshotEvery = 2)
        val before = w.Device("A")
        val old = before.create()
        repeat(2) { before.edit(old, NAME); before.export() }

        val after = w.Device("A-new", id = "A", recovered = true)
        after.import() // the own log has not arrived in A-new's folder yet
        assertTrue(after.sync.readingOwnLog, "nothing of the own log was read: still waiting")
        w.syncthing.session("A", "A-new")
        after.import()
        assertFalse(after.sync.readingOwnLog, "the own log was read back")
        assertEquals(setOf(old), after.rows().keys)
    }

    @Test
    fun aPurgeReachesAPeerAndOutlivesCompaction() {
        // Segments of one line, compacted every two, so three edits compact.
        val w = world(segmentBytes = 200, snapshotEvery = 2)
        val a = w.Device("A")
        val b = w.Device("B")
        val gone = a.create()
        val kept = a.create()
        a.export()
        w.syncthing.session("A", "B")
        b.import()
        a.purge(gone)
        repeat(3) { a.edit(kept, NAME); a.export() }

        val late = w.Device("C")
        w.settle(listOf(a, b, late))

        for (d in listOf(a, b, late)) {
            assertEquals(setOf(kept), d.rows().keys, d.name)
            assertEquals(listOf(gone), runBlocking { d.db.syncDao().purges() }.map { it.id }, d.name)
        }
        assertTrue(w.syncthing.paths("A").any { "/snapshot-" in it })
    }

    @Test
    fun aPurgeReachesAPeerThroughTheLog() {
        val w = world()
        val a = w.Device("A")
        val b = w.Device("B")
        val gone = a.create()
        a.export()
        w.syncthing.session("A", "B")
        b.import()

        a.purge(gone)
        a.export()
        w.syncthing.session("A", "B")
        // The row is live on B when the purge arrives: the import applies it all the same.
        assertEquals(setOf(gone), b.rows().keys)
        b.import()

        assertTrue(b.rows().isEmpty())
        assertTrue(w.syncthing.paths("A").none { "/snapshot-" in it })
    }

    @Test
    fun aPeerFileIsReadOnlyWhenItHasGrown() {
        val w = world()
        val a = w.Device("A")
        val b = w.Device("B")
        a.create()
        a.export()
        w.syncthing.session("A", "B")
        b.import()
        val before = w.syncthing.bytesRead

        b.import()

        assertEquals(before, w.syncthing.bytesRead)
    }

    @Test
    fun aBigFileIsMergedInChunksEachWithItsReadPosition() {
        val w = world()
        val lines = (0 until 2_500).map { w.sealed(line("r$it")) }
        w.syncthing.folder("B").replace(FolderLayout.segment("A", 0), lines.joinToString("").encodeToByteArray())
        val memory = MemoryTable()
        val b = w.Device("B", extra = mapOf(ITEM to memory))

        memory.savesLeft = 1
        assertFailsWith<IllegalStateException> { b.import() }
        assertEquals(2_000, memory.rows.size)
        val firstChunk = lines.take(2_000).sumOf { it.encodeToByteArray().size }.toLong()
        assertEquals(firstChunk, runBlocking { b.db.syncDao().reads() }.single().readBytes)

        memory.savesLeft = Int.MAX_VALUE
        b.import()
        assertEquals(2_500, memory.rows.size)
    }

    @Test
    fun aLineStillBeingWrittenWaitsForItsNewline() {
        val w = world()
        val b = w.Device("B", extra = mapOf(ITEM to MemoryTable()))
        val path = FolderLayout.segment("A", 0)
        val folder = w.syncthing.folder("B")

        folder.replace(path, w.sealed(line("x")).removeSuffix("\n").encodeToByteArray())
        b.import()
        assertTrue(b.rows().isEmpty())

        folder.replace(path, w.sealed(line("x")).encodeToByteArray())
        b.import()
        assertNotNull(b.rows()["x"])
    }

    @Test
    fun aLineForATableThisVersionLacksIsReadAgainOnceItHasIt() {
        val w = world()
        val node = Row("future_table", "n", mapOf("a" to Group(Stamp(1, "A"), mapOf("title" to "notes"))))
        val lines = w.sealed(RecordCodec.encode(RowRecord(node))) + w.sealed("{not json")
        w.syncthing.folder("old").replace(FolderLayout.segment("A", 0), lines.encodeToByteArray())

        val old = w.Device("old")
        assertEquals(ImportReport(changed = 0, skipped = 2), old.import())
        old.db.close()

        val upgraded = w.Device("old", extra = mapOf("future_table" to MemoryTable()))
        assertEquals(ImportReport(changed = 1, skipped = 1), upgraded.import())
    }

    @Test
    fun aRowThatDoesNotFitItsTableIsSkipped() {
        val w = world()
        w.syncthing.folder("B").replace(FolderLayout.segment("A", 0), w.sealed(line("x")).encodeToByteArray())
        val b = w.Device("B")

        assertEquals(ImportReport(changed = 0, skipped = 1), b.import())
        assertTrue(b.rows().isEmpty())
    }

    /** A link that arrives before its parents waits for them; with two of its parents still missing it keeps waiting. */
    @Test
    fun anItemTagLinkThatArrivesBeforeItsParentsWaitsForThem() {
        val w = world()
        val a = w.Device("A")
        val item = a.create("lamp")
        val tag = runBlocking { a.inventory.addTag("light") }
        runBlocking { a.inventory.tag(listOf(item), tag) }
        a.export()
        val lines = w.plainLines("A", FolderLayout.segment("A", 0))
        val linkLine = lines.single { "\"t\":\"$ITEM_TAG\"" in it }
        val itemLine = lines.single { "\"id\":\"$item\"" in it }
        val tagLine = lines.single { "\"id\":\"$tag\"" in it }

        val b = w.Device("B")
        val folder = w.syncthing.folder("B")
        folder.replace(FolderLayout.segment("X", 0), w.sealed(linkLine).encodeToByteArray())
        b.import()
        assertEquals(emptySet(), b.rows(ITEM_TAG).keys)
        assertEquals(1, b.waiting().size)

        // One parent arrives: still waiting for the other.
        folder.replace(FolderLayout.segment("Y", 0), w.sealed(itemLine).encodeToByteArray())
        b.import()
        assertEquals(emptySet(), b.rows(ITEM_TAG).keys)
        assertEquals(1, b.waiting().size)

        folder.replace(FolderLayout.segment("Z", 0), w.sealed(tagLine).encodeToByteArray())
        b.import()
        assertEquals(setOf(item), b.rows().keys)
        assertEquals(1, b.rows(ITEM_TAG).size)
        assertEquals(emptyList(), b.waiting())
    }

    /** A line waiting for its parent is kept once, though two files hold it. */
    @Test
    fun aLineWaitingForItsParentsIsKeptOnceThoughTwoFilesHoldIt() {
        val w = world()
        val a = w.Device("A")
        val item = a.create("lamp")
        val tag = runBlocking { a.inventory.addTag("light") }
        runBlocking { a.inventory.tag(listOf(item), tag) }
        a.export()
        val linkLine = w.plainLines("A", FolderLayout.segment("A", 0)).single { "\"t\":\"$ITEM_TAG\"" in it }
        val b = w.Device("B")
        val folder = w.syncthing.folder("B")

        folder.replace(FolderLayout.segment("X", 0), w.sealed(linkLine).encodeToByteArray())
        folder.replace(FolderLayout.segment("Y", 0), w.sealed(linkLine).encodeToByteArray())
        b.import()

        assertEquals(1, b.waiting().size)
    }

    /** A link whose item was purged is dropped, not kept waiting. */
    @Test
    fun aLinkWhoseItemWasPurgedIsDroppedNotKeptWaiting() {
        val w = world()
        val a = w.Device("A")
        val item = a.create("lamp")
        val tag = runBlocking { a.inventory.addTag("light") }
        runBlocking { a.inventory.tag(listOf(item), tag) }
        a.export()
        val linkLine = w.plainLines("A", FolderLayout.segment("A", 0)).single { "\"t\":\"$ITEM_TAG\"" in it }
        a.purge(item)
        a.export()
        val b = w.Device("B")
        w.syncthing.session("A", "B")
        b.import()

        w.syncthing.folder("B").replace(FolderLayout.segment("X", 0), w.sealed(linkLine).encodeToByteArray())

        assertEquals(ImportReport(changed = 0, skipped = 1), b.import())
        assertEquals(emptyList(), b.waiting())
    }

    /** An undone purge reads every file again; the second pass's skipped and waiting lines are not added to the first's. */
    @Test
    fun anUndonePurgeCountsEachSkippedLineOnce() {
        val w = world()
        val a = w.Device("A")
        val b = w.Device("B")
        val row = a.create()
        a.export()
        w.syncthing.session("A", "B")
        b.import()

        w.syncthing.now = 10
        a.purge(row)
        a.export()
        // B edits the row after the purge, so B takes the purge back when it arrives.
        w.syncthing.now = 20
        b.edit(row, NAME)
        w.syncthing.session("A", "B")
        w.syncthing.folder("B").replace(FolderLayout.segment("X", 0), w.sealed("{not json").encodeToByteArray())

        val report = b.import()

        assertNotNull(b.rows()[row], "the edit after the purge keeps the row")
        assertEquals(1, report.skipped, "one unreadable line in the folder")
        assertEquals(0, report.waiting)
    }

    /** The constraint backstop: a chunk the database refuses is applied line by line, and the refused line is dropped. */
    @Test
    fun aLineAConstraintRefusesIsDroppedAndTheOthersApply() {
        val w = world()
        val table = RefusingTable(refused = "y")
        val b = w.Device("B", extra = mapOf(ITEM to table))
        w.syncthing.folder("B").replace(FolderLayout.segment("A", 0), (w.sealed(line("x")) + w.sealed(line("y"))).encodeToByteArray())

        assertEquals(ImportReport(changed = 1, skipped = 1), b.import())
        assertEquals(setOf("x"), table.rows.keys)
        assertEquals(emptyList(), b.waiting(), "a refused line with no missing parent does not wait")
    }

    // ---- the clock across a restart (R51, R54) ----

    /** A peer whose clock runs fast (within the day the guard allows) edits a name that this device then sees. */
    private fun World.aheadPeerEdit(a: World.Device, b: World.Device): String {
        b.skew = 6 * hour
        val item = a.create("lamp")
        settle(listOf(a, b))
        syncthing.now = 1_000
        b.edit(item, NAME, "B's name")
        settle(listOf(a, b))
        return item
    }

    /** A device restarted after seeing a stamp ahead of its wall clock still stamps its next edit above it. */
    @Test
    fun aRestartedDevicesEditOutranksAStampItSawAheadOfItsClock() {
        val w = world()
        val a = w.Device("A")
        val b = w.Device("B")
        val item = w.aheadPeerEdit(a, b)
        a.db.close()

        val again = w.Device("A") // the same database, opened again
        w.syncthing.now = 2_000
        again.edit(item, NAME, "A's later name")
        w.settle(listOf(again, b))

        for (d in listOf(again, b)) {
            assertEquals("A's later name", d.rows().getValue(item).groups.getValue(NAME).values["name"], "${d.name} holds the later edit")
        }
    }

    /** With no stored clock at all (a fresh clock over the rows), a local edit still outranks the version it replaces. */
    @Test
    fun aLocalEditOutranksTheVersionItReplacesWithAFreshClock() {
        val w = world()
        val a = w.Device("A")
        val b = w.Device("B")
        val item = w.aheadPeerEdit(a, b)
        val seen = a.rows().getValue(item).groups.getValue(NAME).stamp

        val fresh = HybridClock("A", wallMillis = { w.syncthing.now })
        runBlocking { Inventory(a.db, LocalWrites(a.db, fresh)).rename(item, "A's later name") }

        val written = a.rows().getValue(item).groups.getValue(NAME).stamp
        assertTrue(written > seen, "the edit's stamp $written is above the one it replaced, $seen")
    }

    /** A table that refuses to save the row [refused] as a foreign key would, and saves nothing of that call. */
    private class RefusingTable(private val refused: String) : RowTable {
        val rows = mutableMapOf<String, Row>()
        override suspend fun load(ids: Collection<String>) = ids.mapNotNull { rows[it] }
        override suspend fun all() = rows.values.toList()
        override suspend fun save(rows: Collection<Row>) {
            if (rows.any { it.id == refused }) throw SQLiteException("Error code: 787, message: FOREIGN KEY constraint failed")
            rows.forEach { this.rows[it.id] = it }
        }
        override suspend fun delete(ids: Collection<String>) = ids.forEach { rows.remove(it) }
        override fun fits(row: Row) = true
        override fun parents(row: Row) = emptyList<Pair<String, String>>()
    }

    /** A record of a row in a table only [MemoryTable] would take: one group, no item fields. */
    private fun line(id: String) =
        RecordCodec.encode(RowRecord(Row(ITEM, id, mapOf("a" to Group(Stamp(1, "A"), mapOf("v" to id))))))
}
