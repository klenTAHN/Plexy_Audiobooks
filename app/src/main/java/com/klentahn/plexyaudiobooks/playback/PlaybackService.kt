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
import com.klentahn.plexyaudiobooks.data.repository.PlexRepository
import com.klentahn.plexyaudiobooks.data.local.SettingsManager
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService.LibraryParams
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
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
    private lateinit var settingsManager: SettingsManager

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val appContainer = (application as PlexyAudiobooksApplication).container
        plexRepository = appContainer.plexRepository
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
                    android.content.Intent(this, com.klentahn.plexyaudiobooks.MainActivity::class.java),
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
                        .setTitle("Plexy Root")
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
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.of(), params))
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
        ): com.google.common.util.concurrent.ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val currentMediaItem = exoPlayer.currentMediaItem
            if (currentMediaItem != null) {
                return com.google.common.util.concurrent.Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(
                        listOf(currentMediaItem),
                        0,
                        exoPlayer.currentPosition
                    )
                )
            }
            // If no current item, try to get the last one played or just return an empty future
            // For now, to avoid the crash, we must NOT call super.onPlaybackResumption if it's not implemented
            return com.google.common.util.concurrent.Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(
                    emptyList(),
                    0,
                    0
                )
            )
        }
    }
}
