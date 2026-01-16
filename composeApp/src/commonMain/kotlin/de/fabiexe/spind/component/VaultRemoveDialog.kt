package de.fabiexe.spind.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import de.fabiexe.spind.composeapp.generated.resources.Res
import de.fabiexe.spind.composeapp.generated.resources.VaultRemoveDialog_button_cancel
import de.fabiexe.spind.composeapp.generated.resources.VaultRemoveDialog_button_remove
import de.fabiexe.spind.composeapp.generated.resources.VaultRemoveDialog_message
import de.fabiexe.spind.data.Vault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun VaultRemoveDialog(
    vault: Vault,
    onRemove: suspend () -> Unit,
    onClose: () -> Unit
) {
    val coroutineScope = remember { CoroutineScope(Dispatchers.Default) }
    var processing by mutableStateOf(false)

    fun submit() = coroutineScope.launch {
        processing = true
        onRemove()
        processing = false
        onClose()
    }

    DisposableEffect(Unit) {
        onDispose(coroutineScope::cancel)
    }

    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(
                onClick = ::submit,
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                enabled = !processing
            ) {
                Text(stringResource(Res.string.VaultRemoveDialog_button_remove))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onClose,
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
            ) {
                Text(stringResource(Res.string.VaultRemoveDialog_button_cancel))
            }
        },
        icon = { Icon(imageVector = Icons.Outlined.Delete, contentDescription = null) },
        text = { Text(stringResource(Res.string.VaultRemoveDialog_message, vault.name)) }
    )
}