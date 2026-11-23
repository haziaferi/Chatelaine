package app.chatelaine.spike

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Chatelaine KMP spike") {
        SpikeScreen(platform = "${System.getProperty("os.name")} (JVM ${System.getProperty("java.version")})")
    }
}
