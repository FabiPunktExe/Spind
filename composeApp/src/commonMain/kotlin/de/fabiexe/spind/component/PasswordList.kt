package de.fabiexe.spind.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import de.fabiexe.spind.composeapp.generated.resources.PasswordsView_title
import de.fabiexe.spind.composeapp.generated.resources.Res
import de.fabiexe.spind.data.PasswordGroup
import de.fabiexe.spind.data.UnlockedVault
import org.jetbrains.compose.resources.stringResource

@Composable
fun PasswordList(
    vault: UnlockedVault,
    scrollState: ScrollState,
    selectedPassword: Int?,
    selectPassword: (Int?) -> Unit,
    addPassword: () -> Unit,
    editPassword: (Int) -> Unit,
    deletePassword: (Int) -> Unit
) {
    Column(
        modifier = Modifier.padding(12.dp).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(Res.string.PasswordsView_title),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.titleLarge
        )
        Column(Modifier.weight(1f).verticalScroll(scrollState)) {
            PasswordGroupView(
                group = vault.passwords,
                absoluteIndex = 0,
                selectedPassword = selectedPassword,
                selectPassword = selectPassword,
                editPassword = editPassword,
                deletePassword = deletePassword
            )
        }
        FloatingActionButton(
            onClick = addPassword,
            modifier = Modifier.align(Alignment.End).pointerHoverIcon(PointerIcon.Hand)
        ) {
            Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
        }
    }
}

@Composable
private fun PasswordGroupView(
    group: PasswordGroup,
    absoluteIndex: Int,
    selectedPassword: Int?,
    selectPassword: (Int?) -> Unit,
    editPassword: (Int) -> Unit,
    deletePassword: (Int) -> Unit
) {
    var absoluteIndex = absoluteIndex

    for ((relativeIndex, subGroup) in group.groups.withIndex()) {
        val currentAbsoluteIndex = absoluteIndex
        val interactionSource = remember { MutableInteractionSource() }
        val hovered by interactionSource.collectIsHoveredAsState()
        var open by remember { mutableStateOf(false) }
        NavigationDrawerItem(
            label = { Text(subGroup.name) },
            selected = false,
            onClick = { open = !open },
            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
            icon = {
                if (open) {
                    Icon(imageVector = Icons.Outlined.ExpandLess, contentDescription = null)
                } else {
                    Icon(imageVector = Icons.Outlined.ExpandMore, contentDescription = null)
                }
            },
            badge = {
                if (hovered) {
                    Row {
                        // TODO: Edit button
                        // TODO: Delete button
                    }
                }
            },
            interactionSource = interactionSource
        )
        AnimatedVisibility(open) {
            Box(Modifier.padding(start = 24.dp)) {
                PasswordGroupView(
                    group = subGroup,
                    absoluteIndex = currentAbsoluteIndex,
                    selectedPassword = selectedPassword,
                    selectPassword = selectPassword,
                    editPassword = editPassword,
                    deletePassword = deletePassword
                )
            }
        }
        absoluteIndex += subGroup.totalSize
    }

    for (password in group.passwords) {
        val currentAbsoluteIndex = absoluteIndex
        val interactionSource = remember { MutableInteractionSource() }
        val hovered by interactionSource.collectIsHoveredAsState()
        NavigationDrawerItem(
            label = { Text(password.name) },
            selected = selectedPassword == currentAbsoluteIndex,
            onClick = { selectPassword(currentAbsoluteIndex) },
            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
            badge = {
                if (hovered) {
                    Row {
                        IconButton(
                            onClick = { editPassword(currentAbsoluteIndex) },
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Icon(imageVector = Icons.Outlined.Edit, contentDescription = null)
                        }
                        IconButton(
                            onClick = { deletePassword(currentAbsoluteIndex) },
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
                        }
                    }
                }
            },
            interactionSource = interactionSource
        )
        absoluteIndex++
    }
}