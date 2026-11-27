package com.hazia.chatelaine.ui

import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

/** The desktop UI test path works: a composable's test tag can be found. */
@OptIn(ExperimentalTestApi::class)
class ComposeSmokeTest {

    @Test
    fun aTaggedTextIsFound() = runComposeUiTest {
        setContent { BasicText("x", Modifier.testTag("t")) }
        onNodeWithTag("t").assertExists()
    }
}
