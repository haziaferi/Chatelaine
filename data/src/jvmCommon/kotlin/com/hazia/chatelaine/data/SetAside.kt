package com.hazia.chatelaine.data

import java.io.File

private val SIDE_FILES = listOf("-journal", "-wal", "-shm")

/**
 * Renames [dbPath] and its `-journal`, `-wal` and `-shm` files to `<name>.unopenable`, so the new
 * database is never opened beside the old one's journal, and returns where the database file went.
 * One suffix serves all four files and is chosen so that none of the four targets exists: first
 * `.unopenable`, then `.unopenable-<stamp>`, then `.unopenable-<stamp>-<n>`; no earlier kept file
 * is replaced. The database file moves last; if any rename fails, the files already moved are put
 * back and the failure is thrown, so the live database never loses its journal and the next start
 * tries again. [rename] is a seam for tests.
 */
fun setAsideDatabaseFiles(dbPath: String, stamp: Long, rename: (File, File) -> Boolean = File::renameTo): File {
    val names = SIDE_FILES + ""
    val suffix = sequence {
        yield(UNOPENABLE_SUFFIX)
        yield("$UNOPENABLE_SUFFIX-$stamp")
        generateSequence(1) { it + 1 }.forEach { yield("$UNOPENABLE_SUFFIX-$stamp-$it") }
    }.first { s -> names.none { File(dbPath + it + s).exists() } }

    val moved = mutableListOf<Pair<File, File>>()
    for (extra in names) {
        val from = File(dbPath + extra)
        if (!from.exists()) continue
        val to = File(from.path + suffix)
        if (!rename(from, to)) {
            moved.asReversed().forEach { (original, kept) -> rename(kept, original) }
            error("could not set aside ${from.path}")
        }
        moved += from to to
    }
    return File(dbPath + suffix)
}
