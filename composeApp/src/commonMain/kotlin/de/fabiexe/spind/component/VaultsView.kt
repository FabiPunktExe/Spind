package de.fabiexe.spind.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import de.fabiexe.spind.api.SpindApi
import de.fabiexe.spind.data.UnlockedVault
import de.fabiexe.spind.data.Vault
import de.fabiexe.spind.isMobileScreen
import kotlinx.coroutines.launch

@Stable
class VaultsViewState {
    val vaultListState = VaultListState()
    var passwordsViewState = PasswordsViewState()
    var selected by mutableStateOf<Int?>(null)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VaultsView(
    api: SpindApi,
    state: VaultsViewState,
    vaults: List<Vault>,
    onChangeVaults: suspend (List<Vault>) -> Unit,
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
                        state = state.vaultListState,
                        vaults = vaults,
                        onChangeVaults = onChangeVaults,
                        selectedVault = state.selected,
                        onChangeSelectedVault = { index ->
                            state.selected = index
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
                        state = state.vaultListState,
                        vaults = vaults,
                        onChangeVaults = onChangeVaults,
                        selectedVault = state.selected,
                        onChangeSelectedVault = { index ->
                            state.selected = index
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
    vaults: List<Vault>,
    unlockedVaults: List<UnlockedVault>,
    onChangeUnlockedVaults: suspend (List<UnlockedVault>) -> Unit
) {
    if (state.selected == null) {
        return
    }

    val vault = vaults[state.selected!!]
    val unlockedVault = unlockedVaults.firstOrNull { it.sameAddressAndUsername(vault) }
    if (unlockedVault == null) {
        VaultUnlockView(
            api = api,
            vault = vault,
            onUnlock = { newUnlockedVault ->
                onChangeUnlockedVaults(unlockedVaults + newUnlockedVault)
            }
        )
    } else {
        PasswordsView(
            state = state.passwordsViewState,
            vault = unlockedVault,
            onChange = { newVault ->
                api.uploadVault(newVault)
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