package com.hazia.chatelaine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** The debug harness backs up into a folder of its own, never into the sync folder (R60). */
class HarnessBackupFolderTest {

    private val sync = "content://docs/tree/sync"
    private val backups = "content://docs/tree/backups"

    @Test
    fun theBackupGoesToItsOwnPickedFolder() {
        assertEquals(backups, backupFolderOf(backups, sync))
    }

    @Test
    fun noBackupFolderPickedIsRefused() {
        assertThrows(IllegalArgumentException::class.java) { backupFolderOf(null, sync) }
    }

    @Test
    fun theSyncFolderIsRefusedAsTheBackupFolder() {
        assertThrows(IllegalArgumentException::class.java) { backupFolderOf(sync, sync) }
    }
}
