package com.klentahn.plexyaudiobooks.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.klentahn.plexyaudiobooks.data.model.PlexLibrary
import com.klentahn.plexyaudiobooks.ui.components.CommonTopBar

@Composable
fun LibrarySelectScreen(
    onLibrarySelected: () -> Unit,
    viewModel: LibrarySelectViewModel = viewModel(factory = LibrarySelectViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var showWarning by remember { mutableStateOf(false) }

    if (showWarning) {
        AlertDialog(
            onDismissRequest = { 
                showWarning = false
                onLibrarySelected() 
            },
            title = { Text("Warning") },
            text = { Text("If the 'Store track progress' setting is not enabled for this library, audiobook progress will not be saved correctly.") },
            confirmButton = {
                TextButton(onClick = { 
                    onLibrarySelected()
                }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = { CommonTopBar(title = "Select Library") },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is LibrarySelectUiState.Loading -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                is LibrarySelectUiState.Success -> {
                    if (state.libraries.isEmpty()) {
                        Text(text = "No music libraries found on this server.", color = Color.Gray)
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.libraries) { library ->
                                LibraryItem(
                                    library = library,
                                    onClick = {
                                        viewModel.selectLibrary(library) { isTrackProgressEnabled ->
                                            if (isTrackProgressEnabled) {
                                                onLibrarySelected()
                                            } else {
                                                android.util.Log.w("LibrarySelect", "Warning: 'Store track progress' is NOT enabled for library '${library.title}'")
                                                showWarning = true
                                            }
                                        }
                                    }
                                )
                                HorizontalDivider(color = Color.DarkGray)
                            }
                        }
                    }
                }

                is LibrarySelectUiState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        androidx.compose.material3.Button(
                            onClick = { viewModel.loadLibraries() },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("RETRY")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryItem(library: PlexLibrary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = library.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Type: ${library.type}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
