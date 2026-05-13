package com.klentahn.plexyaudiobooks.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.klentahn.plexyaudiobooks.PlexyAudiobooksApplication
import com.klentahn.plexyaudiobooks.ui.components.CommonTopBar
import kotlinx.coroutines.launch

@Composable
fun AuthorsScreen(
    onAuthorClick: (String) -> Unit,
    onNavigateToBooks: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onChangeServer: () -> Unit,
    onChangeLibrary: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: AuthorsViewModel = viewModel(factory = AuthorsViewModel.Factory)
) {
    val authors by viewModel.authors.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = (context.applicationContext as PlexyAudiobooksApplication)
        .container.plexRepository

    Scaffold(
        topBar = {
            CommonTopBar(
                title = "Plexy Audiobooks",
                subtitle = "Authors",
                onChangeServer = onChangeServer,
                onChangeLibrary = onChangeLibrary,
                onSignOut = {
                    scope.launch {
                        repository.signOut()
                        onSignOut()
                    }
                },
                onNavigateToLibrary = onNavigateToBooks,
                onNavigateToAuthors = null,
                onNavigateToAbout = onNavigateToAbout
            )
        },
        containerColor = Color.Black
    ) { padding ->
        if (authors.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No authors found.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(authors) { author ->
                    AuthorListItem(
                        author = author,
                        onClick = { onAuthorClick(author) }
                    )
                }
            }
        }
    }
}

@Composable
fun AuthorListItem(
    author: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = author,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
