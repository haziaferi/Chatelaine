package com.hazia.chatelaine.data.sync

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

/** A [SyncFolder] on a directory the app can open as a [Path]: Windows, and Android's own storage. */
class DirectorySyncFolder(private val root: Path) : SyncFolder {

    override fun list(dir: String): List<String> =
        root.resolve(dir).takeIf { it.isDirectory() }?.listDirectoryEntries()?.map { it.name }.orEmpty()

    override fun size(path: String): Long? = root.resolve(path).takeIf { it.isRegularFile() }?.let(Files::size)

    override fun read(path: String): ByteArray? = root.resolve(path).takeIf { it.isRegularFile() }?.let(Files::readAllBytes)

    override fun replace(path: String, bytes: ByteArray) {
        val target = root.resolve(path)
        val dir = requireNotNull(target.parent) { "no directory for $path" }.also { Files.createDirectories(it) }
        val tmp = dir.resolve(TEMP_PREFIX + target.name)
        // Forced to disk before the move: a crash then leaves the old file or the whole new one, never an empty one.
        FileChannel.open(tmp, CREATE, TRUNCATE_EXISTING, WRITE).use { channel ->
            channel.write(ByteBuffer.wrap(bytes))
            channel.force(true)
        }
        Files.move(tmp, target, ATOMIC_MOVE, REPLACE_EXISTING)
    }

    override fun delete(path: String) {
        Files.deleteIfExists(root.resolve(path))
    }
}
