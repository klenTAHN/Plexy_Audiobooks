package com.klentahn.plexyaudiobooks.ui.screens.auth

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
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
                    LaunchedEffect(state.pin.code) {
                        // Copy to clipboard
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Plex PIN", state.pin.code)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Code ${state.pin.code} copied to clipboard", Toast.LENGTH_SHORT).show()
                    }

                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Link Your Account", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(24.dp))
                        
                        Text("Visit plex.tv/link and enter this code:", textAlign = TextAlign.Center)
                        Text(
                            "(Code copied to clipboard)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(16.dp))
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = state.pin.code,
                                style = MaterialTheme.typography.displayMedium,
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://plex.tv/link"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("OPEN PLEX.TV/LINK")
                        }
                        
                        Spacer(Modifier.height(32.dp))
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Waiting for you to link the app...",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(48.dp))

                        Button(
                            onClick = { viewModel.checkPinStatus(isManual = true) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("I HAVE ENTERED THE CODE")
                        }
                        
                        TextButton(onClick = { viewModel.generatePin() }) {
                            Text("GENERATE NEW CODE")
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