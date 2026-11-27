package com.hazia.chatelaine

import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The phone UI test path works under Robolectric: a composable's test tag can be found.
 * Debug only: the test activity comes from ui-test-manifest, which only the debug build has.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ComposeSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aTaggedTextIsFound() {
        compose.setContent { BasicText("x", Modifier.testTag("t")) }
        compose.onNodeWithTag("t").assertExists()
    }
}
