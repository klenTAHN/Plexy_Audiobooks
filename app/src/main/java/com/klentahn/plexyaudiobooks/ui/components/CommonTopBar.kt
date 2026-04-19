package com.klentahn.plexyaudiobooks.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class MenuState {
    MAIN, VIEW, REFRESH, CONFIGURATION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonTopBar(
    title: String,
    subtitle: String? = null,
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {},
    showMenu: Boolean = false,
    onSortByAuthor: () -> Unit = {},
    onSortByBook: () -> Unit = {},
    onRefreshLibrary: () -> Unit = {},
    onRefreshMetadata: () -> Unit = {},
    onViewTiles: () -> Unit = {},
    onViewList: () -> Unit = {},
    onNavigateToBooks: () -> Unit = {},
    onNavigateToAuthors: () -> Unit = {},
    onChangeServer: () -> Unit = {},
    onChangeLibrary: () -> Unit = {},
    onSignOut: () -> Unit = {},
    showSearch: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    showChapters: Boolean = false,
    chapters: List<com.klentahn.plexyaudiobooks.ui.screens.player.Chapter> = emptyList(),
    onChapterSelected: (Int) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var menuState by remember { mutableStateOf(MenuState.MAIN) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(expanded) {
        if (!expanded) menuState = MenuState.MAIN
    }

    TopAppBar(
        title = {
            if (isSearching) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    placeholder = { Text("Search...", color = Color.Gray) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    trailingIcon = {
                        IconButton(onClick = {
                            isSearching = false
                            onSearchQueryChange("")
                        }) {
                            Icon(Icons.Rounded.Close, "Close Search", tint = Color.Gray)
                        }
                    }
                )
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "Plexy",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.Gray,
                                fontSize = 16.sp
                            ),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
        },
        navigationIcon = {
            if (canNavigateBack && !isSearching) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        actions = {
            if (showSearch && !isSearching) {
                IconButton(onClick = { isSearching = true }) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (showMenu || showChapters) {
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        when (menuState) {
                            MenuState.MAIN -> {
                                if (showMenu) {
                                    DropdownMenuItem(
                                        text = { Text("Books") },
                                        onClick = {
                                            expanded = false
                                            onNavigateToBooks()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Authors") },
                                        onClick = {
                                            expanded = false
                                            onNavigateToAuthors()
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("View As...") },
                                        trailingIcon = { Icon(Icons.Rounded.ChevronRight, null) },
                                        onClick = { menuState = MenuState.VIEW }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Refresh") },
                                        trailingIcon = { Icon(Icons.Rounded.ChevronRight, null) },
                                        onClick = { menuState = MenuState.REFRESH }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Configuration") },
                                        trailingIcon = { Icon(Icons.Rounded.ChevronRight, null) },
                                        onClick = { menuState = MenuState.CONFIGURATION }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Sign Out") },
                                        onClick = {
                                            expanded = false
                                            onSignOut()
                                        }
                                    )
                                }
                                if (showChapters) {
                                    if (showMenu) HorizontalDivider()
                                    Text(
                                        "Chapters",
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    chapters.forEachIndexed { index, chapter ->
                                        DropdownMenuItem(
                                            text = { Text(chapter.title) },
                                            onClick = {
                                                expanded = false
                                                onChapterSelected(index)
                                            }
                                        )
                                    }
                                }
                            }
                            MenuState.VIEW -> {
                                DropdownMenuItem(
                                    text = { Text("Back") },
                                    leadingIcon = { Icon(Icons.Rounded.ArrowBackIosNew, null) },
                                    onClick = { menuState = MenuState.MAIN }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Tiles") },
                                    onClick = {
                                        expanded = false
                                        onViewTiles()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("List") },
                                    onClick = {
                                        expanded = false
                                        onViewList()
                                    }
                                )
                            }
                            MenuState.REFRESH -> {
                                DropdownMenuItem(
                                    text = { Text("Back") },
                                    leadingIcon = { Icon(Icons.Rounded.ArrowBackIosNew, null) },
                                    onClick = { menuState = MenuState.MAIN }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Refresh Library") },
                                    onClick = {
                                        expanded = false
                                        onRefreshLibrary()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Refresh Metadata") },
                                    onClick = {
                                        expanded = false
                                        onRefreshMetadata()
                                    }
                                )
                            }
                            MenuState.CONFIGURATION -> {
                                DropdownMenuItem(
                                    text = { Text("Back") },
                                    leadingIcon = { Icon(Icons.Rounded.ArrowBackIosNew, null) },
                                    onClick = { menuState = MenuState.MAIN }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Change Server") },
                                    onClick = {
                                        expanded = false
                                        onChangeServer()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Change Library") },
                                    onClick = {
                                        expanded = false
                                        onChangeLibrary()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Black,
            titleContentColor = MaterialTheme.colorScheme.primary
        )
    )
}

