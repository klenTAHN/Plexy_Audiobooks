package com.klentahn.plexyaudiobooks.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.klentahn.plexyaudiobooks.ui.components.CommonTopBar
import java.util.Locale

@Composable
fun PlayerScreen(
    ratingKey: String,
    viewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(ratingKey) {
        viewModel.initController(context, ratingKey)
    }

    Scaffold(
        topBar = {
            CommonTopBar(
                title = "Now Playing",
                subtitle = null,
                canNavigateBack = true,
                onNavigateBack = onBack,
                showChapters = uiState.chapters.isNotEmpty(),
                chapters = uiState.chapters,
                onChapterSelected = { viewModel.seekTo(uiState.chapters[it].startTimeMs) }
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val screenHeight = maxHeight
            val isShortScreen = screenHeight < 600.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = if (isShortScreen) 12.dp else 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (isShortScreen) Arrangement.Top else Arrangement.SpaceBetween
            ) {
                // Main Content (Art, Title, Author)
                Column(
                    modifier = if (isShortScreen) Modifier.wrapContentHeight() else Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val imageModel = uiState.thumbUrl ?: uiState.embeddedArt

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (isShortScreen) 0.5f else 1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageModel != null) {
                            AsyncImage(
                                model = imageModel,
                                contentDescription = "Album Art",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "No Art",
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(if (isShortScreen) 60.dp else 100.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isShortScreen) 12.dp else 32.dp))

                    Text(
                        text = uiState.title,
                        style = if (isShortScreen) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = uiState.author,
                        style = if (isShortScreen) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }

                if (isShortScreen) Spacer(modifier = Modifier.height(24.dp))

                // Controls (Slider and Buttons)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (isShortScreen) 16.dp else 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Progress Slider
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = uiState.currentPosition.toFloat(),
                            onValueChange = { viewModel.seekTo(it.toLong()) },
                            valueRange = 0f..uiState.duration.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.Gray
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(uiState.currentPosition),
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = formatTime(uiState.duration),
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isShortScreen) 16.dp else 32.dp))

                    // Playback Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rewind
                        IconButton(
                            onClick = { viewModel.skipBackward() },
                            modifier = Modifier
                                .size(if (isShortScreen) 48.dp else 56.dp)
                                .background(Color.White.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = "Rewind",
                                tint = Color.White,
                                modifier = Modifier.size(if (isShortScreen) 28.dp else 34.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(if (isShortScreen) 32.dp else 48.dp))

                        // Play/Pause
                        Surface(
                            onClick = { viewModel.playPause() },
                            modifier = Modifier.size(if (isShortScreen) 64.dp else 80.dp),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(if (isShortScreen) 36.dp else 48.dp),
                                        color = Color.Black,
                                        strokeWidth = 3.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = Color.Black,
                                        modifier = Modifier.size(if (isShortScreen) 36.dp else 48.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(if (isShortScreen) 32.dp else 48.dp))

                        // Forward
                        IconButton(
                            onClick = { viewModel.skipForward() },
                            modifier = Modifier
                                .size(if (isShortScreen) 48.dp else 56.dp)
                                .background(Color.White.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "Forward",
                                tint = Color.White,
                                modifier = Modifier.size(if (isShortScreen) 28.dp else 34.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    val min = minutes % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, min, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", min, seconds)
    }
}
