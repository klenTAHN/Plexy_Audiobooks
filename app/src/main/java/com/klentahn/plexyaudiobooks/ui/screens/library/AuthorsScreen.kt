package com.klentahn.plexyaudiobooks.ui.screens.library

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.klentahn.plexyaudiobooks.PlexyAudiobooksApplication
import com.klentahn.plexyaudiobooks.data.repository.PlexRepository
import com.klentahn.plexyaudiobooks.ui.components.CommonTopBar
import kotlinx.coroutines.launch

@Composable
fun AuthorsScreen(
    onAuthorClick: (String) -> Unit,
    onNavigateToBooks: () -> Unit,
    onChangeServer: () -> Unit,
    onChangeLibrary: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = (context.applicationContext as PlexyAudiobooksApplication)
        .container.plexRepository

    Scaffold(
        topBar = {
            CommonTopBar(
                title = "Authors",
                onChangeServer = onChangeServer,
                onChangeLibrary = onChangeLibrary,
                onSignOut = {
                    scope.launch {
                        repository.signOut()
                        onSignOut()
                    }
                }
            )
        }
    ) { padding ->
        // Your existing Authors list content here...
    }
}