package com.hazia.chatelaine.data.app

import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
