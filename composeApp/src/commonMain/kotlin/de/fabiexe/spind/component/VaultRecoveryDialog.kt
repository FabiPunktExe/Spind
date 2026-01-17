package de.fabiexe.spind.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import de.fabiexe.spind.Either
import de.fabiexe.spind.api.SpindApi
import de.fabiexe.spind.composeapp.generated.resources.*
import de.fabiexe.spind.data.SecurityQuestion
import de.fabiexe.spind.data.UnlockedVault
import de.fabiexe.spind.data.Vault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Stable
class VaultRecoveryDialogState(questions: List<String>) {
    var processing by mutableStateOf(false)
    var securityQuestions by mutableStateOf(questions.map { SecurityQuestion(it, "") })
    val scrollState = ScrollState(0)

    fun isValid(): Boolean {
        return securityQuestions.all { it.answer.isNotBlank() }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VaultRecoveryDialog(
    api: SpindApi,
    state: VaultRecoveryDialogState,
    onComplete: suspend (UnlockedVault) -> Unit,
    onClose: () -> Unit,
    vaultAddress: String,
    vaultUsername: String
) {
    val coroutineScope = remember { CoroutineScope(Dispatchers.Default) }

    fun submit() = coroutineScope.launch {
        if (!state.isValid()) {
            return@launch
        }

        state.processing = true

        val vault = Vault("Spind", vaultAddress, vaultUsername)
        when (val result = api.unlockVaultUsingSecurityQuestions(vault, state.securityQuestions.map { it.answer })) {
            is Either.Left -> {
                onComplete(result.value)
                onClose()
            }
            is Either.Right -> {
                // TODO: Show error
                println(result.value)
            }
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
                    enabled = state.isValid()
                ) {
                    Text(stringResource(Res.string.VaultRecoveryDialog_button_unlock))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onClose,
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
            ) {
                Text(stringResource(Res.string.VaultSecurityDialog_button_cancel))
            }
        },
        icon = { Icon(imageVector = Icons.Outlined.Lock, contentDescription = null) },
        title = { Text(stringResource(Res.string.VaultRecoveryDialog_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(state.scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Title
                Text(
                    text = stringResource(Res.string.VaultRecoveryDialog_label_questions),
                    style = MaterialTheme.typography.titleMedium
                )

                // Security questions
                for ((index, securityQuestion) in state.securityQuestions.withIndex()) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            // Question
                            Text(
                                text = securityQuestion.question,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Answer
                            OutlinedTextField(
                                value = securityQuestion.answer,
                                onValueChange = {
                                    val securityQuestions = state.securityQuestions.toMutableList()
                                    securityQuestions[index] = securityQuestion.copy(answer = it)
                                    state.securityQuestions = securityQuestions
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(Res.string.VaultSecurityDialog_input_answer)) },
                                keyboardOptions = KeyboardOptions(
                                    autoCorrectEnabled = false,
                                    imeAction = if (index == state.securityQuestions.size - 1) {
                                        ImeAction.Done
                                    } else {
                                        ImeAction.Next
                                    }
                                ),
                                keyboardActions = if (index == state.securityQuestions.size - 1) {
                                    KeyboardActions(onDone = { submit() })
                                } else {
                                    KeyboardActions.Default
                                },
                                singleLine = true
                            )
                        }
                    }
                }
            }
        }
    )
}
