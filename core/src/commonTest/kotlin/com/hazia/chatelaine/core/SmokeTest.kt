package com.hazia.chatelaine.core

import kotlin.test.Test
import kotlin.test.assertEquals

class SmokeTest {
    @Test
    fun coreKnowsItsName() {
        assertEquals("chatelaine", CoreInfo.NAME)
    }
}
