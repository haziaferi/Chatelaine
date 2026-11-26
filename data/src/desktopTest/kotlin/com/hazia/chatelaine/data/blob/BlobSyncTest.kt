package com.hazia.chatelaine.data.blob

import com.hazia.chatelaine.data.sync.SyncFolder
import com.hazia.chatelaine.data.sync.World
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Photo files named by the SHA-256 of their bytes, carried through the sync folder (R56). */
class BlobSyncTest {

    @get:Rule val tmp = TemporaryFolder()

    private val worlds = mutableListOf<World>()
    @After fun close() = worlds.forEach { it.close() }

    private val day = 86_400_000L

    /** A device with its own stored files and the exchange that carries them. */
    private inner class Peer(val device: World.Device, world: World) {
        val store = MemoryBlobStore()
        val blobs = Blobs(device.db, store)
        val folder = CountingFolder(world.syncthing.folder(device.name))
        val sync = BlobSync(device.db, folder, store) { world.syncthing.now }
        fun exchange() = runBlocking { sync.exchange() }
        fun addPhoto(itemId: String, bytes: ByteArray): String = runBlocking {
            val hash = blobs.add(bytes)
            device.inventory.addPhoto(itemId, null, hash)
            hash
        }
    }

    /** A folder that remembers the path of every [replace] made through it. */
    private class CountingFolder(private val folder: SyncFolder) : SyncFolder by folder {
        val replaced = mutableListOf<String>()
        override fun replace(path: String, bytes: ByteArray) {
            replaced += path
            folder.replace(path, bytes)
        }
    }

    private fun <T> two(seed: Int, body: World.(Peer, Peer) -> T): T {
        val w = World(tmp.newFolder(), seed).also { worlds += it }
        return w.body(Peer(w.Device("A"), w), Peer(w.Device("B"), w))
    }

    /** The logs and the files, exchanged in turn until nothing more arrives. */
    private fun World.settleWithFiles(peers: List<Peer>) {
        repeat(3) {
            peers.forEach { it.exchange(); it.device.export() }
            for (a in peers) for (b in peers) if (a.device.name < b.device.name) syncthing.session(a.device.name, b.device.name)
            peers.forEach { it.device.import(); it.exchange() }
        }
    }

    private fun World.blobPaths(device: String) = syncthing.paths(device).filter { it.startsWith("blobs/") }.toSet()

    private val bytes = "A photo's bytes, invented for a test".encodeToByteArray()

    // R56
    @Test
    fun photoTravelsAsFileNamedByItsHash() = two(1) { a, b ->
        syncthing.now = day
        val item = a.device.create("lamp")
        val hash = a.addPhoto(item, bytes)
        settleWithFiles(listOf(a, b))

        assertTrue(Regex("[0-9a-f]{64}").matches(hash))
        assertEquals(setOf("blobs/$hash"), blobPaths("B"))
        assertContentEquals(bytes, syncthing.folder("B").read("blobs/$hash"))
        assertEquals(hash, runBlocking { b.device.db.photoDao().photosOfItem(item) }.single().hash)
        assertContentEquals(bytes, runBlocking { b.blobs.read(hash) })
    }

    @Test
    fun sameBytesAreNotWrittenTwice() = two(2) { a, b ->
        syncthing.now = day
        val item = a.device.create("lamp")
        val other = a.device.create("shade")
        val first = a.addPhoto(item, bytes)
        val second = a.addPhoto(other, bytes)
        assertEquals(first, second)
        assertEquals(listOf(first), a.store.list())
        settleWithFiles(listOf(a, b))

        assertEquals(setOf("blobs/$first"), blobPaths("A"))
        assertEquals(setOf("blobs/$first"), blobPaths("B"))
        assertEquals(listOf(first), b.store.list())
        assertEquals(1, a.store.writes, "the second add finds the file stored and writes nothing")
        assertEquals(1, a.folder.replaced.count { it == "blobs/$first" }, "the file is published once, however many exchanges")
        assertEquals(emptyList(), b.folder.replaced, "the device that fetched it never writes it back")
    }

    /** A temporary file a write cut short left beside a whole copy of its file is cleared, not carried to every device. */
    @Test
    fun aLeftoverTempFileBesideItsFileIsCleared() = two(2) { a, b ->
        syncthing.now = day
        val hash = a.addPhoto(a.device.create("lamp"), bytes)
        a.exchange()
        syncthing.folder("A").replace("blobs/.tmp-$hash", bytes.copyOf(5))
        val other = "b".repeat(64)
        syncthing.folder("A").replace("blobs/.tmp-$other", bytes.copyOf(5)) // its file is not here: maybe still being written

        a.exchange()

        assertEquals(setOf("blobs/$hash", "blobs/.tmp-$other"), blobPaths("A"))
    }

    @Test
    fun unreferencedBlobGoesAfterGracePeriod() = two(3) { a, b ->
        syncthing.now = day
        val item = a.device.create("lamp")
        val hash = a.addPhoto(item, bytes)
        settleWithFiles(listOf(a, b))
        assertEquals(listOf(hash), b.store.list())

        a.device.purge(item)
        settleWithFiles(listOf(a, b))
        // Unreferenced, but the grace period has not run out: still stored.
        syncthing.now += 29 * day
        settleWithFiles(listOf(a, b))
        assertEquals(setOf("blobs/$hash"), blobPaths("A"))
        assertEquals(listOf(hash), a.store.list())

        syncthing.now += 2 * day
        settleWithFiles(listOf(a, b))
        assertEquals(emptySet(), blobPaths("A"))
        assertEquals(emptySet(), blobPaths("B"))
        assertEquals(emptyList(), a.store.list())
        assertEquals(emptyList(), b.store.list())
        assertNull(runBlocking { a.blobs.read(hash) })
    }

    @Test
    fun unchangedPublishedBlobIsNotReadAgain() = two(5) { a, b ->
        syncthing.now = day
        val counting = CountingStore(a.store)
        val sync = BlobSync(a.device.db, syncthing.folder("A"), counting) { syncthing.now }
        val item = a.device.create("lamp")
        a.addPhoto(item, bytes)
        runBlocking { sync.exchange() }
        assertTrue(counting.reads > 0, "the first exchange publishes it")
        counting.reads = 0
        runBlocking { sync.exchange() }
        assertEquals(0, counting.reads)
    }

    private class CountingStore(private val inner: BlobStore) : BlobStore by inner {
        var reads = 0
        override fun read(name: String): ByteArray? = inner.read(name).also { reads++ }
    }

    @Test
    fun damagedOrMismatchedFileIsNotStored() = two(4) { a, b ->
        syncthing.now = day
        val item = a.device.create("lamp")
        val hash = a.addPhoto(item, bytes)
        a.exchange()
        a.device.export()
        // Other bytes than the name promises, in the folder copy B will fetch.
        syncthing.folder("A").replace("blobs/$hash", "other bytes".encodeToByteArray())
        syncthing.session("A", "B")
        b.device.import()
        b.exchange()
        assertEquals(emptyList(), b.store.list())

        // The device holding it undamaged puts it back, and then it arrives.
        settleWithFiles(listOf(a, b))
        assertContentEquals(bytes, runBlocking { b.blobs.read(hash) })
    }
}
