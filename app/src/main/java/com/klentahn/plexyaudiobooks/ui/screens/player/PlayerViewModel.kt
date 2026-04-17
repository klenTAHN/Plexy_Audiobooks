package com.klentahn.plexyaudiobooks.ui.screens.player

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.core.content.ContextCompat
import com.klentahn.plexyaudiobooks.data.local.SettingsManager
import com.klentahn.plexyaudiobooks.data.repository.PlexRepository
import com.klentahn.plexyaudiobooks.data.MetadataMaster
import com.klentahn.plexyaudiobooks.playback.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val title: String = "",
    val author: String = "",
    val thumbUrl: String? = null,
    val embeddedArt: ByteArray? = null,
    val chapters: List<Chapter> = emptyList(),
    val currentChapterIndex: Int = -1
)

data class Chapter(
    val title: String,
    val startTimeMs: Long,
    val endTimeMs: Long
)

class PlayerViewModel(
    private val plexRepository: PlexRepository,
    private val settingsManager: SettingsManager,
    private val metadataMaster: MetadataMaster
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var mediaController: MediaController? = null
    private val controller: MediaController?
        get() = mediaController

    private var progressJob: Job? = null

    private suspend fun resolveThumbRecursive(
        serverUri: String,
        token: String,
        metadata: com.klentahn.plexyaudiobooks.data.model.PlexMetadata,
        depth: Int = 0
    ): String? {
        if (depth > 3) return null

        val thumb = metadata.thumb.takeIf { !it.isNullOrBlank() }
            ?: metadata.parentThumb.takeIf { !it.isNullOrBlank() }
            ?: metadata.grandparentThumb.takeIf { !it.isNullOrBlank() }
            ?: metadata.art.takeIf { !it.isNullOrBlank() }

        if (thumb != null) return thumb

        if (metadata.parentRatingKey != null) {
            val parentMetadata = plexRepository.getMetadata(serverUri, token, metadata.parentRatingKey)
            if (parentMetadata != null) {
                return resolveThumbRecursive(serverUri, token, parentMetadata, depth + 1)
            }
        }
        return null
    }

    fun initController(context: Context, ratingKey: String) {
        Log.d("PlayerViewModel", "initController called for ratingKey: $ratingKey")
        
        if (mediaController != null) {
            Log.d("PlayerViewModel", "Controller already exists, preparing playback for $ratingKey")
            viewModelScope.launch {
                preparePlayback(ratingKey)
            }
            return
        }

        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        
        future.addListener({
            try {
                val controller = future.get()
                Log.d("PlayerViewModel", "MediaController connected successfully")
                this.mediaController = controller
                
                // Set initial state
                _uiState.value = _uiState.value.copy(isPlaying = controller.isPlaying)
                if (controller.isPlaying) startProgressUpdate()

                controller.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        Log.d("PlayerViewModel", "onIsPlayingChanged: $isPlaying")
                        _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
                        if (isPlaying) startProgressUpdate() else stopProgressUpdate()
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        Log.d("PlayerViewModel", "onMediaItemTransition: ${mediaItem?.mediaId}")
                        updateMetadata(mediaItem)
                        if (mediaItem != null) {
                            viewModelScope.launch {
                                refreshTrackMetadata(mediaItem)
                            }
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        Log.d("PlayerViewModel", "onPlaybackStateChanged: $playbackState")
                        _uiState.value = _uiState.value.copy(
                            isLoading = playbackState == Player.STATE_BUFFERING,
                            duration = if (playbackState == Player.STATE_READY) controller.duration else _uiState.value.duration
                        )
                    }
                    
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        val cause = error.cause
                        Log.e("PlayerViewModel", "Player error: ${error.errorCodeName} (${error.errorCode}): ${error.message}")
                        Log.e("PlayerViewModel", "Cause: ${cause?.javaClass?.simpleName}: ${cause?.message}")
                        if (cause is java.net.ConnectException || cause is java.net.SocketTimeoutException) {
                            Log.e("PlayerViewModel", "Network connectivity issue detected.")
                        }
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                })

                viewModelScope.launch {
                    preparePlayback(ratingKey)
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Failed to connect to MediaController", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private suspend fun preparePlayback(ratingKey: String) {
        val c = controller
        if (c == null) {
            Log.w("PlayerViewModel", "preparePlayback: Controller is null")
            return
        }

        // If already playing THIS item, just update state and return
        if (c.currentMediaItem?.mediaId == ratingKey) {
            Log.d("PlayerViewModel", "Item $ratingKey already loaded")
            updateMetadata(c.currentMediaItem)
            _uiState.value = _uiState.value.copy(
                duration = c.duration,
                currentPosition = c.currentPosition,
                isLoading = false
            )
            return
        }

        Log.d("PlayerViewModel", "Preparing playback for $ratingKey")
        _uiState.value = _uiState.value.copy(isLoading = true)

        val serverUri = settingsManager.serverUri.first()?.removeSuffix("/") ?: return
        val token = settingsManager.authToken.first() ?: return
        
        // Use album ratingKey for children, but we need the specific track metadata for progress/chapters
        val initialMetadata = plexRepository.getMetadata(serverUri, token, ratingKey) ?: return

        Log.d("PlayerViewModel", "Metadata for $ratingKey: type=${initialMetadata.type}, title=${initialMetadata.title}")

        val mediaItems = mutableListOf<MediaItem>()
        var targetTrackRatingKey: String? = null

        if (initialMetadata.type == "album") {
            Log.d("PlayerViewModel", "RatingKey is an album, fetching children...")
            val children = plexRepository.getChildren(serverUri, token, ratingKey)
            
            // Find the last played track or default to first
            val lastPlayedTrack = children?.filter { it.lastViewedAt != null }
                ?.maxByOrNull { it.lastViewedAt!! }
                ?: children?.firstOrNull()
            
            targetTrackRatingKey = lastPlayedTrack?.ratingKey

            children?.forEach { child ->
                val mediaItem = processMetadata(child, serverUri, token, targetTrackRatingKey == child.ratingKey)
                if (mediaItem != null) mediaItems.add(mediaItem)
            }
        } else {
            targetTrackRatingKey = initialMetadata.ratingKey
            processMetadata(initialMetadata, serverUri, token, true)?.let { mediaItems.add(it) }
        }

        if (mediaItems.isEmpty()) {
            Log.w("PlayerViewModel", "No playable media found for $ratingKey")
            _uiState.value = _uiState.value.copy(
                title = initialMetadata.title,
                isLoading = false
            )
            return
        }

        // Find the index of the track we want to start with
        val startIndex = mediaItems.indexOfFirst { it.mediaId == targetTrackRatingKey }.coerceAtLeast(0)
        val targetMediaItem = mediaItems[startIndex]
        val targetMetadata = targetMediaItem.mediaMetadata

        Log.d("PlayerViewModel", "Loaded ${mediaItems.size} media items. Starting playback at index $startIndex.")

        _uiState.value = _uiState.value.copy(
            title = targetMetadata.title?.toString() ?: "",
            author = targetMetadata.artist?.toString() ?: "",
            thumbUrl = targetMetadata.artworkUri?.toString()
        )

        c.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
        c.prepare()

        // Restore playback position if available
        val initialOffset = targetMediaItem.mediaMetadata.extras?.getLong("viewOffset") ?: 0L
        if (initialOffset > 0) {
            Log.d("PlayerViewModel", "Restoring playback position to $initialOffset ms for $targetTrackRatingKey")
            c.seekTo(initialOffset)
        }

        c.play()

        // Fetch embedded art and chapters for the starting item
        refreshTrackMetadata(targetMediaItem)
    }

    private suspend fun refreshTrackMetadata(mediaItem: MediaItem) {
        val serverUri = settingsManager.serverUri.first()?.removeSuffix("/") ?: return
        val token = settingsManager.authToken.first() ?: return
        val ratingKey = mediaItem.mediaId

        viewModelScope.launch(Dispatchers.IO) {
            // Check for embedded art if thumb is missing
            if (mediaItem.mediaMetadata.artworkUri == null) {
                val streamUrl = mediaItem.localConfiguration?.uri.toString()
                val art = metadataMaster.getEmbeddedArt(android.net.Uri.parse(streamUrl))
                _uiState.value = _uiState.value.copy(embeddedArt = art)
            } else {
                _uiState.value = _uiState.value.copy(embeddedArt = null)
            }
            
            // Fetch full metadata for chapters
            val fullMetadata = plexRepository.getMetadata(serverUri, token, ratingKey)
            val chapters = if (fullMetadata?.chapters != null) {
                fullMetadata.chapters.mapIndexed { index, plexChapter ->
                    val startTime = plexChapter.startTimeOffset ?: 0L
                    val endTime = plexChapter.endTimeOffset 
                        ?: fullMetadata.chapters.getOrNull(index + 1)?.startTimeOffset 
                        ?: (mediaItem.mediaMetadata.extras?.getLong("duration") ?: 0L)
                    
                    Chapter(
                        title = plexChapter.tag ?: "Chapter ${plexChapter.index ?: (index + 1)}",
                        startTimeMs = startTime,
                        endTimeMs = endTime
                    )
                }
            } else {
                val streamUrl = mediaItem.localConfiguration?.uri
                if (streamUrl != null) metadataMaster.getChapters(streamUrl) else emptyList()
            }
            
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(chapters = chapters)
                Log.d("PlayerViewModel", "Updated chapters for $ratingKey: ${chapters.size} found")
            }
        }
    }

    private suspend fun processMetadata(
        item: com.klentahn.plexyaudiobooks.data.model.PlexMetadata,
        serverUri: String,
        token: String,
        isTarget: Boolean
    ): MediaItem? {
        // Fetch full metadata for the target item to get viewOffset and chapters
        val fullItem = if (isTarget && (item.viewOffset == null && item.chapters == null)) {
            plexRepository.getMetadata(serverUri, token, item.ratingKey) ?: item
        } else item

        val mediaPart = fullItem.media?.firstOrNull()?.parts?.firstOrNull()
        if (mediaPart != null) {
            val streamUrl = "$serverUri${mediaPart.key}${if (mediaPart.key.contains("?")) "&" else "?"}X-Plex-Token=$token"
            
            val thumbPath = resolveThumbRecursive(serverUri, token, fullItem)
            val itemThumbUrl = if (thumbPath != null) {
                val encodedThumb = java.net.URLEncoder.encode(thumbPath, "UTF-8")
                "$serverUri/photo/:/transcode?url=$encodedThumb&width=600&height=600&X-Plex-Token=$token"
            } else null

            val author = fullItem.grandparentTitle.takeIf { !it.isNullOrBlank() && it != "Various Artists" && it != "Unknown Artist" } 
                ?: fullItem.parentTitle.takeIf { !it.isNullOrBlank() && it != "Various Artists" && it != "Unknown Artist" } 
                ?: fullItem.grandparentTitle.takeIf { !it.isNullOrBlank() }
                ?: fullItem.parentTitle.takeIf { !it.isNullOrBlank() }
                ?: ""

            return MediaItem.Builder()
                .setMediaId(fullItem.ratingKey)
                .setUri(streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(fullItem.title)
                        .setArtist(author)
                        .setArtworkUri(itemThumbUrl?.let { android.net.Uri.parse(it) })
                        .setExtras(Bundle().apply { 
                            putString("key", fullItem.key) 
                            putLong("viewOffset", fullItem.viewOffset ?: 0L)
                        })
                        .build()
                )
                .build()
        }
        return null
    }

    private fun updateMetadata(mediaItem: MediaItem?) {
        mediaItem?.let {
            _uiState.value = _uiState.value.copy(
                title = it.mediaMetadata.title?.toString() ?: _uiState.value.title,
                author = it.mediaMetadata.artist?.toString() ?: _uiState.value.author,
                thumbUrl = it.mediaMetadata.artworkUri?.toString() ?: _uiState.value.thumbUrl
            )
        }
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                controller?.let {
                    _uiState.value = _uiState.value.copy(
                        currentPosition = it.currentPosition,
                        duration = it.duration
                    )
                }
                delay(1000)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
    }

    fun playPause() {
        val c = controller
        if (c != null) {
            Log.d("PlayerViewModel", "playPause clicked. Current isPlaying: ${c.isPlaying}, state: ${c.playbackState}")
            if (c.isPlaying) {
                c.pause()
            } else {
                if (c.playbackState == Player.STATE_ENDED) {
                    c.seekTo(0)
                }
                c.play()
            }
        } else {
            Log.w("PlayerViewModel", "playPause clicked but controller is null")
        }
    }

    fun seekTo(position: Long) {
        controller?.seekTo(position)
        _uiState.value = _uiState.value.copy(currentPosition = position)
    }

    fun skipForward() {
        val c = controller
        if (c != null) {
            c.seekTo(c.currentPosition + 30000)
        }
    }

    fun skipBackward() {
        val c = controller
        if (c != null) {
            c.seekTo(c.currentPosition - 15000)
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaController?.release()
    }
}
