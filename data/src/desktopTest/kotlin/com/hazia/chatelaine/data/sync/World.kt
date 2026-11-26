package com.hazia.chatelaine.data.sync

import com.hazia.chatelaine.core.sync.Group
import com.hazia.chatelaine.core.sync.Row
import com.hazia.chatelaine.data.ChatelaineDatabase
import com.hazia.chatelaine.data.Fields
import com.hazia.chatelaine.data.History
import com.hazia.chatelaine.data.ITEM
import com.hazia.chatelaine.data.Inventory
import com.hazia.chatelaine.data.LocalWrites
import com.hazia.chatelaine.data.NAME
import com.hazia.chatelaine.data.NOTES
import com.hazia.chatelaine.data.openChatelaineDatabase
import com.hazia.chatelaine.data.syncedTables
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.random.Random
import kotlin.test.assertEquals

/** The groups the ordinary-use workload edits: neither asks a person (none does in Chatelaine). */
val GROUPS = listOf(NAME, NOTES)

/** A table held in memory, for rows no real table has. Its saves fail once [savesLeft] runs out. */
internal class MemoryTable : RowTable {
    val rows = mutableMapOf<String, Row>()
    var savesLeft = Int.MAX_VALUE
    override suspend fun load(ids: Collection<String>) = ids.mapNotNull { rows[it] }
    override suspend fun all() = rows.values.toList()
    override suspend fun save(rows: Collection<Row>) {
        check(savesLeft-- > 0) { "disk full" }
        rows.forEach { this.rows[it.id] = it }
    }
    override suspend fun delete(ids: Collection<String>) = ids.forEach { rows.remove(it) }
    override fun fits(row: Row) = true
    override fun parents(row: Row) = emptyList<Pair<String, String>>()
}

/**
 * Devices sharing one folder through [syncthing], writing items through [LocalWrites], their lines in plain text
 * ([PlainLines]). [truth] holds the newest version of every group any device wrote: what every device must hold once
 * sync settles (the join).
 */
internal class World(private val dir: File, seed: Int, private val segmentBytes: Int = 16 * 1024, private val snapshotEvery: Int = 64) {
    val syncthing = SyncthingDouble(Random(seed))
    val truth = mutableMapOf<Pair<String, String>, Group>()
    private val opened = mutableListOf<ChatelaineDatabase>()

    /** A folder line as a device writes it: [plain] sealed, then a newline. */
    fun sealed(plain: String) = PlainLines.seal(plain.encodeToByteArray()) + "\n"

    /** The plain records of the file at [path] in [device]'s copy of the folder. */
    fun plainLines(device: String, path: String): List<String> =
        syncthing.folder(device).read(path)!!.decodeToString().lines().filter { it.isNotEmpty() }.map { requireNotNull(PlainLines.open(it)).decodeToString() }

    /**
     * [id] is the sync identity; two devices given the same one model a cloned device id.
     * [extra] adds tables, or replaces a real one, for rows written straight into the folder.
     * [recovered] says the database was made anew; left null it is whatever opening the file found.
     */
    inner class Device(
        val name: String,
        val id: String = name,
        extra: Map<String, RowTable> = emptyMap(),
        recovered: Boolean? = null,
    ) {
        /** How far this device's wall clock is from the shared one, in milliseconds. */
        var skew = 0L
        private val wall = { syncthing.now + skew }
        private val open = openChatelaineDatabase(File(dir, "$name.db"))
        val db = open.database.also { opened += it }
        /** Resumed from the database, as the app's own clock is. */
        val clock = runBlocking { db.syncDao().loadClock(id, wall) }
        private val tables = db.syncedTables() + extra
        val writes = LocalWrites(db, clock)
        val inventory = Inventory(db, writes)
        val fields = Fields(db, writes)
        val history = History(db, writes)
        val sync = FolderSync(
            db, syncthing.folder(name), id, clock, tables, PlainLines, wall, segmentBytes, snapshotEvery,
            recovered = recovered ?: open.recovered,
        )

        /** A new item, every group stamped with one tick. */
        fun create(name: String = "Item ${this.name}@${syncthing.now}"): String {
            val itemId = runBlocking { inventory.addItem(name) }
            record(itemId)
            return itemId
        }

        fun edit(itemId: String, group: String, value: String = "$name@${syncthing.now}") {
            runBlocking {
                when (group) {
                    NAME -> inventory.rename(itemId, value)
                    NOTES -> inventory.setNotes(itemId, value)
                    else -> error("the workload does not edit $group")
                }
            }
            record(itemId)
        }

        /** Delete forever; a peer applies it through its import. */
        fun purge(itemId: String) = runBlocking { inventory.deleteItem(itemId) }

        private fun record(itemId: String) {
            for ((g, v) in rows().getValue(itemId).groups) {
                truth.merge(itemId to g, v) { old, new -> if (new.stamp > old.stamp) new else old }
            }
        }

        fun rows(table: String = ITEM): Map<String, Row> = runBlocking { tables.getValue(table).all() }.associateBy { it.id }

        fun import() = runBlocking { sync.import() }

        fun export() = runBlocking { sync.export() }

        fun held(): Map<Pair<String, String>, Group> =
            rows().values.flatMap { r -> r.groups.map { (g, v) -> (r.id to g) to v } }.toMap()
    }

    /** Every device exports, every pair meets in full, and every device imports, until an import changes nothing. */
    fun settle(devices: List<Device>) {
        repeat(20) {
            devices.forEach { it.export() }
            for (a in devices) for (b in devices) if (a.name < b.name) syncthing.session(a.name, b.name)
            if (devices.sumOf { it.import().changed } == 0) return
        }
        error("sync did not settle")
    }

    fun assertJoined(devices: List<Device>) {
        for (d in devices) assertEquals(truth, d.held(), "device ${d.name} differs from the join")
    }

    fun close() = opened.forEach { it.close() }
}
