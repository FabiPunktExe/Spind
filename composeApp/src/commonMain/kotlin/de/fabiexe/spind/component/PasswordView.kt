package de.fabiexe.spind.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import de.fabiexe.spind.composeapp.generated.resources.PasswordView_button_copyPassword
import de.fabiexe.spind.composeapp.generated.resources.PasswordView_title
import de.fabiexe.spind.composeapp.generated.resources.Res
import de.fabiexe.spind.data.Password
import de.fabiexe.spind.toClipEntry
import kotlinx.coroutines.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun PasswordView(password: Password) {
    val coroutineScope = remember { CoroutineScope(Dispatchers.Default) }
    val clipboard = LocalClipboard.current

    DisposableEffect(Unit) {
        onDispose(coroutineScope::cancel)
    }

    Column(
        modifier = Modifier.padding(12.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = stringResource(Res.string.PasswordView_title, password.name),
            style = MaterialTheme.typography.titleLarge
        )

        if ((password.fields.keys - "password").isNotEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                for ((key, value) in password.fields) {
                    if (key == "password") {
                        continue
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = value,
                            onValueChange = {},
                            enabled = false,
                            label = { Text(key) },
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    clipboard.setClipEntry(value.toClipEntry())
                                }
                            },
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = null)
                        }
                    }
                }
            }
        }

        val passwordValue = password.fields["password"]
        if (passwordValue != null) {
            var job by remember { mutableStateOf<Job?>(null) }
            var copied by remember { mutableStateOf(false) }
            Button(
                onClick = {
                    job?.cancel()
                    job = coroutineScope.launch {
                        clipboard.setClipEntry(passwordValue.toClipEntry())
                        copied = true
                        delay(3000)
                        copied = false
                        job = null
                    }
                },
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
            ) {
                Text(stringResource(Res.string.PasswordView_button_copyPassword))
                AnimatedVisibility(copied) {
                    Icon(imageVector = Icons.Outlined.Check, contentDescription = null)
                }
            }
        }
    }
}