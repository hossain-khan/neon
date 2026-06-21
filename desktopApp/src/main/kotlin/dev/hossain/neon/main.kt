package dev.hossain.neon

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.hossain.neon.demo.NeonShowcaseApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "NeonProject",
    ) {
        NeonShowcaseApp()
    }
}
