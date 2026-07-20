package de.fabiexe.spind.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import de.fabiexe.spind.app.api.SpindApi
import de.fabiexe.spind.app.component.VaultsView
import de.fabiexe.spind.app.component.VaultsViewState
import de.fabiexe.spind.app.data.VaultInfoStorage
import de.fabiexe.spind.app.data.VaultStorage
import de.fabiexe.spind.app.data.UnlockedVault
import de.fabiexe.spind.app.data.VaultInfo
import io.ktor.client.engine.*

val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("No SnackbarHostState provided")
}

@Stable
class AppState {
    var vaults by mutableStateOf(listOf<VaultInfo>())
    var unlockedVaults by mutableStateOf(listOf<UnlockedVault>())
    val vaultsViewState = VaultsViewState()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun App(
    vaultInfoStorage: VaultInfoStorage,
    vaultStorage: VaultStorage,
    ktorEngine: HttpClientEngineFactory<*>
) {
    val state = remember { AppState() }
    val api = remember { SpindApi(ktorEngine, vaultStorage) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        state.vaults = vaultInfoStorage.load()
    }

    DynamicMaterialExpressiveTheme(
        seedColor = Color(0xFF88FF00),
        style = PaletteStyle.TonalSpot,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
        animate = true
    ) {
        CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
            Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VaultsView(
                        api = api,
                        state = state.vaultsViewState,
                        vaults = state.vaults,
                        onChangeVaults = { newVaults ->
                            vaultInfoStorage.save(newVaults)
                            state.vaults = newVaults
                        },
                        unlockedVaults = state.unlockedVaults,
                        onChangeUnlockedVaults = { newUnlockedVaults ->
                            state.unlockedVaults = newUnlockedVaults
                        }
                    )
                }
            }
        }
    }
}