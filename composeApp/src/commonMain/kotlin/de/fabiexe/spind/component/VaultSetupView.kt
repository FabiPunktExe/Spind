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
import de.fabiexe.spind.api.SpindApi
import de.fabiexe.spind.composeapp.generated.resources.*
import de.fabiexe.spind.data.PasswordGroup
import de.fabiexe.spind.data.SecurityQuestion
import de.fabiexe.spind.data.UnlockedVault
import de.fabiexe.spind.data.Vault
import de.fabiexe.spind.hashSHA3256
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VaultSetupView(
    api: SpindApi,
    vault: Vault,
    onComplete: suspend (UnlockedVault) -> Unit
) {
    val coroutineScope = remember { CoroutineScope(Dispatchers.Default) }
    var processing by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var repeatedPassword by remember { mutableStateOf("") }
    var securityQuestions by remember { mutableStateOf(listOf<SecurityQuestion>()) }

    fun submit() = coroutineScope.launch {
        if (password.isBlank() || password != repeatedPassword) {
            return@launch
        }

        processing = true
        val passwordHash = hashSHA3256(password.encodeToByteArray()).toHexString()
        val secret = hashSHA3256(passwordHash.encodeToByteArray()).toHexString()
        val unlockedVault = UnlockedVault(
            vault.address,
            vault.username,
            passwordHash,
            secret,
            PasswordGroup("", listOf(), listOf()),
            securityQuestions
        )
        val result = api.uploadVault(unlockedVault)
        if (result == null) {
            onComplete(unlockedVault)
        } else {
            // TODO: Show error to user
            println(result)
        }
        processing = false
    }

    DisposableEffect(Unit) {
        onDispose {
            coroutineScope.cancel()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(Res.string.VaultSetupView_title))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            enabled = !processing,
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
            value = repeatedPassword,
            onValueChange = { repeatedPassword = it },
            enabled = !processing,
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
        if (processing) {
            LoadingIndicator()
        } else {
            Button(
                onClick = ::submit,
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
            ) {
                Text(stringResource(Res.string.VaultSetupView_button_complete))
            }
        }
    }
}