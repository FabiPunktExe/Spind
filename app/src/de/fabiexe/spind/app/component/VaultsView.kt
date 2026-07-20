package de.fabiexe.spind.app.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import de.fabiexe.spind.app.LocalSnackbarHostState
import de.fabiexe.spind.app.api.SpindApi
import de.fabiexe.spind.app.data.UnlockedVault
import de.fabiexe.spind.app.data.VaultInfo
import de.fabiexe.spind.app.isMobileScreen
import de.fabiexe.spind.app.show
import kotlinx.coroutines.launch

@Stable
class VaultsViewState {
    val vaultListState = VaultListState()
    var passwordsViewState = PasswordsViewState()
    var selected by mutableStateOf<Int?>(null)
    var vaultUnlockViewState by mutableStateOf<VaultUnlockViewState?>(null)

    fun select(index: Int?, vaults: List<VaultInfo>, unlockedVaults: List<UnlockedVault>) {
        selected = index
        vaultUnlockViewState = if (index != null && unlockedVaults.none { it.sameAddressAndUsername(vaults[index]) }) {
            VaultUnlockViewState()
        } else {
            null
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VaultsView(
    api: SpindApi,
    state: VaultsViewState,
    vaults: List<VaultInfo>,
    onChangeVaults: suspend (List<VaultInfo>) -> Unit,
    unlockedVaults: List<UnlockedVault>,
    onChangeUnlockedVaults: suspend (List<UnlockedVault>) -> Unit
) {
    if (isMobileScreen() && vaults.isNotEmpty()) {
        // Mobile
        val coroutineScope = rememberCoroutineScope()
        val drawerState = rememberDrawerState(DrawerValue.Open)
        DismissibleNavigationDrawer(
            drawerContent = {
                DismissibleDrawerSheet(
                    drawerState = drawerState,
                    drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    VaultList(
                        api = api,
                        state = state.vaultListState,
                        vaults = vaults,
                        onChangeVaults = onChangeVaults,
                        unlockedVaults = unlockedVaults,
                        onChangeUnlockedVaults = onChangeUnlockedVaults,
                        selectedVault = state.selected,
                        onChangeSelectedVault = { index ->
                            state.select(index, vaults, unlockedVaults)
                            coroutineScope.launch { drawerState.close() }
                        }
                    )
                }
            },
            drawerState = drawerState
        ) {
            VaultsViewContent(
                api = api,
                state = state,
                vaults = vaults,
                unlockedVaults = unlockedVaults,
                onChangeUnlockedVaults = onChangeUnlockedVaults
            )
        }
    } else {
        // Desktop
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(
                    modifier = if (isMobileScreen()) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                    },
                    drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    VaultList(
                        api = api,
                        state = state.vaultListState,
                        vaults = vaults,
                        onChangeVaults = onChangeVaults,
                        unlockedVaults = unlockedVaults,
                        onChangeUnlockedVaults = onChangeUnlockedVaults,
                        selectedVault = state.selected,
                        onChangeSelectedVault = { index ->
                            state.select(index, vaults, unlockedVaults)
                        }
                    )
                }
            }
        ) {
            VaultsViewContent(
                api = api,
                state = state,
                vaults = vaults,
                unlockedVaults = unlockedVaults,
                onChangeUnlockedVaults = onChangeUnlockedVaults
            )
        }
    }
}

@Composable
private fun VaultsViewContent(
    api: SpindApi,
    state: VaultsViewState,
    vaults: List<VaultInfo>,
    unlockedVaults: List<UnlockedVault>,
    onChangeUnlockedVaults: suspend (List<UnlockedVault>) -> Unit
) {
    if (state.selected == null) {
        return
    }

    val snackbarHostState = LocalSnackbarHostState.current
    val vault = vaults[state.selected!!]
    val unlockedVault = unlockedVaults.firstOrNull { it.sameAddressAndUsername(vault) }
    if (unlockedVault == null) {
        VaultUnlockView(
            api = api,
            state = state.vaultUnlockViewState!!,
            vault = vault,
            onUnlock = { unlockedVault -> onChangeUnlockedVaults(unlockedVaults + unlockedVault) }
        )
    } else {
        PasswordsView(
            state = state.passwordsViewState,
            vault = unlockedVault,
            onChange = { newVault ->
                val result = api.uploadVault(newVault)
                if (result != null) {
                    launch { result.show(snackbarHostState) }
                }
                onChangeUnlockedVaults(unlockedVaults.map { vault ->
                    if (vault.sameAddressAndUsername(newVault)) {
                        newVault
                    } else {
                        vault
                    }
                })
            }
        )
    }
}