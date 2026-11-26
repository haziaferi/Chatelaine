package com.hazia.chatelaine.data.sync

import android.content.Context
import android.net.Uri
import java.io.IOException

/**
 * The sync folder on a tree the person picked through the Storage Access Framework, which has no atomic
 * overwrite. So [replace] writes `.tmp-<name>` first and reads it back byte for byte; only then does it swap:
 * the current file is renamed to `.tmp-old-<name>`, the temporary file is renamed onto the name, and the
 * previous copy is deleted. The target is never deleted first, so a crash leaves the last complete content
 * somewhere: when a name is missing and its `.tmp-old-` copy exists, [list], [read], [size] and [replace]
 * rename the copy back (a copy beside a name that stands again is dropped). All of these begin with that
 * recovery, so it runs before anything can clear temporary files. [list] shows `.tmp-` files, as a directory does,
 * so leftovers can be cleared. The bytes are written as they are: encryption is a later layer.
 *
 * Blocking: call it off the main thread.
 */
class SafSyncFolder(context: Context, tree: Uri) : SyncFolder {

    private val docs = SafTree(context.applicationContext, tree)

    /** The persisted read-and-write grant exists, and the tree exists and can be written. */
    fun reachable(): Boolean = docs.reachable()

    override fun list(dir: String): List<String> {
        val id = docs.dir(segments(dir), create = false) ?: return emptyList()
        // Temporary files are listed, as a directory lists them, so the code that clears leftovers (the exporter, the
        // backup, the photo exchange) sees them; every reader ignores names outside its own patterns. A previous copy
        // cannot be among them: [recover] has dealt with each one.
        return recover(id).map { it.name }.filter { !it.startsWith(OLD_PREFIX) }
    }

    override fun size(path: String): Long? {
        val doc = find(path) ?: return null
        return doc.size ?: docs.openIn(doc.id).use { it.readBytes().size.toLong() }
    }

    override fun read(path: String): ByteArray? {
        val doc = find(path) ?: return null
        return docs.openIn(doc.id).use { it.readBytes() }
    }

    override fun replace(path: String, bytes: ByteArray) {
        val parts = segments(path)
        require(parts.isNotEmpty()) { "no file in $path" }
        val name = parts.last()
        val dirId = docs.dir(parts.dropLast(1), create = true) ?: throw IOException("A file stands where the folder of $path should be.")
        val children = recover(dirId)
        val tmpName = TEMP_PREFIX + name
        children.firstOrNull { it.name == tmpName }?.let { docs.delete(it.id) } // the new bytes supersede whatever it held
        val tmp = docs.create(dirId, tmpName)
        docs.openOut(tmp.id, "w").use { it.write(bytes) }
        if (!docs.openIn(tmp.id).use { it.readBytes() }.contentEquals(bytes)) {
            docs.delete(tmp.id)
            throw IOException(NOT_KEPT)
        }
        val current = children.firstOrNull { it.name == name }
        if (current?.isDir == true) {
            docs.delete(tmp.id)
            throw IOException("A folder stands where $name should be.")
        }
        val previous = current?.let { docs.rename(it.id, OLD_PREFIX + name) }
        docs.rename(tmp.id, name)
        previous?.let { docs.delete(it) }
    }

    override fun delete(path: String) {
        val parts = segments(path)
        if (parts.isEmpty()) return
        val dirId = docs.dir(parts.dropLast(1), create = false) ?: return
        val name = parts.last()
        docs.children(dirId).filter { it.name == name || it.name == TEMP_PREFIX + name || it.name == OLD_PREFIX + name }.forEach { docs.delete(it.id) }
    }

    /**
     * Finishes a swap a crash cut short in [dirId], and returns the directory's children as they now stand: a
     * `.tmp-old-<name>` whose name is missing is renamed back to it, one whose name stands is deleted.
     */
    private fun recover(dirId: String): List<SafTree.Doc> {
        val children = docs.children(dirId)
        val olds = children.filter { it.name.startsWith(OLD_PREFIX) }
        if (olds.isEmpty()) return children
        for (old in olds) {
            if (children.any { it.name == old.name.removePrefix(OLD_PREFIX) }) docs.delete(old.id)
            else docs.rename(old.id, old.name.removePrefix(OLD_PREFIX))
        }
        return docs.children(dirId)
    }

    /** The file at [path], or null when it or its directory is missing, or it is a directory. */
    private fun find(path: String): SafTree.Doc? {
        val parts = segments(path)
        if (parts.isEmpty()) return null
        val dirId = docs.dir(parts.dropLast(1), create = false) ?: return null
        return recover(dirId).firstOrNull { it.name == parts.last() }?.takeIf { !it.isDir }
    }

    private fun segments(path: String) = path.split('/').filter { it.isNotEmpty() }

    private companion object {
        const val OLD_PREFIX = TEMP_PREFIX + "old-"
        const val NOT_KEPT = "The sync folder did not keep the file whole."
    }
}
