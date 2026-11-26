package com.hazia.chatelaine.data.blob

import com.hazia.chatelaine.data.ChatelaineDatabase
import com.hazia.chatelaine.data.sync.ConflictCopies
import com.hazia.chatelaine.data.sync.SyncFolder

/**
 * Carries photo files through the sync folder, as `blobs/<sha256>` beside `devices/` (R56): the folder only carries
 * them, and each device keeps its own copy in [store]. A file is referenced while a `photo` row names its hash. A copy
 * counts as good only when its bytes hash to its name.
 *
 * - **Publish**: every referenced file held here undamaged and missing from the folder (or there with another size)
 *   is written there; so a referenced file collected or damaged in the folder comes back from any device holding it.
 * - **Fetch**: every referenced file this device lacks is read from the folder, and kept only when it hashes to its
 *   name.
 * - **Collect**: the folder's copy goes once this device has seen it unreferenced for 30 days, so a photo whose row
 *   arrives up to 30 days after its file is not left without it. This device's copy goes once no photo names it, for
 *   as long. Syncthing's conflict copies go at once: a file's name is its bytes' hash, so the copy is the same file or
 *   a damaged one.
 *
 * A known limit: a device away longer than the 30-day grace period can lose a file the others collected, and gets it
 * back only from a device that still holds it.
 */
internal class BlobSync(
    db: ChatelaineDatabase,
    private val folder: SyncFolder,
    private val store: BlobStore,
    private val wallMillis: () -> Long,
) {
    private val photos = db.photoDao()
    private val dao = db.blobDao()

    suspend fun exchange() {
        val referenced = photos.photoHashes().filter { BlobName.isValid(it) }.toSet()
        val listed = folder.list(BLOBS)
        listed.filter { ConflictCopies.isCopy(it) }.forEach { folder.delete("$BLOBS/$it") }
        val inFolder = listed.filter { BlobName.isValid(it) }.toSet()
        val held = store.list().filter { BlobName.isValid(it) }.toMutableSet()
        for (name in referenced - held) {
            if (name !in inFolder) continue
            val bytes = folder.read("$BLOBS/$name") ?: continue
            if (sha256Hex(bytes) != name) continue
            store.write(name, bytes)
            held += name
        }
        for (name in referenced intersect held) {
            val size = store.size(name) ?: continue
            if (name in inFolder && folder.size("$BLOBS/$name") == size) continue
            val bytes = store.read(name) ?: continue
            if (sha256Hex(bytes) != name) {
                store.delete(name)
                held -= name
            } else {
                folder.replace("$BLOBS/$name", bytes)
            }
        }

        val now = wallMillis()
        val seen = dao.seen().associateBy { it.name }
        val next = mutableListOf<BlobSeenEntity>()
        val cleared = mutableListOf<String>()
        for (name in inFolder + held) {
            val was = seen[name]
            val folderSince = if (name in referenced || name !in inFolder) null else was?.folderSince ?: now
            val localSince = if (name in referenced || name !in held) null else was?.localSince ?: now
            if (folderSince != null && now - folderSince >= GRACE_MS) folder.delete("$BLOBS/$name")
            if (localSince != null && now - localSince >= GRACE_MS) store.delete(name)
            if (folderSince == null && localSince == null) cleared += name else next += BlobSeenEntity(name, folderSince, localSince)
        }
        dao.putSeen(next)
        dao.clearSeen(cleared + (seen.keys - inFolder - held))
    }

    private companion object {
        const val BLOBS = "blobs"
        const val GRACE_MS = 30L * 24 * 60 * 60 * 1000
    }
}
