package de.fabiexe.spind

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.ktor.client.engine.apache5.*
import java.awt.Toolkit

fun main() {
    application {
        val dimension = Toolkit.getDefaultToolkit().screenSize
        Window(
            onCloseRequest = ::exitApplication,
            state = rememberWindowState(
                size = DpSize(dimension.width.dp * 2/3, dimension.height.dp * 2/3)
            ),
            title = "Spind"
        ) {
            App(JvmStorage, Apache5)
        }
    }
}