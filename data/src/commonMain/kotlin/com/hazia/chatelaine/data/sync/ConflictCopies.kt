package com.hazia.chatelaine.data.sync

/**
 * How a folder-sync tool names the copy it keeps of the losing side when two devices wrote one file at once. This is
 * the only tool-specific code in the sync (R57): a new tool is a new entry in [PATTERNS]. Syncthing's comes first.
 * Only possible here if a device id was cloned, since each device writes only its own files.
 */
object ConflictCopies {
    val PATTERNS = listOf(".sync-conflict-")

    /** Whether the file called [name] is a tool's conflict copy. */
    fun isCopy(name: String): Boolean = PATTERNS.any { it in name }
}
