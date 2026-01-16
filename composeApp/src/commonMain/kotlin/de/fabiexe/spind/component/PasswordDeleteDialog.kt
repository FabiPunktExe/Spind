package de.fabiexe.spind.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import de.fabiexe.spind.composeapp.generated.resources.PasswordDeleteDialog_button_cancel
import de.fabiexe.spind.composeapp.generated.resources.PasswordDeleteDialog_button_delete
import de.fabiexe.spind.composeapp.generated.resources.PasswordDeleteDialog_message
import de.fabiexe.spind.composeapp.generated.resources.Res
import de.fabiexe.spind.data.Password
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PasswordDeleteDialog(
    password: Password,
    onDelete: suspend () -> Unit,
    onClose: () -> Unit
) {
    val coroutineScope = remember { CoroutineScope(Dispatchers.Default) }
    var processing by remember { mutableStateOf(false) }

    fun submit() = coroutineScope.launch {
        processing = true
        onDelete()
        processing = false
        onClose()
    }

    DisposableEffect(Unit) {
        onDispose(coroutineScope::cancel)
    }

    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            if (processing) {
                LoadingIndicator()
            } else {
                TextButton(
                    onClick = ::submit,
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Text(stringResource(Res.string.PasswordDeleteDialog_button_delete))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onClose,
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
            ) {
                Text(stringResource(Res.string.PasswordDeleteDialog_button_cancel))
            }
        },
        icon = { Icon(imageVector = Icons.Outlined.Delete, contentDescription = null) },
        text = { Text(stringResource(Res.string.PasswordDeleteDialog_message, password.name)) }
    )
}