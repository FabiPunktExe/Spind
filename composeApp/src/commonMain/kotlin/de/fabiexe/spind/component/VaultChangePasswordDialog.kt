package de.fabiexe.spind.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import de.fabiexe.spind.api.SpindApi
import de.fabiexe.spind.composeapp.generated.resources.*
import de.fabiexe.spind.data.UnlockedVault
import de.fabiexe.spind.hashSHA3256
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Stable
class VaultChangePasswordDialogState {
    var processing by mutableStateOf(false)
    var password by mutableStateOf("")
    var repeatedPassword by mutableStateOf("")
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VaultChangePasswordDialog(
    api: SpindApi,
    state: VaultChangePasswordDialogState,
    unlockedVault: UnlockedVault,
    onComplete: suspend (UnlockedVault) -> Unit,
    onClose: () -> Unit
) {
    val coroutineScope = remember { CoroutineScope(Dispatchers.Default) }

    fun submit() = coroutineScope.launch {
        if (state.password.isBlank() || state.password != state.repeatedPassword) {
            return@launch
        }

        state.processing = true
        val passwordHash = hashSHA3256(state.password.encodeToByteArray()).toHexString()
        val secret = hashSHA3256(passwordHash.encodeToByteArray()).toHexString()
        
        val newUnlockedVault = unlockedVault.copy(
            passwordHash = passwordHash,
            secret = secret
        )

        // Update secret
        val secretResult = api.changeSecret(unlockedVault, secret)
        if (secretResult != null) {
            // TODO: Show error
            println(secretResult)
            state.processing = false
            return@launch
        }

        // Upload re-encrypted vault
        val uploadResult = api.uploadVault(newUnlockedVault)
        if (uploadResult == null) {
            onComplete(newUnlockedVault)
            onClose()
        } else {
            // TODO: Show error
            println(uploadResult)
        }
        state.processing = false
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
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                    enabled = state.password.isNotBlank() && state.password == state.repeatedPassword
                ) {
                    Text(stringResource(Res.string.VaultChangePasswordDialog_button_change))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onClose,
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
            ) {
                Text(stringResource(Res.string.VaultChangePasswordDialog_button_cancel))
            }
        },
        icon = { Icon(imageVector = Icons.Outlined.Lock, contentDescription = null) },
        title = { Text(stringResource(Res.string.VaultChangePasswordDialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = state.password,
                    onValueChange = { state.password = it },
                    enabled = !state.processing,
                    label = { Text(stringResource(Res.string.VaultSetupView_input_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.repeatedPassword,
                    onValueChange = { state.repeatedPassword = it },
                    enabled = !state.processing,
                    label = { Text(stringResource(Res.string.VaultSetupView_input_repeatPassword)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    singleLine = true
                )
            }
        }
    )
}