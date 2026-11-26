package com.hazia.chatelaine.core.sync

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Record ids (SPEC:R15): a random UUID made on the device. `Merger` takes ids as plain strings. */
object Ids {
    @OptIn(ExperimentalUuidApi::class)
    fun new(): String = Uuid.random().toString()
}
