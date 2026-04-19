package com.klentahn.plexyaudiobooks.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.klentahn.plexyaudiobooks.ui.components.CommonTopBar

@Composable
fun AuthorsScreen(
    onAuthorClick: (String) -> Unit,
    onNavigateToBooks: () -> Unit,
    onChangeServer: () -> Unit,
    onChangeLibrary: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: AuthorsViewModel = viewModel(factory = AuthorsViewModel.Factory)
) {
    val authors by viewModel.authors.collectAsState()

    Scaffold(
        topBar = {
            CommonTopBar(
                title = "Plexy",
                subtitle = "Authors",
                showMenu = true,
                onNavigateToBooks = onNavigateToBooks,
                onNavigateToAuthors = {}, // Already here
                onChangeServer = onChangeServer,
                onChangeLibrary = onChangeLibrary,
                onSignOut = { viewModel.signOut(onSignOut) }
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (authors.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No authors found.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(authors) { author ->
                        Text(
                            text = author,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAuthorClick(author) }
                                .padding(vertical = 16.dp)
                        )
                        HorizontalDivider(color = Color.DarkGray)
                    }
                }
            }
        }
    }
}
