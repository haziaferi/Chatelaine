package com.hazia.chatelaine.data

import com.hazia.chatelaine.core.CoreInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class SmokeTest {
    @Test
    fun dataSeesCoreThroughItsDependency() {
        assertEquals("chatelaine", CoreInfo.NAME)
    }
}
