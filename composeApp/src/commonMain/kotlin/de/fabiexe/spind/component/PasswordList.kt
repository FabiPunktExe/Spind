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
import de.fabiexe.spind.composeapp.generated.resources.PasswordList_input_search
import de.fabiexe.spind.composeapp.generated.resources.PasswordList_title
import de.fabiexe.spind.composeapp.generated.resources.Res
import de.fabiexe.spind.data.Password
import de.fabiexe.spind.data.PasswordGroup
import de.fabiexe.spind.data.UnlockedVault
import de.fabiexe.spind.isMobileScreen
import org.jetbrains.compose.resources.stringResource

fun flattenPasswords(group: PasswordGroup, index: Int): List<Pair<Int, Password>> {
    val result = mutableListOf<Pair<Int, Password>>()
    var index = index
    for (subGroup in group.groups) {
        val subGroupResult = flattenPasswords(subGroup, index)
        result.addAll(subGroupResult)
        index += subGroupResult.size
    }
    for (password in group.passwords) {
        result.add(index to password)
        index++
    }
    return result
}

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
    var searchQuery by remember { mutableStateOf("") }
    val flattenedPasswords = remember(vault) {
        flattenPasswords(vault.passwords, 0)
    }
    val filteredPasswords = remember(vault, searchQuery) {
        if (searchQuery.isBlank()) {
            listOf()
        } else {
            val queryParts = searchQuery.split(" ")
                .map(String::trim)
                .filter(String::isNotBlank)
            flattenedPasswords.filter { (_, password) ->
                queryParts.all { part ->
                    password.name.contains(part, ignoreCase = true)
                }
            }
        }
    }

    Column(
        modifier = Modifier.padding(12.dp).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(Res.string.PasswordList_title),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.titleLarge
        )
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.PasswordList_input_search)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = null)
                    }
                }
            },
            singleLine = true
        )
        Column(Modifier.weight(1f).verticalScroll(scrollState)) {
            if (searchQuery.isBlank()) {
                PasswordGroupView(
                    group = vault.passwords,
                    absoluteIndex = 0,
                    selectedPassword = selectedPassword,
                    selectPassword = selectPassword,
                    editPassword = editPassword,
                    deletePassword = deletePassword
                )
            } else {
                PasswordGroupView(
                    group = PasswordGroup(
                        name = "Search Results",
                        groups = listOf(),
                        passwords = filteredPasswords.map(Pair<Int, Password>::second)
                    ),
                    absoluteIndex = 0,
                    selectedPassword = filteredPasswords.withIndex()
                        .find { (_, filteredPassword) -> filteredPassword.first == selectedPassword }
                        ?.index,
                    selectPassword = { index ->
                        if (index == null) {
                            selectPassword(null)
                        } else {
                            selectPassword(filteredPasswords[index].first)
                        }
                    },
                    editPassword = { index ->
                        editPassword(filteredPasswords[index].first)
                    },
                    deletePassword = { index ->
                        deletePassword(filteredPasswords[index].first)
                    }
                )
            }
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
                if (isMobileScreen() || hovered) {
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