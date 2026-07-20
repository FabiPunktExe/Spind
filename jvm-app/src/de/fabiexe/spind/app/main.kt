package de.fabiexe.spind.app

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import de.fabiexe.spind.app.data.LocalVaultInfoStorage
import de.fabiexe.spind.app.data.CachingVaultStorage
import de.fabiexe.spind.app.data.VaultStorage
import de.fabiexe.spind.app.data.VaultInfo
import io.ktor.client.engine.apache5.*
import java.awt.Toolkit

object NoOpVaultStorage : VaultStorage {
    override fun loadRevision(url: String): Long? = null
    override fun loadData(url: String): ByteArray? = null
    override fun save(vault: VaultInfo, revision: Long, data: ByteArray) {}
}

fun main() = application {
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val windowState = rememberWindowState(
        position = WindowPosition.Aligned(Alignment.Center),
        size = DpSize(screenSize.width.dp * 2/3, screenSize.height.dp * 2/3)
    )

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Spind"
    ) {
        App(
            vaultInfoStorage = LocalVaultInfoStorage,
            vaultStorage = CachingVaultStorage(NoOpVaultStorage),
            ktorEngine = Apache5
        )
    }
}