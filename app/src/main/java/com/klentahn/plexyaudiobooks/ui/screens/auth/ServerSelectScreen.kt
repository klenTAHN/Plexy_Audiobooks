package com.klentahn.plexyaudiobooks.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.klentahn.plexyaudiobooks.data.model.PlexDevice
import com.klentahn.plexyaudiobooks.ui.components.CommonTopBar

@Composable
fun ServerSelectScreen(
    onServerSelected: () -> Unit,
    viewModel: ServerSelectViewModel = viewModel(factory = ServerSelectViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { CommonTopBar(title = "Select Server") },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is ServerSelectUiState.Loading -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                is ServerSelectUiState.Success -> {
                    if (state.servers.isEmpty()) {
                        Text(text = "No Plex servers found.", color = Color.Gray)
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.servers) { server ->
                                ServerItem(
                                    server = server,
                                    onClick = {
                                        viewModel.selectServer(server, onServerSelected)
                                    }
                                )
                                HorizontalDivider(color = Color.DarkGray)
                            }
                        }
                    }
                }

                is ServerSelectUiState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        androidx.compose.material3.Button(
                            onClick = { viewModel.loadServers() },
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
fun ServerItem(server: PlexDevice, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = server.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${server.product} v${server.productVersion}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
