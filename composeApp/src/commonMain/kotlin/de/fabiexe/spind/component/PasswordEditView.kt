package de.fabiexe.spind.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
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
import de.fabiexe.spind.composeapp.generated.resources.*
import de.fabiexe.spind.data.Password
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PasswordEditView(
    password: Password?,
    onChange: suspend CoroutineScope.(Password) -> Unit,
    close: () -> Unit
) {
    val coroutineScope = remember { CoroutineScope(Dispatchers.Default) }
    var processing by remember { mutableStateOf(false) }
    var name by remember(password) { mutableStateOf(password?.name ?: "") }
    var newFieldName by remember(password) { mutableStateOf("") }
    var fields by remember(password) { mutableStateOf(password?.fields ?: mapOf("password" to "")) }
    val scrollState = rememberScrollState()

    fun submit() = coroutineScope.launch {
        processing = true
        onChange(Password(name, fields))
        close()
        processing = false
    }

    DisposableEffect(Unit) {
        onDispose {
            coroutineScope.cancel()
        }
    }

    Column(
        modifier = Modifier
            .padding(12.dp)
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (password == null) {
            Text(stringResource(Res.string.PasswordEditView_title_add), style = MaterialTheme.typography.titleLarge)
        } else {
            Text(stringResource(Res.string.PasswordEditView_title_edit), style = MaterialTheme.typography.titleLarge)
        }

        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .weight(1f)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = !processing,
                label = { Text(stringResource(Res.string.PasswordEditView_input_name)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                fun submit() {
                    if (newFieldName.isBlank() || newFieldName in fields) {
                        return
                    }

                    val newFields = fields.toMutableMap()
                    newFields[newFieldName] = ""
                    fields = newFields
                    newFieldName = ""
                }

                OutlinedTextField(
                    value = newFieldName,
                    onValueChange = { newFieldName = it },
                    enabled = !processing,
                    label = { Text(stringResource(Res.string.PasswordEditView_input_addField)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    singleLine = true
                )
                IconButton(
                    onClick = ::submit,
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                }
            }

            var i = 0
            for ((key, value) in fields) {
                if (key == "password") {
                    continue
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value ->
                            val newFields = fields.toMutableMap()
                            newFields[key] = value
                            fields = newFields
                        },
                        enabled = !processing,
                        label = { Text(key) },
                        keyboardOptions = KeyboardOptions(
                            autoCorrectEnabled = false,
                            keyboardType = if (key.equals("email", ignoreCase = true)) {
                                KeyboardType.Password
                            } else {
                                KeyboardType.Text
                            },
                            imeAction = if (i == fields.size - 1) {
                                ImeAction.Done
                            } else {
                                ImeAction.Next
                            }
                        ),
                        singleLine = true
                    )
                    IconButton(
                        onClick = { fields -= key },
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(imageVector = Icons.Outlined.Remove, contentDescription = null)
                    }
                }
                i++
            }

            val password = fields["password"]
            if (password != null) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password ->
                        val newFields = fields.toMutableMap()
                        newFields["password"] = password
                        fields = newFields
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !processing,
                    label = { Text(stringResource(Res.string.PasswordEditView_input_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true
                )
                TextButton(
                    onClick = {
                        val chars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
                        val randomPassword = (1..16).joinToString("") { chars.random().toString() }
                        val newFields = fields.toMutableMap()
                        newFields["password"] = randomPassword
                        fields = newFields
                    },
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Text(stringResource(Res.string.PasswordEditView_button_randomPassword))
                }
            }
        }

        if (processing) {
            LoadingIndicator()
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    onClick = close,
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Text(stringResource(Res.string.PasswordEditView_button_cancel))
                }
                Button(
                    onClick = ::submit,
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                    enabled = !processing
                ) {
                    Text(stringResource(Res.string.PasswordEditView_button_save))
                }
            }
        }
    }
}