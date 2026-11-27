package com.hazia.chatelaine.data

import java.io.File
import java.io.FileOutputStream
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

/**
 * Writes [bytes] to [file] through a temporary file beside it, forced to disk and then moved into place, so a crash
 * leaves the old file or the whole new one; a temporary file a failed write or move left is deleted. With [replace]
 * false an existing [file] is kept and false returned (the first writer wins).
 */
internal fun writeThroughTemp(file: File, bytes: ByteArray, replace: Boolean = true): Boolean {
    val dir = requireNotNull(file.absoluteFile.parentFile) { "no directory for ${file.path}" }.apply { mkdirs() }
    val tmp = Files.createTempFile(dir.toPath(), file.name, ".tmp")
    try {
        FileOutputStream(tmp.toFile()).use { out ->
            out.write(bytes)
            out.fd.sync()
        }
        if (replace) {
            Files.move(tmp, file.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } else {
            try {
                Files.move(tmp, file.toPath())
            } catch (_: FileAlreadyExistsException) {
                return false
            }
        }
        return true
    } finally {
        Files.deleteIfExists(tmp)
    }
}
