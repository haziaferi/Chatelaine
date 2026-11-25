package com.hazia.chatelaine.data.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ZipBackupTest {
    private val hash = "a".repeat(64)
    private val good = mapOf(
        "records.jsonl" to "r".encodeToByteArray(),
        "purges.jsonl" to "p".encodeToByteArray(),
        "blobs/$hash" to byteArrayOf(1, 2, 3),
    )

    @Test
    fun theThreeKindsOfEntryRoundTrip() {
        val back = ZipBackup.unpack(ZipBackup.pack(good))
        assertEquals(good.keys, back.keys)
        good.forEach { (name, bytes) -> assertTrue(bytes.contentEquals(back.getValue(name)), name) }
    }

    @Test
    fun anEntryNameABackupNeverHoldsMeansNotABackup() {
        for (bad in listOf("../evil", "blobs/../../evil", "/etc/passwd", "blobs/", "blobs/${hash}x", "notes.txt", "records.jsonl/")) {
            assertEquals(emptyMap(), ZipBackup.unpack(ZipBackup.pack(good + (bad to byteArrayOf(9)))), bad)
        }
    }
}
