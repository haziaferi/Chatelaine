package com.hazia.chatelaine.data

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Decision 8: a recovered database gets a new device id, once per kept file, and a crash at any step ends with one
 * new id and the database marked; a kept file beside an established database is only noted.
 */
class DeviceIdentityTest {

    @get:Rule val tmp = TemporaryFolder()

    private val identity by lazy { tmp.newFolder(DEVICE_ID_DIR) }
    private val idFile get() = File(identity, "device-id")
    private val renewalFile get() = File(identity, "renewal")
    private val marked = mutableListOf<String>()

    private val kept = KeptFile("chatelaine.db$UNOPENABLE_SUFFIX", size = 4096, modified = 1_700_000_000_000)
    private val keptJson = """{"name": "${kept.name}", "size": ${kept.size}, "modified": ${kept.modified}}"""

    private fun renew(kept: List<KeptFile>, ownLogPending: Boolean = true): String =
        runBlocking { renewDeviceIdIfRecovered(identity, kept, ownLogPending, markRecovered = { marked += it }) }

    private fun renewalMarked(): Boolean = Json.parseToJsonElement(renewalFile.readText()).jsonObject.getValue("marked").jsonPrimitive.boolean

    @Test
    fun noKeptFileKeepsTheId() {
        val id = deviceId(idFile)

        assertEquals(id, renew(emptyList()))
        assertEquals(emptyList(), marked)
    }

    @Test
    fun keptFileRenewsOnce() {
        val old = deviceId(idFile)

        val renewed = renew(listOf(kept))
        assertNotEquals(old, renewed)
        assertEquals(renewed, deviceId(idFile), "the new id is the one on file")
        assertEquals(listOf(old), marked)
        assertTrue(renewalMarked())

        assertEquals(renewed, renew(listOf(kept)), "the same kept file renews once")
        assertEquals(listOf(old), marked)
    }

    @Test
    fun aNewKeptFileRenewsAgain() {
        val old = deviceId(idFile)
        val first = renew(listOf(kept))

        val second = renew(listOf(kept, KeptFile("chatelaine.db$UNOPENABLE_SUFFIX-1700000000000", 4096, 1_700_000_100_000)))

        assertNotEquals(first, second)
        assertNotEquals(old, second)
        assertEquals(listOf(old, first), marked)
    }

    /** The first kept file was deleted and a later corruption was kept under its name: another file. */
    @Test
    fun aReusedKeptFileNameRenewsAgain() {
        val old = deviceId(idFile)
        val first = renew(listOf(kept))

        val second = renew(listOf(kept.copy(size = 8192, modified = kept.modified + 86_400_000)))

        assertNotEquals(first, second)
        assertNotEquals(old, second)
        assertEquals(listOf(old, first), marked)
    }

    /** A kept file beside a database whose own log was read back long ago: no new id, nothing held. */
    @Test
    fun keptFileBesideAnEstablishedDatabaseOnlyNotes() {
        val id = deviceId(idFile)

        assertEquals(id, renew(listOf(kept), ownLogPending = false))
        assertEquals(emptyList(), marked)
        assertTrue(renewalMarked())

        assertEquals(id, renew(listOf(kept), ownLogPending = true), "the noted file is known: nothing to do")
        assertEquals(emptyList(), marked)
    }

    @Test
    fun crashBeforeTheNewIdIsWritten() {
        val old = deviceId(idFile)
        renewalFile.writeText("""{"old": "$old", "kept": [$keptJson], "marked": false}""")

        val renewed = renew(listOf(kept))

        assertNotEquals(old, renewed)
        assertEquals(renewed, deviceId(idFile))
        assertEquals(listOf(old), marked)
        assertTrue(renewalMarked())
    }

    @Test
    fun crashBeforeMarkingMarksOnNextStart() {
        val old = "3f0c8a52-0d5e-4d43-9f55-1b6a0e0b7c11"
        val renewedBefore = "9b2d4e61-7a3c-4f08-8e19-5c7d2a1f6e44"
        idFile.writeText(renewedBefore)
        renewalFile.writeText("""{"old": "$old", "kept": [$keptJson], "marked": false}""")

        val id = renew(listOf(kept))

        assertEquals(renewedBefore, id, "the id written before the crash stays")
        assertEquals(listOf(old), marked)
        assertTrue(renewalMarked())
    }
}
