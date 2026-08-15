package com.blackwalkmusic

import android.content.ComponentName
import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.delay
import androidx.compose.runtime.*

class MainActivity : ComponentActivity() {

    private lateinit var controllerFuture:
        ListenableFuture<MediaController>

    private var controller: MediaController? = null

    private var songs by mutableStateOf<List<Song>>(
        emptyList()
    )

    private var currentSong by mutableStateOf<Song?>(
        null
    )

    private var currentQueue by mutableStateOf<List<Song>>(
        emptyList()
    )

    private var isPlaying by mutableStateOf(false)

    private var currentPosition by mutableLongStateOf(0L)

    private var duration by mutableLongStateOf(0L)

    private var repeatMode by mutableIntStateOf(
        Player.REPEAT_MODE_OFF
    )

    private var shuffleEnabled by mutableStateOf(false)

    private var favoriteIds by mutableStateOf<Set<Long>>(
        emptySet()
    )

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            loadMusic()
        }

    private val playerListener =
        object : Player.Listener {

            override fun onIsPlayingChanged(
                isPlayingNow: Boolean
            ) {

                isPlaying =
                    isPlayingNow
            }

            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int
            ) {

                syncPlayerState()
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {

                syncPlayerState()
            }

            override fun onTimelineChanged(
                timeline:
                    androidx.media3.common.Timeline,
                reason: Int
            ) {

                syncPlayerState()
            }

            override fun onPlaybackStateChanged(
                playbackState: Int
            ) {

                updateProgress()
            }

            override fun onRepeatModeChanged(
                mode: Int
            ) {

                repeatMode =
                    mode
            }

            override fun onShuffleModeEnabledChanged(
                shuffleModeEnabledNow: Boolean
            ) {

                shuffleEnabled =
                    shuffleModeEnabledNow

                updateCurrentQueue()
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        favoriteIds =
            FavoritesManager.getFavorites(
                this
            )

        setContent {

            var showFullPlayer by remember {
                mutableStateOf(false)
            }

            LaunchedEffect(Unit) {

                while (true) {

                    updateProgress()

                    delay(500)
                }
            }

            if (
                showFullPlayer &&
                currentSong != null
            ) {

                FullPlayerScreen(

                    song =
                        currentSong!!,

                    isPlaying =
                        isPlaying,

                    currentPosition =
                        currentPosition,

                    duration =
                        duration,

                    repeatMode =
                        repeatMode,

                    shuffleEnabled =
                        shuffleEnabled,

                    isFavorite =
                        favoriteIds.contains(
                            currentSong!!.id
                        ),

                    queue =
                        currentQueue,

                    onBack = {

                        showFullPlayer =
                            false
                    },

                    onPlayPause = {

                        playPause()
                    },

                    onNext = {

                        nextSong()
                    },

                    onPrevious = {

                        previousSong()
                    },

                    onSeek = { position ->

                        controller?.seekTo(
                            position
                        )

                        updateProgress()
                    },

                    onShuffle = {

                        toggleShuffle()
                    },

                    onRepeat = {

                        toggleRepeat()
                    },

                    onFavorite = {

                        currentSong?.let {
                            toggleFavorite(it)
                        }
                    },

                    onQueueSongClick = {
                        queueSong ->

                        playSongFromCurrentQueue(
                            queueSong
                        )
                    }
                )

            } else {

                BlackWalkMusicScreen(

                    songs =
                        songs,

                    currentSong =
                        currentSong,

                    isPlaying =
                        isPlaying,

                    favoriteIds =
                        favoriteIds,

                    currentPosition =
                        currentPosition,

                    duration =
                        duration,

                    onSongClick = {

                        playSong(it)
                    },

                    onPlayPause = {

                        playPause()
                    },

                    onNext = {

                        nextSong()
                    },

                    onPrevious = {

                        previousSong()
                    },

                    onShuffle = {

                        shuffleSongs()
                    },

                    onOpenPlayer = {

                        if (
                            currentSong != null
                        ) {

                            showFullPlayer =
                                true
                        }
                    },

                    onFavorite = {

                        toggleFavorite(it)
                    }
                )
            }
        }

        requestMusicPermission()

        connectToMusicService()
    }

    private fun connectToMusicService() {

        val sessionToken =
            SessionToken(
                this,
                ComponentName(
                    this,
                    MusicService::class.java
                )
            )

        controllerFuture =
            MediaController.Builder(
                this,
                sessionToken
            ).buildAsync()

        controllerFuture.addListener(
            {

                controller =
                    controllerFuture.get()

                controller?.addListener(
                    playerListener
                )

                syncPlayerState()

            },
            ContextCompat.getMainExecutor(
                this
            )
        )
    }

    private fun createMediaItem(
        song: Song
    ): MediaItem {

        val artworkUri =
            Uri.parse(
                "content://media/external/audio/albumart/${song.albumId}"
            )

        val metadata =
            MediaMetadata.Builder()
                .setTitle(
                    song.title
                )
                .setArtist(
                    song.artist
                )
                .setAlbumTitle(
                    song.album
                )
                .setArtworkUri(
                    artworkUri
                )
                .build()

        return MediaItem.Builder()
            .setUri(
                song.uri
            )
            .setMediaMetadata(
                metadata
            )
            .build()
    }

    private fun syncPlayerState() {

        val mediaController =
            controller ?: return

        isPlaying =
            mediaController.isPlaying

        repeatMode =
            mediaController.repeatMode

        shuffleEnabled =
            mediaController.shuffleModeEnabled

        val mediaItem =
            mediaController.currentMediaItem

        val currentUri =
            mediaItem
                ?.localConfiguration
                ?.uri
                ?.toString()

        val newSong =
            songs.firstOrNull {
                it.uri == currentUri
            }

        if (
            newSong?.id !=
            currentSong?.id
        ) {

            currentSong =
                newSong
        }

        updateCurrentQueue()

        updateProgress()
    }

    private fun updateCurrentQueue() {

        val mediaController =
            controller ?: return

        if (songs.isEmpty()) {

            currentQueue =
                emptyList()

            return
        }

        val count =
            mediaController.mediaItemCount

        if (count <= 0) {

            currentQueue =
                emptyList()

            return
        }

        val songsByUri =
            songs.associateBy {
                it.uri
            }

        val result =
            ArrayList<Song>(
                count
            )

        for (
            index in 0 until count
        ) {

            val mediaItem =
                mediaController
                    .getMediaItemAt(index)

            val uri =
                mediaItem
                    .localConfiguration
                    ?.uri
                    ?.toString()
                    ?: continue

            songsByUri[uri]?.let {
                result.add(it)
            }
        }

        currentQueue =
            result
    }

    private fun updateProgress() {

        val mediaController =
            controller ?: return

        currentPosition =
            mediaController
                .currentPosition
                .coerceAtLeast(0L)

        duration =
            mediaController
                .duration
                .takeIf {
                    it > 0L
                }
                ?: currentSong?.duration
                ?: 0L
    }

    private fun playSong(
        song: Song
    ) {

        val mediaController =
            controller ?: return

        if (songs.isEmpty()) {
            return
        }

        val mediaItems =
            songs.map {
                createMediaItem(it)
            }

        val startIndex =
            songs.indexOfFirst {
                it.uri == song.uri
            }
                .coerceAtLeast(0)

        mediaController.setMediaItems(
            mediaItems,
            startIndex,
            0L
        )

        mediaController.shuffleModeEnabled =
            false

        mediaController.prepare()

        mediaController.play()

        syncPlayerState()
    }

    private fun playSongFromCurrentQueue(
        song: Song
    ) {

        val mediaController =
            controller ?: return

        val count =
            mediaController.mediaItemCount

        if (count <= 0) {
            return
        }

        for (
            index in 0 until count
        ) {

            val mediaItem =
                mediaController
                    .getMediaItemAt(index)

            val uri =
                mediaItem
                    .localConfiguration
                    ?.uri
                    ?.toString()

            if (
                uri == song.uri
            ) {

                mediaController.seekTo(
                    index,
                    0L
                )

                mediaController.play()

                syncPlayerState()

                return
            }
        }
    }

    private fun playPause() {

        val mediaController =
            controller ?: return

        if (
            mediaController.isPlaying
        ) {

            mediaController.pause()

        } else {

            mediaController.play()
        }

        isPlaying =
            mediaController.isPlaying
    }

    private fun nextSong() {

        val mediaController =
            controller ?: return

        if (
            mediaController.hasNextMediaItem()
        ) {

            mediaController
                .seekToNextMediaItem()

            mediaController.play()

        } else if (
            mediaController.repeatMode ==
            Player.REPEAT_MODE_ALL
        ) {

            mediaController
                .seekToNextMediaItem()

            mediaController.play()
        }

        syncPlayerState()
    }

    private fun previousSong() {

        val mediaController =
            controller ?: return

        if (
            mediaController.currentPosition >
            3000L
        ) {

            mediaController.seekTo(
                0L
            )

            syncPlayerState()

            return
        }

        if (
            mediaController.hasPreviousMediaItem()
        ) {

            mediaController
                .seekToPreviousMediaItem()

            mediaController.play()
        }

        syncPlayerState()
    }

    private fun shuffleSongs() {

        val mediaController =
            controller ?: return

        if (songs.isEmpty()) {
            return
        }

        val mediaItems =
            songs.map {
                createMediaItem(it)
            }

        val randomIndex =
            songs.indices.random()

        mediaController.setMediaItems(
            mediaItems,
            randomIndex,
            0L
        )

        mediaController.shuffleModeEnabled =
            true

        mediaController.prepare()

        mediaController.play()

        shuffleEnabled =
            true

        syncPlayerState()
    }

    private fun toggleShuffle() {

        val mediaController =
            controller ?: return

        val newValue =
            !mediaController
                .shuffleModeEnabled

        mediaController.shuffleModeEnabled =
            newValue

        shuffleEnabled =
            newValue

        updateCurrentQueue()

        syncPlayerState()
    }

    private fun toggleRepeat() {

        val mediaController =
            controller ?: return

        val newMode =
            when (
                mediaController.repeatMode
            ) {

                Player.REPEAT_MODE_OFF ->
                    Player.REPEAT_MODE_ALL

                Player.REPEAT_MODE_ALL ->
                    Player.REPEAT_MODE_ONE

                else ->
                    Player.REPEAT_MODE_OFF
            }

        mediaController.repeatMode =
            newMode

        repeatMode =
            newMode
    }

    private fun toggleFavorite(
        song: Song
    ) {

        FavoritesManager.toggleFavorite(
            this,
            song.id
        )

        favoriteIds =
            FavoritesManager.getFavorites(
                this
            )
    }

    private fun requestMusicPermission() {

        val permissions =
            if (
                android.os.Build.VERSION.SDK_INT >= 33
            ) {

                arrayOf(
                    android.Manifest.permission.READ_MEDIA_AUDIO,
                    android.Manifest.permission.POST_NOTIFICATIONS
                )

            } else {

                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }

        permissionLauncher.launch(
            permissions
        )
    }

    private fun loadMusic() {

        songs =
            MusicRepository.getSongs(
                contentResolver
            )

        favoriteIds =
            FavoritesManager.getFavorites(
                this
            )

        syncPlayerState()
    }

    override fun onDestroy() {

        controller?.removeListener(
            playerListener
        )

        if (
            ::controllerFuture.isInitialized
        ) {

            MediaController.releaseFuture(
                controllerFuture
            )
        }

        controller =
            null

        super.onDestroy()
    }
}
