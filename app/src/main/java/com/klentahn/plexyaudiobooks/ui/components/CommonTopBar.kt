package com.klentahn.plexyaudiobooks.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.klentahn.plexyaudiobooks.ui.screens.player.Chapter

enum class MenuType {
    MAIN, CHANGE_VIEW, CONFIG, REFRESH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonTopBar(
    title: String,
    subtitle: String? = null,
    canNavigateBack: Boolean = false,
    onNavigateBack: (() -> Unit)? = null,
    showChapters: Boolean = false,
    chapters: List<Chapter> = emptyList(),
    onChapterSelected: (Int) -> Unit = {},
    onChangeServer: (() -> Unit)? = null,
    onChangeLibrary: (() -> Unit)? = null,
    onSignOut: (() -> Unit)? = null,
    onNavigateToLibrary: (() -> Unit)? = null,
    onNavigateToAuthors: (() -> Unit)? = null,
    onNavigateToAbout: (() -> Unit)? = null,
    onRefreshLibrary: (() -> Unit)? = null,
    onRefreshMetadata: (() -> Unit)? = null,
    showMenuIcon: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var menuType by remember { mutableStateOf(MenuType.MAIN) }
    var showChaptersMenu by remember { mutableStateOf(false) }

    // Reset menu type when menu is closed
    LaunchedEffect(showMenu) {
        if (!showMenu) {
            menuType = MenuType.MAIN
        }
    }

    TopAppBar(
        title = {
            Column {
                Text(title)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            if (canNavigateBack && onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        modifier = modifier,
        actions = {
            if (showChapters) {
                Box {
                    IconButton(
                        onClick = { showChaptersMenu = true },
                        enabled = chapters.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "Chapters",
                            tint = if (chapters.isNotEmpty()) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
                        )
                    }
                    if (chapters.isNotEmpty()) {
                        DropdownMenu(
                            expanded = showChaptersMenu,
                            onDismissRequest = { showChaptersMenu = false }
                        ) {
                            chapters.forEachIndexed { index, chapter ->
                                DropdownMenuItem(
                                    text = { Text(chapter.title) },
                                    onClick = {
                                        showChaptersMenu = false
                                        onChapterSelected(index)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            val hasViewOptions = onNavigateToLibrary != null || onNavigateToAuthors != null
            val hasConfigOptions = onChangeServer != null || onChangeLibrary != null || onNavigateToAbout != null
            val hasRefreshOptions = onRefreshLibrary != null || onRefreshMetadata != null
            val showMoreOptions = (hasViewOptions || hasConfigOptions || hasRefreshOptions || onSignOut != null) && showMenuIcon

            if (showMoreOptions) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        when (menuType) {
                            MenuType.MAIN -> {
                                if (hasViewOptions) {
                                    DropdownMenuItem(
                                        text = { Text("Change View") },
                                        trailingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                                        onClick = { menuType = MenuType.CHANGE_VIEW }
                                    )
                                }
                                if (hasConfigOptions) {
                                    DropdownMenuItem(
                                        text = { Text("Config") },
                                        trailingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                                        onClick = { menuType = MenuType.CONFIG }
                                    )
                                }
                                if (hasRefreshOptions) {
                                    DropdownMenuItem(
                                        text = { Text("Refresh") },
                                        trailingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                                        onClick = { menuType = MenuType.REFRESH }
                                    )
                                }
                                if (onSignOut != null) {
                                    DropdownMenuItem(
                                        text = { Text("Sign Out") },
                                        onClick = {
                                            showMenu = false
                                            onSignOut()
                                        }
                                    )
                                }
                            }
                            MenuType.CHANGE_VIEW -> {
                                if (onNavigateToLibrary != null) {
                                    DropdownMenuItem(
                                        text = { Text("Library View") },
                                        onClick = {
                                            showMenu = false
                                            onNavigateToLibrary()
                                        }
                                    )
                                }
                                if (onNavigateToAuthors != null) {
                                    DropdownMenuItem(
                                        text = { Text("Author View") },
                                        onClick = {
                                            showMenu = false
                                            onNavigateToAuthors()
                                        }
                                    )
                                }
                            }
                            MenuType.CONFIG -> {
                                if (onChangeServer != null) {
                                    DropdownMenuItem(
                                        text = { Text("Change Server") },
                                        onClick = {
                                            showMenu = false
                                            onChangeServer()
                                        }
                                    )
                                }
                                if (onChangeLibrary != null) {
                                    DropdownMenuItem(
                                        text = { Text("Change Library") },
                                        onClick = {
                                            showMenu = false
                                            onChangeLibrary()
                                        }
                                    )
                                }
                                if (onNavigateToAbout != null) {
                                    DropdownMenuItem(
                                        text = { Text("About") },
                                        onClick = {
                                            showMenu = false
                                            onNavigateToAbout()
                                        }
                                    )
                                }
                            }
                            MenuType.REFRESH -> {
                                if (onRefreshLibrary != null) {
                                    DropdownMenuItem(
                                        text = { Text("Refresh Library") },
                                        onClick = {
                                            showMenu = false
                                            onRefreshLibrary()
                                        }
                                    )
                                }
                                if (onRefreshMetadata != null) {
                                    DropdownMenuItem(
                                        text = { Text("Refresh Metadata") },
                                        onClick = {
                                            showMenu = false
                                            onRefreshMetadata()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
