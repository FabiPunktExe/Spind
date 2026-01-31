package de.fabiexe.spind.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import de.fabiexe.spind.LocalSnackbarHostState
import de.fabiexe.spind.api.SpindApi
import de.fabiexe.spind.composeapp.generated.resources.*
import de.fabiexe.spind.data.PasswordGroup
import de.fabiexe.spind.data.SecurityQuestion
import de.fabiexe.spind.data.UnlockedVault
import de.fabiexe.spind.data.Vault
import de.fabiexe.spind.hashSHA3256
import de.fabiexe.spind.show
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Stable
class VaultSecurityDialogState(val vault: Vault, val unlockedVault: UnlockedVault? = null) {
    var processing by mutableStateOf(false)
    var password by mutableStateOf("")
    var repeatedPassword by mutableStateOf("")
    var securityQuestions by mutableStateOf(unlockedVault?.securityQuestions ?: listOf())
    val scrollState = ScrollState(0)

    fun isValid(): Boolean {
        return password.isNotBlank() &&
                password == repeatedPassword &&
                securityQuestions.all { it.question.isNotBlank() && it.answer.isNotBlank() }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VaultSecurityDialog(
    api: SpindApi,
    state: VaultSecurityDialogState,
    onComplete: suspend (UnlockedVault) -> Unit,
    onClose: () -> Unit
) {
    val coroutineScope = remember { CoroutineScope(Dispatchers.Default) }
    val snackbarHostState = LocalSnackbarHostState.current
    val proposedQuestions = listOf(
        stringResource(Res.string.SecurityQuestion_GrandparentsHome),
        stringResource(Res.string.SecurityQuestion_PetsFirstName),
        stringResource(Res.string.SecurityQuestion_FirstCarsBrand),
        stringResource(Res.string.SecurityQuestion_FathersSecondName),
        stringResource(Res.string.SecurityQuestion_MothersSecondName),
        stringResource(Res.string.SecurityQuestion_FirstTeachersLastName),
        stringResource(Res.string.SecurityQuestion_FirstVideoGame)
    )

    fun submit() = coroutineScope.launch {
        if (!state.isValid()) {
            return@launch
        }

        state.processing = true

        val passwordHash = hashSHA3256(state.password.encodeToByteArray())
        val passwordHashStr = passwordHash.toHexString()
        val secret = hashSHA3256(passwordHash)
        val secretStr = secret.toHexString()

        // Update security stuff on server if vault exists
        val result = api.updateSecurity(
            state.vault,
            state.unlockedVault?.secret ?: "",
            secretStr,
            state.securityQuestions
        )
        if (result != null) {
            launch { result.show(snackbarHostState) }
            state.processing = false
            return@launch
        }

        val newUnlockedVault = if (state.unlockedVault != null) {
            state.unlockedVault.copy(
                passwordHash = passwordHashStr,
                secret = secretStr,
                securityQuestions = state.securityQuestions.toList()
            )
        } else {
            UnlockedVault(
                state.vault.address,
                state.vault.username,
                passwordHashStr,
                secretStr,
                PasswordGroup("", listOf(), listOf()),
                state.securityQuestions.toList()
            )
        }

        // Upload new vault
        val uploadResult = api.uploadVault(newUnlockedVault)
        if (uploadResult == null) {
            onComplete(newUnlockedVault)
            onClose()
        } else {
            launch { uploadResult.show(snackbarHostState) }
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
                    if (state.unlockedVault == null) {
                        Text(stringResource(Res.string.VaultSecurityDialog_button_complete))
                    } else {
                        Text(stringResource(Res.string.VaultSecurityDialog_button_changePassword))
                    }
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
        title = {
            if (state.unlockedVault == null) {
                Text(stringResource(Res.string.VaultSecurityDialog_title_setup))
            } else {
                Text(stringResource(Res.string.VaultSecurityDialog_title_changePassword))
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(state.scrollState),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Password
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { state.password = it },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.processing,
                        label = { Text(stringResource(Res.string.VaultSecurityDialog_input_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true
                    )

                    // Repeat password
                    OutlinedTextField(
                        value = state.repeatedPassword,
                        onValueChange = { state.repeatedPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.processing,
                        label = { Text(stringResource(Res.string.VaultSecurityDialog_input_repeatPassword)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Password,
                            imeAction = if (state.securityQuestions.isEmpty()) {
                                ImeAction.Done
                            } else {
                                ImeAction.Next
                            }
                        ),
                        keyboardActions = if (state.securityQuestions.isEmpty()) {
                            KeyboardActions(onDone = { submit() })
                        } else {
                            KeyboardActions.Default
                        },
                        singleLine = true
                    )
                }

                // Security questions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Title
                    Text(
                        text = stringResource(Res.string.VaultSecurityDialog_label_securityQuestions),
                        style = MaterialTheme.typography.titleMedium
                    )

                    for ((index, securityQuestion) in state.securityQuestions.withIndex()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                // Question (with dropdown for proposals)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    var expanded by remember { mutableStateOf(false) }
                                    val filteredProposedQuestions = proposedQuestions.filter {
                                        it.contains(securityQuestion.question, ignoreCase = true)
                                    }
                                    ExposedDropdownMenuBox(
                                        expanded = expanded,
                                        onExpandedChange = { expanded = it },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            value = securityQuestion.question,
                                            onValueChange = { question ->
                                                val securityQuestions = state.securityQuestions.toMutableList()
                                                securityQuestions[index] = securityQuestion.copy(question = question)
                                                state.securityQuestions = securityQuestions
                                                expanded = true
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                                            label = { Text(stringResource(Res.string.VaultSecurityDialog_input_securityQuestion)) },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                                        )

                                        ExposedDropdownMenu(
                                            expanded = expanded && filteredProposedQuestions.isNotEmpty(),
                                            onDismissRequest = { expanded = false }
                                        ) {
                                            for (proposal in filteredProposedQuestions) {
                                                DropdownMenuItem(
                                                    text = { Text(proposal, softWrap = true) },
                                                    onClick = {
                                                        val securityQuestions = state.securityQuestions.toMutableList()
                                                        securityQuestions[index] =
                                                            securityQuestion.copy(question = proposal)
                                                        state.securityQuestions = securityQuestions
                                                        expanded = false
                                                    },
                                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                                )
                                            }
                                        }
                                    }

                                    // Remove security question button
                                    IconButton(
                                        onClick = {
                                            val securityQuestions = state.securityQuestions.toMutableList()
                                            securityQuestions.removeAt(index)
                                            state.securityQuestions = securityQuestions
                                        },
                                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                                    ) {
                                        Icon(Icons.Outlined.Delete, contentDescription = null)
                                    }
                                }

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
                                    singleLine = false
                                )
                            }
                        }
                    }

                    // Add security question button
                    TextButton(
                        onClick = { state.securityQuestions += SecurityQuestion("", "") },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(Icons.Outlined.Add, null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(Res.string.VaultSecurityDialog_button_addSecurityQuestion))
                    }
                }
            }
        }
    )
}
