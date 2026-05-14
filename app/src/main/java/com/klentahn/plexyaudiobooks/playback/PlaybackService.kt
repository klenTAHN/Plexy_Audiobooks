package com.klentahn.plexyaudiobooks.playback

import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.klentahn.plexyaudiobooks.PlexyAudiobooksApplication
import androidx.core.net.toUri
import com.klentahn.plexyaudiobooks.data.repository.PlexRepository
import com.klentahn.plexyaudiobooks.data.repository.LibraryRepository
import com.klentahn.plexyaudiobooks.data.local.SettingsManager
import com.klentahn.plexyaudiobooks.data.local.db.BookEntity
import com.klentahn.plexyaudiobooks.data.model.PlexMetadata
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService.LibraryParams
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlaybackService : MediaLibraryService() {

    private lateinit var exoPlayer: ExoPlayer
    private var mediaSession: MediaLibrarySession? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var progressSyncJob: Job? = null

    private lateinit var plexRepository: PlexRepository
    private lateinit var libraryRepository: LibraryRepository
    private lateinit var settingsManager: SettingsManager

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val appContainer = (application as PlexyAudiobooksApplication).container
        plexRepository = appContainer.plexRepository
        libraryRepository = appContainer.libraryRepository
        settingsManager = appContainer.settingsManager

        val attributionContext = createAttributionContext("media_playback")

        exoPlayer = ExoPlayer.Builder(attributionContext)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        mediaSession = MediaLibrarySession.Builder(attributionContext, exoPlayer, LibrarySessionCallback())
            .setSessionActivity(
                android.app.PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, com.klentahn.plexyaudiobooks.MainActivity::class.java),
                    android.app.PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    startProgressSync()
                } else {
                    stopProgressSync()
                    syncProgressToPlex()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    scrobbleToPlex()
                }
            }
        })
    }

    private fun startProgressSync() {
        progressSyncJob?.cancel()
        progressSyncJob = serviceScope.launch {
            while (true) {
                delay(10000) // Sync every 10 seconds
                syncProgressToPlex()
            }
        }
    }

    private fun stopProgressSync() {
        progressSyncJob?.cancel()
        progressSyncJob = null
    }

    private fun syncProgressToPlex() {
        // MUST capture these on the main thread
        val currentMediaItem = exoPlayer.currentMediaItem ?: return
        val ratingKey = currentMediaItem.mediaId
        val key = currentMediaItem.mediaMetadata.extras?.getString("key") ?: return
        val position = exoPlayer.currentPosition
        val duration = exoPlayer.duration
        val isPlaying = exoPlayer.isPlaying

        serviceScope.launch(Dispatchers.IO) {
            val serverUri = settingsManager.serverUri.first() ?: return@launch
            val token = settingsManager.authToken.first() ?: return@launch

            plexRepository.updateTimeline(
                serverUri = serverUri,
                token = token,
                ratingKey = ratingKey,
                key = key,
                state = if (isPlaying) "playing" else "paused",
                time = position,
                duration = duration
            )
        }
    }

    private fun scrobbleToPlex() {
        val currentMediaItem = exoPlayer.currentMediaItem ?: return
        val key = currentMediaItem.mediaMetadata.extras?.getString("key") ?: return

        serviceScope.launch(Dispatchers.IO) {
            val serverUri = settingsManager.serverUri.first() ?: return@launch
            val token = settingsManager.authToken.first() ?: return@launch
            plexRepository.scrobble(serverUri, token, key)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

    private suspend fun resolveMediaItem(ratingKey: String): List<MediaItem> {
        val serverUri = settingsManager.serverUri.first()?.removeSuffix("/") ?: return emptyList()
        val token = settingsManager.authToken.first() ?: return emptyList()

        val initialMetadata = plexRepository.getMetadata(serverUri, token, ratingKey) ?: return emptyList()

        val mediaItems = mutableListOf<MediaItem>()
        if (initialMetadata.type == "album") {
            val children = plexRepository.getChildren(serverUri, token, ratingKey)
            children?.forEach { child ->
                val mediaItem = processMetadata(child, serverUri, token, initialMetadata)
                if (mediaItem != null) mediaItems.add(mediaItem)
            }
        } else {
            val parentMetadata = if (!initialMetadata.parentRatingKey.isNullOrBlank()) {
                plexRepository.getMetadata(serverUri, token, initialMetadata.parentRatingKey)
            } else null
            processMetadata(initialMetadata, serverUri, token, parentMetadata)?.let { mediaItems.add(it) }
        }
        return mediaItems
    }

    private fun processMetadata(
        item: PlexMetadata,
        serverUri: String,
        token: String,
        parentMetadata: PlexMetadata? = null
    ): MediaItem? {
        val mediaPart = item.media?.firstOrNull()?.parts?.firstOrNull()
        if (mediaPart != null) {
            val streamUrl = "$serverUri${mediaPart.key}${if (mediaPart.key.contains("?")) "&" else "?"}X-Plex-Token=$token"
            
            val thumbPath = item.thumb ?: parentMetadata?.thumb
            val itemThumbUrl = if (thumbPath != null) {
                val encodedThumb = java.net.URLEncoder.encode(thumbPath, "UTF-8")
                "$serverUri/photo/:/transcode?url=$encodedThumb&width=600&height=600&X-Plex-Token=$token"
            } else null

            val bookTitle = parentMetadata?.title ?: item.parentTitle ?: item.title
            val author = item.grandparentTitle.takeIf { !it.isNullOrBlank() && it != "Various Artists" && it != "Unknown Artist" }
                ?: item.parentTitle.takeIf { !it.isNullOrBlank() && it != "Various Artists" && it != "Unknown Artist" }
                ?: item.grandparentTitle.takeIf { !it.isNullOrBlank() }
                ?: item.parentTitle.takeIf { !it.isNullOrBlank() }
                ?: parentMetadata?.parentTitle ?: parentMetadata?.grandparentTitle
                ?: ""

            return MediaItem.Builder()
                .setMediaId(item.ratingKey)
                .setUri(streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(item.title)
                        .setAlbumTitle(bookTitle)
                        .setArtist(author)
                        .setArtworkUri(itemThumbUrl?.toUri())
                        .setExtras(Bundle().apply { 
                            putString("key", item.key) 
                            putLong("viewOffset", item.viewOffset ?: 0L)
                        })
                        .build()
                )
                .build()
        }
        return null
    }

    override fun onDestroy() {
        stopProgressSync()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId("ROOT")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setTitle("Plexy Audiobooks")
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            serviceScope.launch {
                val items = mutableListOf<MediaItem>()
                when (parentId) {
                    "ROOT" -> {
                        items.add(
                            MediaItem.Builder()
                                .setMediaId("ALL_BOOKS")
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle("All Books")
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .build()
                                )
                                .build()
                        )
                        items.add(
                            MediaItem.Builder()
                                .setMediaId("AUTHORS")
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle("Authors")
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .build()
                                )
                                .build()
                        )
                    }
                    "ALL_BOOKS" -> {
                        val books = libraryRepository.getBooksByTitle("").first()
                        books.forEach { book ->
                            items.add(
                                MediaItem.Builder()
                                    .setMediaId(book.ratingKey)
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle(book.title)
                                            .setArtist(book.author)
                                            .setIsBrowsable(false)
                                            .setIsPlayable(true)
                                            .setArtworkUri(book.thumb?.toUri())
                                            .build()
                                    )
                                    .build()
                            )
                        }
                    }
                    "AUTHORS" -> {
                        val authors = libraryRepository.getAuthors("").first()
                        authors.forEach { author ->
                            items.add(
                                MediaItem.Builder()
                                    .setMediaId("AUTHOR|$author")
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setTitle(author)
                                            .setIsBrowsable(true)
                                            .setIsPlayable(false)
                                            .build()
                                    )
                                    .build()
                            )
                        }
                    }
                    else -> {
                        if (parentId.startsWith("AUTHOR|")) {
                            val author = parentId.substringAfter("AUTHOR|")
                            val books = libraryRepository.getBooksByAuthor(author).first()
                            books.forEach { book ->
                                items.add(
                                    MediaItem.Builder()
                                        .setMediaId(book.ratingKey)
                                        .setMediaMetadata(
                                            MediaMetadata.Builder()
                                                .setTitle(book.title)
                                                .setArtist(book.author)
                                                .setIsBrowsable(false)
                                                .setIsPlayable(true)
                                                .setArtworkUri(book.thumb?.toUri())
                                                .build()
                                        )
                                        .build()
                                )
                            }
                        }
                    }
                }
                future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
            }
            return future
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val future = SettableFuture.create<MutableList<MediaItem>>()
            serviceScope.launch {
                val resolvedItems = mutableListOf<MediaItem>()
                for (item in mediaItems) {
                    if (item.localConfiguration?.uri != null) {
                        resolvedItems.add(item)
                    } else {
                        resolvedItems.addAll(resolveMediaItem(item.mediaId))
                    }
                }
                future.set(resolvedItems)
            }
            return future
        }

        @OptIn(UnstableApi::class)
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val availablePlayerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_BACK)
                .add(Player.COMMAND_SEEK_FORWARD)
                .build()
            return MediaSession.ConnectionResult.accept(
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS,
                availablePlayerCommands
            )
        }

        @OptIn(UnstableApi::class)
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val currentMediaItem = exoPlayer.currentMediaItem
            if (currentMediaItem != null) {
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(
                        listOf(currentMediaItem),
                        0,
                        exoPlayer.currentPosition
                    )
                )
            }
            // If no current item, try to get the last one played or just return an empty future
            // For now, to avoid the crash, we must NOT call super.onPlaybackResumption if it's not implemented
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(
                    emptyList(),
                    0,
                    0
                )
            )
        }
    }
}
