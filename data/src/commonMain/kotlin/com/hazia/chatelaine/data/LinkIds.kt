package com.hazia.chatelaine.data

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** The namespace of the ids derived from a pair of ids (chosen once, random). */
private const val LINK_NAMESPACE = "5d0c0f2e-8d3b-4b8e-9a52-2f6d7a1c4e90"

/**
 * The id of the link between an item and a tag, the same on every device: one row per pair, however many
 * devices tag it and however often it is untagged and tagged again.
 */
internal fun linkId(itemId: String, tagId: String): String = uuidV5(LINK_NAMESPACE, "$itemId|$tagId")

/** A name-based (version 5, SHA-1) UUID, RFC 4122. */
@OptIn(ExperimentalUuidApi::class)
internal fun uuidV5(namespace: String, name: String): String {
    val digest = sha1(Uuid.parse(namespace).toByteArray() + name.encodeToByteArray())
    val bytes = digest.copyOf(16)
    bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x50).toByte()
    bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
    return Uuid.fromByteArray(bytes).toString()
}

private fun sha1(message: ByteArray): ByteArray {
    var h0 = 0x67452301
    var h1 = 0xEFCDAB89.toInt()
    var h2 = 0x98BADCFE.toInt()
    var h3 = 0x10325476
    var h4 = 0xC3D2E1F0.toInt()

    val bitLength = message.size.toLong() * 8
    val padded = ByteArray(((message.size + 8) / 64 + 1) * 64)
    message.copyInto(padded)
    padded[message.size] = 0x80.toByte()
    for (i in 0 until 8) padded[padded.size - 1 - i] = (bitLength ushr (8 * i)).toByte()

    val w = IntArray(80)
    for (block in padded.indices step 64) {
        for (t in 0 until 16) {
            val o = block + t * 4
            w[t] = ((padded[o].toInt() and 0xff) shl 24) or ((padded[o + 1].toInt() and 0xff) shl 16) or
                ((padded[o + 2].toInt() and 0xff) shl 8) or (padded[o + 3].toInt() and 0xff)
        }
        for (t in 16 until 80) w[t] = (w[t - 3] xor w[t - 8] xor w[t - 14] xor w[t - 16]).rotateLeft(1)
        var a = h0
        var b = h1
        var c = h2
        var d = h3
        var e = h4
        for (t in 0 until 80) {
            val (f, k) = when {
                t < 20 -> ((b and c) or (b.inv() and d)) to 0x5A827999
                t < 40 -> (b xor c xor d) to 0x6ED9EBA1
                t < 60 -> ((b and c) or (b and d) or (c and d)) to 0x8F1BBCDC.toInt()
                else -> (b xor c xor d) to 0xCA62C1D6.toInt()
            }
            val temp = a.rotateLeft(5) + f + e + k + w[t]
            e = d
            d = c
            c = b.rotateLeft(30)
            b = a
            a = temp
        }
        h0 += a
        h1 += b
        h2 += c
        h3 += d
        h4 += e
    }
    val out = ByteArray(20)
    intArrayOf(h0, h1, h2, h3, h4).forEachIndexed { i, h ->
        for (j in 0 until 4) out[i * 4 + j] = (h ushr (24 - 8 * j)).toByte()
    }
    return out
}
