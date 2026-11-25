package com.hazia.chatelaine

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** R60: with no backup folder picked the daily work does nothing. */
class BackupWorkerTest {

    @Test
    fun doesNothingWithoutAFolder() {
        assertNull(runBlocking { runBackup({ null }, nowMillis = 1_000L) })
    }

    @Test
    fun noFolderIsPickedUntilTheAppSetsOne() {
        assertNull(BackupWorker.job())
        assertTrue(BackupWorker.UNIQUE_NAME.isNotEmpty())
    }
}
