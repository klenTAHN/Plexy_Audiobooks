package com.klentahn.plexyaudiobooks.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.klentahn.plexyaudiobooks.ui.screens.player.Chapter

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
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showChaptersMenu by remember { mutableStateOf(false) }

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
            if (showChapters && chapters.isNotEmpty()) {
                Box {
                    IconButton(onClick = { showChaptersMenu = true }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Chapters")
                    }
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

            if (onChangeServer != null || onChangeLibrary != null || onSignOut != null) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
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
                }
            }
        }
    )
}
