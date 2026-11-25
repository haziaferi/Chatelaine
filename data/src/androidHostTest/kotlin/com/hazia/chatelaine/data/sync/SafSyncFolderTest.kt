package com.hazia.chatelaine.data.sync

import android.content.pm.ProviderInfo
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** The sync folder through the Storage Access Framework, on a tree of a documents provider over a plain directory. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SafSyncFolderTest {

    @get:Rule val tmp = TemporaryFolder()

    private lateinit var dir: File
    private lateinit var tree: Uri

    @Before fun setUp() {
        dir = tmp.newFolder("tree-1")
        StubDocumentsProvider.base = tmp.root
        val info = ProviderInfo().apply {
            authority = StubDocumentsProvider.AUTHORITY
            exported = true
            grantUriPermissions = true
        }
        Robolectric.buildContentProvider(StubDocumentsProvider::class.java).create(info)
        tree = DocumentsContract.buildTreeDocumentUri(StubDocumentsProvider.AUTHORITY, "tree-1")
    }

    @After fun tearDown() {
        StubDocumentsProvider.onCall = {}
    }

    private fun folder() = SafSyncFolder(RuntimeEnvironment.getApplication(), tree)

    private val path = "devices/A/log-000000.jsonl"
    private val old = "one\n".toByteArray()
    private val new = "one\ntwo\n".toByteArray()

    private fun onDisk(sub: String) = File(dir, sub).list()?.sorted().orEmpty()

    @Test
    fun replaceIsAtomicThroughATempName() {
        val folder = folder()
        val seen = mutableListOf<Pair<String, Boolean>>() // each call, and whether the final name existed when it began
        StubDocumentsProvider.onCall = { call -> seen += call to File(dir, path).exists() }

        folder.replace(path, new)

        val rename = seen.indexOfFirst { it.first.startsWith("rename ") }
        assertTrue(rename > 0, "the file arrives by a rename: $seen")
        assertTrue(seen.take(rename + 1).none { it.second }, "no file under the final name before the rename: $seen")
        assertTrue(seen.any { it.first.contains(".tmp-log-000000.jsonl") }, "the bytes go to a temporary name first: $seen")
        assertEquals(listOf("log-000000.jsonl"), onDisk("devices/A"), "no temporary file is left")
        assertContentEquals(new, folder.read(path))

        // Replacing an existing file lands whole and leaves nothing behind.
        folder.replace(path, old)
        assertContentEquals(old, folder.read(path))
        assertEquals(old.size.toLong(), folder.size(path))
        assertEquals(listOf("log-000000.jsonl"), onDisk("devices/A"))
    }

    @Test
    fun listReadAndDeleteLeaveTempFilesOut() {
        val folder = folder()
        folder.replace(path, old)
        File(dir, "devices/A/.tmp-log-000001.jsonl").writeBytes(new) // a write a crash cut short

        assertEquals(listOf("log-000000.jsonl"), folder.list("devices/A"))
        assertEquals(listOf("A"), folder.list("devices"))
        assertEquals(emptyList(), folder.list("devices/B"))
        assertNull(folder.read("devices/A/log-000001.jsonl"))
        assertNull(folder.size("devices/B/log-000000.jsonl"))

        folder.delete(path)
        folder.delete("devices/A/log-000001.jsonl")
        assertNull(folder.read(path))
        assertEquals(emptyList(), onDisk("devices/A"))
    }

    private class Crash : Error("invented crash")

    @Test
    fun aCrashInsideAnOverwriteLeavesThePreviousCompleteContent() {
        folder().replace(path, old)
        var renames = 0
        // The second rename (the temporary file onto the name) never runs: the process stops after the first.
        StubDocumentsProvider.onCall = { call -> if (call.startsWith("rename ") && ++renames == 2) throw Crash() }

        val failure = runCatching { folder().replace(path, new) }.exceptionOrNull()
        assertTrue(generateSequence(failure) { it.cause }.any { it is Crash }, "the invented crash happened: $failure")
        StubDocumentsProvider.onCall = {}
        assertFalse(File(dir, path).exists(), "the crash left no file under the name")

        val reopened = folder()
        assertContentEquals(old, reopened.read(path), "the last complete content is back")
        assertEquals(listOf("log-000000.jsonl"), reopened.list("devices/A"))
        assertEquals(old.size.toLong(), reopened.size(path))

        reopened.replace(path, new)
        assertContentEquals(new, reopened.read(path))
        assertEquals(listOf("log-000000.jsonl"), onDisk("devices/A"), "no temporary file is left")
    }

    @Test
    fun listAndSizeAlsoBringBackThePreviousContent() {
        folder().replace(path, old)
        File(dir, path).renameTo(File(dir, "devices/A/.tmp-old-log-000000.jsonl")) // as a crash between the two renames leaves it

        assertEquals(listOf("log-000000.jsonl"), folder().list("devices/A"))
        assertContentEquals(old, folder().read(path))

        File(dir, "devices/A/.tmp-old-log-000000.jsonl").let { File(dir, path).renameTo(it) }
        assertEquals(old.size.toLong(), folder().size(path))

        // A previous copy left beside a name that stands again is dropped.
        File(dir, "devices/A/.tmp-old-log-000000.jsonl").writeBytes(new)
        assertContentEquals(old, folder().read(path))
        assertEquals(listOf("log-000000.jsonl"), onDisk("devices/A"))
    }
}
