package de.fabiexe.spind.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import de.fabiexe.spind.data.UnlockedVault
import de.fabiexe.spind.isMobileScreen
import kotlinx.coroutines.launch

@Stable
class PasswordsViewState {
    val scrollState = ScrollState(0)
    var selectedPassword by mutableStateOf<Int?>(null)
    var editView by mutableStateOf(false)
    var deleteDialog by mutableStateOf(false)

    fun selectPassword(index: Int?) {
        selectedPassword = index
        editView = false
        deleteDialog = false
    }

    fun addPassword() {
        selectedPassword = null
        editView = true
        deleteDialog = false
    }

    fun editPassword(index: Int) {
        selectedPassword = index
        editView = true
        deleteDialog = false
    }

    fun deletePassword(index: Int) {
        selectedPassword = index
        editView = false
        deleteDialog = true
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PasswordsView(
    state: PasswordsViewState,
    vault: UnlockedVault,
    onChange: suspend (UnlockedVault) -> Unit
) {
    if (isMobileScreen() && vault.passwords.totalSize > 0) {
        // Mobile
        val coroutineScope = rememberCoroutineScope()
        val sheetState = rememberModalBottomSheetState(true) { value ->
            value != SheetValue.Hidden || state.editView || state.selectedPassword != null
        }
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                PasswordsViewContent(vault, onChange, state)
            }
            FloatingActionButton(
                onClick = { coroutineScope.launch { sheetState.expand() } },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(12.dp)
                    .pointerHoverIcon(PointerIcon.Hand)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Outlined.List, contentDescription = null)
            }
        }
        if (sheetState.isVisible || sheetState.isAnimationRunning) {
            ModalBottomSheet(
                onDismissRequest = {},
                sheetState = sheetState
            ) {
                PasswordList(
                    vault = vault,
                    scrollState = state.scrollState,
                    selectedPassword = state.selectedPassword,
                    selectPassword = { index ->
                        state.selectPassword(index)
                        coroutineScope.launch { sheetState.hide() }
                    },
                    addPassword = {
                        state.addPassword()
                        coroutineScope.launch { sheetState.hide() }
                    },
                    editPassword = { index ->
                        state.editPassword(index)
                        coroutineScope.launch { sheetState.hide() }
                    },
                    deletePassword = state::deletePassword
                )
                if (state.deleteDialog) {
                    PasswordDeleteDialog(
                        password = vault.passwords[state.selectedPassword!!],
                        onDelete = {
                            onChange(vault.copy(passwords = vault.passwords - state.selectedPassword!!))
                            state.selectedPassword = null
                        },
                        onClose = { state.deleteDialog = false }
                    )
                }
            }
        }
    } else {
        // Desktop
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(
                    modifier = if (isMobileScreen()) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                    },
                    drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    PasswordList(
                        vault = vault,
                        scrollState = state.scrollState,
                        selectedPassword = state.selectedPassword,
                        selectPassword = state::selectPassword,
                        addPassword = state::addPassword,
                        editPassword = state::editPassword,
                        deletePassword = state::deletePassword
                    )
                    if (state.deleteDialog) {
                        PasswordDeleteDialog(
                            password = vault.passwords[state.selectedPassword!!],
                            onDelete = {
                                onChange(vault.copy(passwords = vault.passwords - state.selectedPassword!!))
                                state.selectedPassword = null
                            },
                            onClose = { state.deleteDialog = false }
                        )
                    }
                }
            }
        ) {
            PasswordsViewContent(vault, onChange, state)
        }
    }
}

@Composable
private fun PasswordsViewContent(
    vault: UnlockedVault,
    onChange: suspend (UnlockedVault) -> Unit,
    state: PasswordsViewState
) {
    val selectedPassword = state.selectedPassword
    if (state.editView) {
        PasswordEditView(
            password = if (selectedPassword == null) {
                null
            } else {
                vault.passwords[selectedPassword]
            },
            onChange = { password ->
                if (selectedPassword == null) {
                    onChange(vault.copy(passwords = vault.passwords + password))
                    state.selectedPassword = vault.passwords.totalSize
                } else {
                    onChange(vault.copy(passwords = vault.passwords.set(selectedPassword, password)))
                }
            },
            close = { state.editView = false }
        )
    } else if (selectedPassword != null) {
        PasswordView(vault.passwords[selectedPassword])
    }
}