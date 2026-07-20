package de.fabiexe.spind.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.fabiexe.spind.app.data.AndroidVaultInfoStorage
import de.fabiexe.spind.app.data.AndroidVaultStorage
import io.ktor.client.engine.android.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(
                vaultInfoStorage = AndroidVaultInfoStorage(applicationContext),
                vaultStorage = AndroidVaultStorage(applicationContext),
                ktorEngine = Android
            )
        }
    }
}