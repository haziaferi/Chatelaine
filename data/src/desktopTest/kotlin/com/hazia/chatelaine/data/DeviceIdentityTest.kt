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

/** Decision 8: a recovered database gets a new device id, once per kept file, and a crash at any step ends with one new id and the database marked. */
class DeviceIdentityTest {

    @get:Rule val tmp = TemporaryFolder()

    private val identity by lazy { tmp.newFolder(DEVICE_ID_DIR) }
    private val idFile get() = File(identity, "device-id")
    private val renewalFile get() = File(identity, "renewal")
    private val marked = mutableListOf<String>()

    private fun renew(kept: List<String>): String =
        runBlocking { renewDeviceIdIfRecovered(identity, kept, markRecovered = { marked += it }) }

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
        val kept = listOf("chatelaine.db$UNOPENABLE_SUFFIX")

        val renewed = renew(kept)
        assertNotEquals(old, renewed)
        assertEquals(renewed, deviceId(idFile), "the new id is the one on file")
        assertEquals(listOf(old), marked)
        assertTrue(renewalMarked())

        assertEquals(renewed, renew(kept), "the same kept file renews once")
        assertEquals(listOf(old), marked)
    }

    @Test
    fun aNewKeptFileRenewsAgain() {
        val old = deviceId(idFile)
        val first = renew(listOf("chatelaine.db$UNOPENABLE_SUFFIX"))

        val second = renew(listOf("chatelaine.db$UNOPENABLE_SUFFIX", "chatelaine.db$UNOPENABLE_SUFFIX-1700000000000"))

        assertNotEquals(first, second)
        assertNotEquals(old, second)
        assertEquals(listOf(old, first), marked)
    }

    @Test
    fun crashBeforeTheNewIdIsWritten() {
        val old = deviceId(idFile)
        renewalFile.writeText("""{"old": "$old", "kept": ["chatelaine.db$UNOPENABLE_SUFFIX"], "marked": false}""")

        val renewed = renew(listOf("chatelaine.db$UNOPENABLE_SUFFIX"))

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
        renewalFile.writeText("""{"old": "$old", "kept": ["chatelaine.db$UNOPENABLE_SUFFIX"], "marked": false}""")

        val id = renew(listOf("chatelaine.db$UNOPENABLE_SUFFIX"))

        assertEquals(renewedBefore, id, "the id written before the crash stays")
        assertEquals(listOf(old), marked)
        assertTrue(renewalMarked())
    }
}
