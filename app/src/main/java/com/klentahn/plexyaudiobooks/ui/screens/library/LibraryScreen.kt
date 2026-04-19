package com.klentahn.plexyaudiobooks.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.klentahn.plexyaudiobooks.data.MetadataMaster
import com.klentahn.plexyaudiobooks.data.local.db.BookEntity
import com.klentahn.plexyaudiobooks.ui.components.CommonTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBookClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    onNavigateToAuthors: () -> Unit,
    onChangeServer: () -> Unit,
    onChangeLibrary: () -> Unit,
    onSignOut: () -> Unit,
    serverUri: String?,
    token: String?,
    viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory)
) {
    val books by viewModel.books.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val context = LocalContext.current
    val metadataMaster = remember { MetadataMaster(context) }

    Scaffold(
        topBar = {
            CommonTopBar(
                title = "Plexy",
                subtitle = "Library",
                showMenu = true,
                showSearch = true,
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onSortByAuthor = { viewModel.setSortOrder(SortOrder.AUTHOR) },
                onSortByBook = { viewModel.setSortOrder(SortOrder.TITLE) },
                onRefreshLibrary = { viewModel.refreshLibrary() },
                onRefreshMetadata = { viewModel.refreshMetadata() },
                onViewTiles = { viewModel.setViewMode(LibraryViewMode.TILES) },
                onViewList = { viewModel.setViewMode(LibraryViewMode.LIST) },
                onNavigateToBooks = {}, // Already on books
                onNavigateToAuthors = onNavigateToAuthors,
                onChangeServer = onChangeServer,
                onChangeLibrary = onChangeLibrary,
                onSignOut = { viewModel.signOut(onSignOut) }
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshLibrary() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (books.isEmpty() && !isRefreshing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No books found. Try refreshing.", color = Color.Gray)
                }
            } else {
                if (viewMode == LibraryViewMode.LIST) {
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
                                onAuthorClick = { onAuthorClick(book.author) }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(books, key = { it.ratingKey }) { book ->
                            BookTile(
                                book = book,
                                serverUri = serverUri,
                                token = token,
                                metadataMaster = metadataMaster,
                                onClick = { onBookClick(book.ratingKey) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookTile(
    book: BookEntity,
    serverUri: String?,
    token: String?,
    metadataMaster: MetadataMaster,
    onClick: () -> Unit
) {
    var imageModel by remember(book.ratingKey, book.thumb) {
        val url = if (serverUri != null && token != null && book.thumb != null) {
            val encodedThumb = URLEncoder.encode(book.thumb, "UTF-8")
            "$serverUri/photo/:/transcode?url=$encodedThumb&width=400&height=400&X-Plex-Token=$token"
        } else null
        mutableStateOf<Any?>(url)
    }

    if (imageModel == null && book.streamUrl != null) {
        LaunchedEffect(book.streamUrl) {
            val embedded = withContext(Dispatchers.IO) {
                metadataMaster.getEmbeddedArt(android.net.Uri.parse(book.streamUrl))
            }
            if (embedded != null) {
                imageModel = embedded
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.aspectRatio(1f),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            AsyncImage(
                model = imageModel,
                contentDescription = "Cover for ${book.title}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = book.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            text = book.author,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BookItem(
    book: BookEntity,
    serverUri: String?,
    token: String?,
    metadataMaster: MetadataMaster,
    onClick: () -> Unit,
    onAuthorClick: () -> Unit
) {
    var imageModel by remember(book.ratingKey, book.thumb) {
        val url = if (serverUri != null && token != null && book.thumb != null) {
            val encodedThumb = URLEncoder.encode(book.thumb, "UTF-8")
            "$serverUri/photo/:/transcode?url=$encodedThumb&width=300&height=300&X-Plex-Token=$token"
        } else null
        mutableStateOf<Any?>(url)
    }

    if (imageModel == null && book.streamUrl != null) {
        LaunchedEffect(book.streamUrl) {
            val embedded = withContext(Dispatchers.IO) {
                metadataMaster.getEmbeddedArt(android.net.Uri.parse(book.streamUrl))
            }
            if (embedded != null) {
                imageModel = embedded
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = imageModel,
                contentDescription = "Cover for ${book.title}",
                modifier = Modifier
                    .size(80.dp)
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onAuthorClick() },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (book.year != null) {
                    Text(
                        text = book.year.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
