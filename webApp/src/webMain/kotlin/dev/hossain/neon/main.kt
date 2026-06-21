package dev.hossain.neon

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dev.hossain.neon.demo.NeonShowcaseApp

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        NeonShowcaseApp()
    }
}
