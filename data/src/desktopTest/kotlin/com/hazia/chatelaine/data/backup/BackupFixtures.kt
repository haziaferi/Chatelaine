package com.hazia.chatelaine.data.backup

import com.hazia.chatelaine.data.FieldInput
import com.hazia.chatelaine.data.FieldType
import com.hazia.chatelaine.data.SYNCED_TABLES
import com.hazia.chatelaine.data.blob.Blobs
import com.hazia.chatelaine.data.blob.MemoryBlobStore
import com.hazia.chatelaine.data.sync.SyncFolder
import com.hazia.chatelaine.data.sync.World
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

/** A device of [world] with its photo files in memory, and the [Backup] over both; [recovered] as for [World.Device]. */
internal class Phone(world: World, name: String, recovered: Boolean = false) {
    val device = world.Device(name, recovered = recovered)
    val store = MemoryBlobStore()
    val blobs = Blobs(device.db, store)
    val backup = Backup(device.db, store, device.clock, ZipBackup, wallMillis = { world.syncthing.now })
}

private fun <T> run(block: suspend () -> T): T = runBlocking { block() }

private fun localDate(millis: Long) = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).date

/**
 * R60's round trip: a device holding 20 items, 5 places, 6 tags, 3 photos with files, fields and a purge exports to
 * [target], and a new device restores it: every synced table, the purge registry and every photo file are equal.
 */
internal fun assertExportImportRoundTrip(world: World, target: SyncFolder) {
    val a = Phone(world, "A")
    val inv = a.device.inventory
    val items = run { (1..20).map { inv.addItem("item $it") } }
    val places = run { (1..5).map { inv.addPlace("place $it") } }
    val tags = run { (1..6).map { inv.addTag("tag $it") } }
    val pictures = (1..3).map { n -> a.blobs.add(ByteArray(100) { b -> (b * n).toByte() }) }
    run {
        inv.tag(items.take(5), tags[0])
        inv.tagPlace(places[0], tags[1])
        val photos = pictures.mapIndexed { i, hash -> inv.addPhoto(if (i < 2) items[i] else null, if (i < 2) null else places[0], hash) }
        inv.addRegion(photos[0], 0.1, 0.1, 0.2, 0.2, items[0])
        a.device.fields.define("colour", FieldType.CHOICE, options = listOf("red", "blue"))
        val def = a.device.fields.define("size", FieldType.TEXT)
        a.device.fields.attachToItem(def, items[0])
        a.device.fields.set(items[0], def, FieldInput.Text("big"))
    }
    val purged = run { inv.addItem("purged").also { inv.deleteItem(it) } }
    world.syncthing.now += 5_000
    val name = run { a.backup.write(target, world.syncthing.now) }!!
    assertEquals("chatelaine-backup-${localDate(world.syncthing.now)}.zip", name)

    val b = Phone(world, "B")
    val report = run { b.backup.restore(target.read(name)!!) }

    assertEquals(0, report.skipped)
    val purges = run { a.device.db.syncDao().purges() }.sortedBy { it.id }
    assertTrue(purges.any { it.id == purged }, "the purge is in the registry")
    assertEquals(purges, run { b.device.db.syncDao().purges() }.sortedBy { it.id }, "the purge registry, ids and stamps")
    for (table in SYNCED_TABLES) {
        val mine = a.device.rows(table)
        assertTrue(mine.isNotEmpty(), "the workload left $table empty")
        assertEquals(mine, b.device.rows(table), "table $table differs")
    }
    for (hash in pictures) assertEquals(a.store.read(hash)!!.toList(), b.store.read(hash)?.toList(), "the photo $hash")
}
