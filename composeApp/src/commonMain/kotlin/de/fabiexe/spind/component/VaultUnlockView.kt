package de.fabiexe.spind.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import de.fabiexe.spind.ApiError
import de.fabiexe.spind.Either
import de.fabiexe.spind.api.SpindApi
import de.fabiexe.spind.composeapp.generated.resources.Res
import de.fabiexe.spind.composeapp.generated.resources.VaultUnlockView_button_unlock
import de.fabiexe.spind.composeapp.generated.resources.VaultUnlockView_input_password
import de.fabiexe.spind.composeapp.generated.resources.VaultUnlockView_title
import de.fabiexe.spind.data.UnlockedVault
import de.fabiexe.spind.data.Vault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Stable
class VaultUnlockViewState(val vault: Vault) {
    var vaultSecurityDialogState by mutableStateOf<VaultSecurityDialogState?>(null)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VaultUnlockView(
    api: SpindApi,
    state: VaultUnlockViewState,
    onUnlock: suspend (UnlockedVault) -> Unit
) {
    val coroutineScope = remember { CoroutineScope(Dispatchers.Default) }
    var processing by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }

    fun submit() = coroutineScope.launch {
        if (password.isBlank()) {
            return@launch
        }

        processing = true
        when (val result = api.unlockVaultUsingPassword(state.vault, password)) {
            is Either.Left -> onUnlock(result.value)
            is Either.Right -> {
                if (result.value.error == ApiError.VAULT_NOT_INITIALIZED) {
                    state.vaultSecurityDialogState = VaultSecurityDialogState(null)
                } else {
                    // TODO: Show error to user
                    println(result.value)
                }
            }
        }
        processing = false
    }

    DisposableEffect(Unit) {
        onDispose(coroutineScope::cancel)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(Res.string.VaultUnlockView_title, state.vault.name))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            enabled = !processing,
            label = { Text(stringResource(Res.string.VaultUnlockView_input_password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            singleLine = true
        )
        if (processing) {
            LoadingIndicator()
        } else {
            Button(
                onClick = ::submit,
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
            ) {
                Text(stringResource(Res.string.VaultUnlockView_button_unlock))
            }
        }
    }

    if (state.vaultSecurityDialogState != null) {
        VaultSecurityDialog(
            api = api,
            state = state.vaultSecurityDialogState!!,
            onComplete = onUnlock,
            onClose = { state.vaultSecurityDialogState = null },
            vaultAddress = state.vault.address,
            vaultUsername = state.vault.username
        )
    }
}