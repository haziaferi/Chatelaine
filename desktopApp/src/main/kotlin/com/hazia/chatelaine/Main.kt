package com.hazia.chatelaine

import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.hazia.chatelaine.core.CoreInfo

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Chatelaine") {
        BasicText(CoreInfo.NAME)
    }
}
