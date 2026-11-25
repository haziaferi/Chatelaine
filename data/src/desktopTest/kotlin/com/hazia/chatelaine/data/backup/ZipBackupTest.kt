package com.hazia.chatelaine.data.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ZipBackupTest {
    private val photo = byteArrayOf(1, 2, 3)
    private val hash = java.security.MessageDigest.getInstance("SHA-256").digest(photo).joinToString("") { "%02x".format(it) }
    private val good = mapOf(
        "records.jsonl" to "r".encodeToByteArray(),
        "purges.jsonl" to "p".encodeToByteArray(),
        "blobs/$hash" to photo,
    )

    @Test
    fun theThreeKindsOfEntryRoundTrip() {
        val back = ZipBackup.unpack(ZipBackup.pack(good))
        assertEquals(good.keys, back.keys)
        good.forEach { (name, bytes) -> assertTrue(bytes.contentEquals(back.getValue(name)), name) }
    }

    @Test
    fun anEntryNameABackupNeverHoldsMeansNotABackup() {
        val wrongHash = "b".repeat(64)
        for (bad in listOf("../evil", "blobs/../../evil", "/etc/passwd", "blobs/", "blobs/${hash}x", "blobs/$wrongHash", "notes.txt", "records.jsonl/")) {
            assertEquals(emptyMap(), ZipBackup.unpack(ZipBackup.pack(good + (bad to byteArrayOf(9)))), bad)
        }
    }
}
