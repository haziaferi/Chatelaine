package com.hazia.chatelaine.data.blob

/** The SHA-256 of [bytes] in lower-case hex: a blob's name (R56). */
internal expect fun sha256Hex(bytes: ByteArray): String
