package com.klentahn.plexyaudiobooks.ui.screens.library

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.klentahn.plexyaudiobooks.PlexyAudiobooksApplication
import com.klentahn.plexyaudiobooks.data.repository.PlexRepository
import com.klentahn.plexyaudiobooks.ui.components.CommonTopBar
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    onBookClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    onNavigateToAuthors: () -> Unit,
    onChangeServer: () -> Unit,
    onChangeLibrary: () -> Unit,
    onSignOut: () -> Unit,           // Keep for direct navigation if needed
    serverUri: String?,
    token: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appContainer = (context.applicationContext as PlexyAudiobooksApplication).container
    val repository: PlexRepository = appContainer.plexRepository

    // Main content (your existing UI)
    Scaffold(
        topBar = {
            CommonTopBar(
                title = "My Audiobooks",
                onChangeServer = onChangeServer,
                onChangeLibrary = onChangeLibrary,
                onSignOut = {
                    scope.launch {
                        repository.signOut()
                        onSignOut()           // This triggers navigation back to Auth
                    }
                }
            )
        }
    ) { padding ->
        // Your existing Library content here...
        // Example:
        // BookList(
        //     serverUri = serverUri,
        //     token = token,
        //     onBookClick = onBookClick,
        //     onAuthorClick = onAuthorClick,
        //     padding = padding
        // )
    }
}