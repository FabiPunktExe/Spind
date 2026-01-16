package de.fabiexe.spind

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import io.ktor.client.engine.js.*

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        App(WebStorage, Js)
    }
}