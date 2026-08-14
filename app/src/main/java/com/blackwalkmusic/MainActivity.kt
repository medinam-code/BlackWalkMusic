package com.blackwalkmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

class MainActivity : ComponentActivity() {

    private lateinit var controllerFuture: ListenableFuture<MediaController>

    private var controller: MediaController? = null

    private var songs by mutableStateOf<List<Song>>(emptyList())

    private var currentSong by mutableStateOf<Song?>(null)

    private var isPlaying by mutableStateOf(false)

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            loadMusic()
        }

    private val playerListener = object : Player.Listener {

        override fun onIsPlayingChanged(isPlayingNow: Boolean) {
            isPlaying = isPlayingNow
        }

        override fun onMediaItemTransition(
            mediaItem: MediaItem?,
            reason: Int
        ) {
            val uri = mediaItem?.localConfiguration?.uri?.toString()

            currentSong = songs.find {
                it.uri == uri
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestMusicPermission()

        connectToMusicService()

        setContent {
            BlackWalkMusicScreen(
                songs = songs,
                currentSong = currentSong,
                isPlaying = isPlaying,
                onSongClick = { song ->
                    playSong(song)
                },
                onPlayPause = {
                    playPause()
                },
                onNext = {
                    controller?.seekToNextMediaItem()
                },
                onPrevious = {
                    controller?.seekToPreviousMediaItem()
                }
            )
        }
    }

    private fun connectToMusicService() {

        val sessionToken =
    SessionToken(
        this,
        android.content.ComponentName(
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

                controller = controllerFuture.get()

                controller?.addListener(playerListener)

                isPlaying =
                    controller?.isPlaying == true

            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun playSong(song: Song) {

        val mediaController = controller ?: return

        val mediaItem =
            MediaItem.fromUri(song.uri)

        mediaController.setMediaItem(mediaItem)

        mediaController.prepare()

        mediaController.play()

        currentSong = song
        isPlaying = true
    }

    private fun playPause() {

        val mediaController = controller ?: return

        if (mediaController.isPlaying) {
            mediaController.pause()
        } else {
            mediaController.play()
        }
    }

    private fun requestMusicPermission() {

        val permissions =
            if (android.os.Build.VERSION.SDK_INT >= 33) {

                arrayOf(
                    android.Manifest.permission.READ_MEDIA_AUDIO,
                    android.Manifest.permission.POST_NOTIFICATIONS
                )

            } else {

                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }

        permissionLauncher.launch(permissions)
    }

    private fun loadMusic() {

        songs =
            MusicRepository.getSongs(
                contentResolver
            )
    }

    override fun onDestroy() {

        controller?.removeListener(playerListener)

        MediaController.releaseFuture(controllerFuture)

        super.onDestroy()
    }
}

@Composable
fun BlackWalkMusicScreen(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {

    MaterialTheme(
        colorScheme =
            darkColorScheme(
                background = Color.Black,
                surface = Color(0xFF111111),
                primary = Color.White
            )
    ) {

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 18.dp,
                            vertical = 18.dp
                        )
            ) {

                Text(
                    text = "BLACKWALK",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "MUSIC",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    letterSpacing = 4.sp
                )

                Spacer(
                    modifier =
                        Modifier.height(24.dp)
                )

                Text(
                    text = "Canciones",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                if (songs.isEmpty()) {

                    Box(
                        modifier =
                            Modifier.fillMaxSize(),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text = "No se encontraron canciones",
                            color = Color.Gray
                        )
                    }

                } else {

                    LazyColumn(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        items(
                            items = songs,
                            key = { it.id }
                        ) { song ->

                            SongItem(
                                song = song,
                                isCurrent =
                                    currentSong?.id == song.id,
                                onClick = {
                                    onSongClick(song)
                                }
                            )
                        }
                    }
                }

                if (currentSong != null) {

                    MiniPlayer(
                        song = currentSong,
                        isPlaying = isPlaying,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        onPrevious = onPrevious
                    )
                }
            }
        }
    }
}

@Composable
fun SongItem(
    song: Song,
    isCurrent: Boolean,
    onClick: () -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                }
                .padding(
                    vertical = 9.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier =
                Modifier
                    .size(58.dp)
                    .background(
                        Color(0xFF202020)
                    ),
            contentAlignment =
                Alignment.Center
        ) {

            Icon(
                imageVector =
                    Icons.Default.MusicNote,
                contentDescription = null,
                tint =
                    if (isCurrent)
                        Color.White
                    else
                        Color.Gray,
                modifier =
                    Modifier.size(30.dp)
            )
        }

        Spacer(
            modifier =
                Modifier.width(14.dp)
        )

        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                text = song.title,
                color =
                    if (isCurrent)
                        Color.White
                    else
                        Color(0xFFE8E8E8),
                fontSize = 16.sp,
                fontWeight =
                    if (isCurrent)
                        FontWeight.Bold
                    else
                        FontWeight.Normal,
                maxLines = 2
            )

            Text(
                text = song.artist,
                color = Color.Gray,
                fontSize = 13.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {

    Surface(
        modifier =
            Modifier.fillMaxWidth(),
        color =
            Color(0xFF181818),
        shape =
            MaterialTheme.shapes.large
    ) {

        Column(
            modifier =
                Modifier.padding(12.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(55.dp)
                            .background(
                                Color(0xFF292929)
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(12.dp)
                )

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text = song.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Text(
                        text = song.artist,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.Center,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = onPrevious
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.SkipPrevious,
                        contentDescription =
                            "Anterior",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = onPlayPause
                ) {

                    Icon(
                        imageVector =
                            if (isPlaying)
                                Icons.Default.Pause
                            else
                                Icons.Default.PlayArrow,
                        contentDescription =
                            if (isPlaying)
                                "Pausar"
                            else
                                "Reproducir",
                        tint = Color.White,
                        modifier =
                            Modifier.size(34.dp)
                    )
                }

                IconButton(
                    onClick = onNext
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.SkipNext,
                        contentDescription =
                            "Siguiente",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
