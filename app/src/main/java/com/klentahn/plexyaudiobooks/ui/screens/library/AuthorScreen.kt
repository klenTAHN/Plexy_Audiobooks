package com.klentahn.plexyaudiobooks.ui.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.klentahn.plexyaudiobooks.data.MetadataMaster
import com.klentahn.plexyaudiobooks.ui.components.CommonTopBar

@Composable
fun AuthorScreen(
    author: String,
    onBookClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    serverUri: String?,
    token: String?,
    viewModel: AuthorViewModel = viewModel(factory = AuthorViewModel.provideFactory(author))
) {
    val books by viewModel.books.collectAsState()
    val context = LocalContext.current
    val metadataMaster = remember { MetadataMaster(context) }

    Scaffold(
        topBar = {
            CommonTopBar(
                title = "Plexy",
                subtitle = author,
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (books.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No books found for $author", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(books, key = { it.ratingKey }) { book ->
                        BookItem(
                            book = book,
                            serverUri = serverUri,
                            token = token,
                            metadataMaster = metadataMaster,
                            onClick = { onBookClick(book.ratingKey) },
                            onAuthorClick = {} // Already in author screen
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
