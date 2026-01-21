package ma.zer0ne.ocr

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val windowState = rememberWindowState(width = 480.dp, height = 800.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Invoice OCR",
        state = windowState,
        resizable = false,
        undecorated = true,
        transparent = true
    ) {
        App(window)
    }
}