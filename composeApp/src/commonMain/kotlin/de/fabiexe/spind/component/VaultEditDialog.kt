package de.fabiexe.spind.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import de.fabiexe.spind.composeapp.generated.resources.*
import de.fabiexe.spind.data.Vault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Stable
class VaultEditDialogState(val vault: Vault?) {
    var processing by mutableStateOf(false)
    var name by mutableStateOf(vault?.name ?: "")
    var address by mutableStateOf(vault?.address ?: "")
    var username by mutableStateOf(vault?.username ?: "")
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VaultEditDialog(
    state: VaultEditDialogState,
    onChange: suspend (Vault) -> Unit,
    onClose: () -> Unit
) {
    val coroutineScope = remember { CoroutineScope(Dispatchers.Default) }

    fun submit() = coroutineScope.launch {
        state.processing = true
        onChange(Vault(state.name, state.address, state.username))
        state.processing = false
        onClose()
    }

    DisposableEffect(Unit) {
        onDispose(coroutineScope::cancel)
    }

    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            if (state.processing) {
                LoadingIndicator()
            } else {
                TextButton(
                    onClick = ::submit,
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Text(stringResource(Res.string.VaultEditDialog_button_save))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onClose,
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
            ) {
                Text(stringResource(Res.string.VaultEditDialog_button_cancel))
            }
        },
        icon = { Icon(imageVector = Icons.Outlined.Storage, contentDescription = null) },
        title = {
            if (state.vault == null) {
                Text(stringResource(Res.string.VaultEditDialog_title_add))
            } else {
                Text(stringResource(Res.string.VaultEditDialog_title_edit))
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { state.name = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.processing,
                    label = { Text(stringResource(Res.string.VaultEditDialog_input_name)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.address,
                    onValueChange = { state.address = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.processing,
                    label = { Text(stringResource(Res.string.VaultEditDialog_input_address)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.username,
                    onValueChange = { state.username = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.processing,
                    label = { Text(stringResource(Res.string.VaultEditDialog_input_username)) },
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    singleLine = true
                )
            }
        }
    )
}