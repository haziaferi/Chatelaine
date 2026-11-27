package com.hazia.chatelaine.data.app

import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileSettingsStoreTest {

    @get:Rule val tmp = TemporaryFolder()

    private val file by lazy { File(tmp.root, "$LOCAL_DIR/settings.json") }

    @Test
    fun settingsSurviveAReopen() {
        val written = runBlocking { FileSettingsStore(file).update { it.copy(syncFolder = "x") } }

        assertEquals("x", written.syncFolder)
        assertEquals("x", FileSettingsStore(file).settings.value.syncFolder)
    }

    @Test
    fun unreadableFileIsSetAsideAndDefaultsUsed() {
        file.parentFile.mkdirs()
        file.writeText("{not settings")

        val store = FileSettingsStore(file)

        assertEquals(Settings(), store.settings.value)
        assertEquals("{not settings", File(file.parentFile, "settings.json.unreadable").readText())
    }

    /** A file that cannot be read now (here a folder in its place) is not unreadable settings: it stays, and the open fails. */
    @Test
    fun aFailedReadLeavesTheFileAndThrows() {
        assertTrue(file.mkdirs())

        assertFailsWith<IOException> { FileSettingsStore(file) }
        assertTrue(file.isDirectory, "left in place")
        assertFalse(File(file.parentFile, "settings.json.unreadable").exists())
    }
}
