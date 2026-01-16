package de.fabiexe.spind.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import de.fabiexe.spind.composeapp.generated.resources.Res
import de.fabiexe.spind.composeapp.generated.resources.VaultsView_title
import de.fabiexe.spind.data.Vault
import org.jetbrains.compose.resources.stringResource

enum class VaultListDialog {
    Add, Edit, Remove
}

@Stable
class VaultListState {
    val scrollState = ScrollState(0)
    var dialog by mutableStateOf<VaultListDialog?>(null)
    var vaultEditDialogState by mutableStateOf<VaultEditDialogState?>(null)
}

@Composable
fun VaultList(
    state: VaultListState,
    vaults: List<Vault>,
    onChangeVaults: suspend (List<Vault>) -> Unit,
    selectedVault: Int?,
    onChangeSelectedVault: (Int?) -> Unit
) {
    Column(
        modifier = Modifier.padding(12.dp).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(Res.string.VaultsView_title),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.titleLarge
        )
        Column(Modifier.weight(1f).verticalScroll(state.scrollState)) {
            for ((index, vault) in vaults.withIndex()) {
                val interactionSource = remember { MutableInteractionSource() }
                val hovered by interactionSource.collectIsHoveredAsState()
                NavigationDrawerItem(
                    label = { Text(vault.name) },
                    selected = selectedVault == index,
                    onClick = { onChangeSelectedVault(index) },
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                    badge = {
                        if (hovered) {
                            Row {
                                IconButton(
                                    onClick = {
                                        onChangeSelectedVault(index)
                                        state.dialog = VaultListDialog.Edit
                                    },
                                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                                ) {
                                    Icon(imageVector = Icons.Outlined.Edit, contentDescription = null)
                                }
                                IconButton(
                                    onClick = {
                                        onChangeSelectedVault(index)
                                        state.dialog = VaultListDialog.Remove
                                    },
                                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                                ) {
                                    Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
                                }
                            }
                        }
                    },
                    interactionSource = interactionSource
                )
            }
        }
        FloatingActionButton(
            onClick = { state.dialog = VaultListDialog.Add },
            modifier = Modifier.align(Alignment.End).pointerHoverIcon(PointerIcon.Hand)
        ) {
            Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
        }
    }

    when (state.dialog) {
        VaultListDialog.Add -> {
            VaultEditDialog(
                state = state.vaultEditDialogState!!,
                onChange = { newVault ->
                    val newVaults = vaults + newVault
                    onChangeVaults(newVaults)
                    onChangeSelectedVault(newVaults.size - 1)
                },
                onClose = { state.dialog = null }
            )
        }
        VaultListDialog.Edit -> {
            VaultEditDialog(
                state = state.vaultEditDialogState!!,
                onChange = { newVault ->
                    val newVaults = vaults.toMutableList()
                    newVaults[selectedVault!!] = newVault
                    onChangeVaults(newVaults)
                },
                onClose = { state.dialog = null }
            )
        }
        VaultListDialog.Remove -> {
            VaultRemoveDialog(
                vault = vaults[selectedVault!!],
                onRemove = {
                    val newVaults = vaults.toMutableList()
                    newVaults.removeAt(selectedVault)
                    onChangeVaults(newVaults)
                    onChangeSelectedVault(null)
                },
                onClose = { state.dialog = null }
            )
        }
        else -> {}
    }
}