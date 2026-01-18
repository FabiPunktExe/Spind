package de.fabiexe.spind

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import de.fabiexe.spind.api.SpindApi
import de.fabiexe.spind.component.VaultsView
import de.fabiexe.spind.component.VaultsViewState
import de.fabiexe.spind.data.Storage
import de.fabiexe.spind.data.UnlockedVault
import de.fabiexe.spind.data.Vault
import io.ktor.client.engine.*

val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("No SnackbarHostState provided")
}

@Stable
class AppState {
    var vaults by mutableStateOf(listOf<Vault>())
    var unlockedVaults by mutableStateOf(listOf<UnlockedVault>())
    val vaultsViewState = VaultsViewState()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun App(storage: Storage, httpClientEngineFactory: HttpClientEngineFactory<*>) {
    val state = remember { AppState() }
    val api = remember { SpindApi(httpClientEngineFactory) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        state.vaults = storage.getVaults()
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
                            storage.setVaults(newVaults)
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