package com.hazia.chatelaine.data

import java.io.File

/**
 * Renames [dbPath] and its `-journal`, `-wal` and `-shm` files to `<name>.unopenable`, so the new
 * database is never opened beside the old one's journal, and returns where the database file went.
 * A name already taken by an earlier set-aside file gets `-<stamp>` after it, so no earlier file is
 * replaced. The database file moves last: if a rename fails, it is still in place and the next
 * start tries again.
 */
fun setAsideDatabaseFiles(dbPath: String, stamp: Long): File {
    val database = File(dbPath)
    val suffix = if (File(dbPath + UNOPENABLE_SUFFIX).exists()) "$UNOPENABLE_SUFFIX-$stamp" else UNOPENABLE_SUFFIX
    for (extra in listOf("-journal", "-wal", "-shm")) {
        val file = File(dbPath + extra)
        if (file.exists()) setAside(file, suffix)
    }
    return setAside(database, suffix)
}

/** Renames [file] to `<name><suffix>`; it is kept, never deleted. */
internal fun setAside(file: File, suffix: String): File {
    val target = File(file.path + suffix)
    check(!target.exists() && file.renameTo(target)) { "could not set aside ${file.path}" }
    return target
}
