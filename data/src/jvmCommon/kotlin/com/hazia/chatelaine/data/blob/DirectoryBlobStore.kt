package com.hazia.chatelaine.data.blob

import com.hazia.chatelaine.data.sync.TEMP_PREFIX
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

/** A [BlobStore] in a directory of the app's own storage; a file is written whole or not at all. */
class DirectoryBlobStore(private val root: Path) : BlobStore {

    override fun read(name: String): ByteArray? = file(name).takeIf { it.isRegularFile() }?.let(Files::readAllBytes)

    override fun write(name: String, bytes: ByteArray) {
        val target = file(name)
        Files.createDirectories(root)
        val tmp = root.resolve(TEMP_PREFIX + name)
        Files.write(tmp, bytes)
        Files.move(tmp, target, ATOMIC_MOVE, REPLACE_EXISTING)
    }

    override fun size(name: String): Long? = file(name).takeIf { it.isRegularFile() }?.let(Files::size)

    override fun delete(name: String) {
        Files.deleteIfExists(file(name))
    }

    override fun list(): List<String> =
        root.takeIf { it.isDirectory() }?.listDirectoryEntries()?.map { it.name }?.filter { BlobName.isValid(it) }.orEmpty()

    /** Only a name this app writes reaches the disk, so no name walks out of [root]. */
    private fun file(name: String): Path = root.resolve(name.also { require(BlobName.isValid(it)) { "not a blob name: $it" } })
}
