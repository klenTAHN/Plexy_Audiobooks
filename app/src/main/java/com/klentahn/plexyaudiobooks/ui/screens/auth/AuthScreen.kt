package com.klentahn.plexyaudiobooks.ui.screens.auth

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.klentahn.plexyaudiobooks.ui.components.CommonTopBar

@Composable
fun AuthScreen(
    onSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    val uiState by viewModel.pinState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = { CommonTopBar(title = "Link Plex Account") },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is AuthUiState.Idle -> {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Connect to Plex", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Sign in to access your audiobooks",
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(48.dp))

                        Button(onClick = { viewModel.generatePin() }, modifier = Modifier.fillMaxWidth()) {
                            Text("LOGIN TO PLEX")
                        }
                    }
                }

                is AuthUiState.Loading -> {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Initializing Plex Login...")
                    }
                }

                is AuthUiState.PinGenerated -> {
                    LaunchedEffect(state.authUrl) {
                        Log.d("AuthScreen", "Opening Custom Tab for: ${state.authUrl}")
                        val customTabsIntent = CustomTabsIntent.Builder().build()
                        customTabsIntent.launchUrl(context, Uri.parse(state.authUrl))
                    }

                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Plex Login Opened", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "We've opened the Plex login in a secure browser tab. Please sign in there.",
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(32.dp))
                        CircularProgressIndicator()
                        Spacer(Modifier.height(48.dp))

                        Button(
                            onClick = { viewModel.checkPinStatus() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("I HAVE SIGNED IN")
                        }
                        
                        TextButton(onClick = { viewModel.generatePin() }) {
                            Text("RE-OPEN LOGIN")
                        }
                    }
                }

                is AuthUiState.Success -> LaunchedEffect(Unit) { onSuccess() }

                is AuthUiState.Error -> {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { viewModel.generatePin() }) {
                            Text("TRY AGAIN")
                        }
                    }
                }
            }
        }
    }
}