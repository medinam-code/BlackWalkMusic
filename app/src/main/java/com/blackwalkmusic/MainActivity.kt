package com.blackwalkmusic

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.compose.SubcomposeAsyncImage
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.delay

/*
 * ============================================================
 * CYBERPUNK GAMER PALETTE
 * ============================================================
 *
 * IMPORTANTE:
 *
 * Estos colores NO se utilizan para la marca.
 *
 * La marca TERERÉ MUSIC PY conserva exclusivamente:
 *
 * ROJO -> BLANCO -> AZUL
 *
 * Todo el resto utiliza negro / verde neón / gris.
 */

private val CyberBlack = Color(0xFF030403)
private val CyberBlack2 = Color(0xFF070907)
private val CyberSurface = Color(0xFF0B0F0B)
private val CyberSurface2 = Color(0xFF111711)

private val CyberGreen = Color(0xFF39FF14)
private val CyberGreenBright = Color(0xFF66FF33)
private val CyberGreenDark = Color(0xFF123A0C)

private val CyberBorder = Color(0xFF1C4218)
private val CyberBorderBright = Color(0xFF2D7A24)

private val CyberGray = Color(0xFF7E897E)
private val CyberGrayDark = Color(0xFF4B514B)

private val CyberWhite = Color(0xFFE9F3E9)

/*
 * ============================================================
 * COLORES EXCLUSIVOS DE LA MARCA
 * ============================================================
 */

private val ParaguayRed = Color(0xFFD90012)
private val ParaguayWhite = Color(0xFFF5F5F5)
private val ParaguayBlue = Color(0xFF0038A8)

private enum class SongSort {
    TITLE,
    ARTIST,
    ALBUM,
    DURATION,
    NEWEST,
    OLDEST
}

class MainActivity : ComponentActivity() {

    private lateinit var controllerFuture: ListenableFuture<MediaController>

    private var controller: MediaController? = null

    private var songs by mutableStateOf<List<Song>>(emptyList())

    private var currentSong by mutableStateOf<Song?>(null)

    private var isPlaying by mutableStateOf(false)

    private var currentPosition by mutableLongStateOf(0L)

    private var duration by mutableLongStateOf(0L)

    private var repeatMode by mutableIntStateOf(
        Player.REPEAT_MODE_OFF
    )

    private var shuffleEnabled by mutableStateOf(false)

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
                isPlaying = isPlayingNow
            }

            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int
            ) {
                updateCurrentSong()
                updateProgress()
            }

            override fun onPlaybackStateChanged(
                playbackState: Int
            ) {
                updateProgress()
            }

            override fun onRepeatModeChanged(
                mode: Int
            ) {
                repeatMode = mode
            }

            override fun onShuffleModeEnabledChanged(
                shuffleModeEnabled: Boolean
            ) {
                shuffleEnabled = shuffleModeEnabled
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContent {

            var showFullPlayer by remember {
                mutableStateOf(false)
            }

            LaunchedEffect(
                isPlaying,
                currentSong
            ) {

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
                    song = currentSong!!,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    repeatMode = repeatMode,
                    shuffleEnabled = shuffleEnabled,

                    onBack = {
                        showFullPlayer = false
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

                        controller?.seekTo(position)

                        currentPosition = position
                    },

                    onShuffle = {
                        toggleShuffle()
                    },

                    onRepeat = {
                        toggleRepeat()
                    }
                )

            } else {

                BlackWalkMusicScreen(
                    songs = songs,
                    currentSong = currentSong,
                    isPlaying = isPlaying,

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

                        if (currentSong != null) {
                            showFullPlayer = true
                        }
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

                isPlaying =
                    controller?.isPlaying == true

                repeatMode =
                    controller?.repeatMode
                        ?: Player.REPEAT_MODE_OFF

                shuffleEnabled =
                    controller?.shuffleModeEnabled == true

                updateCurrentSong()

                updateProgress()

            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun updateCurrentSong() {

        val mediaController =
            controller ?: return

        val index =
            mediaController.currentMediaItemIndex

        if (
            index >= 0 &&
            index < songs.size
        ) {

            currentSong =
                songs[index]

        } else {

            val uri =
                mediaController
                    .currentMediaItem
                    ?.localConfiguration
                    ?.uri
                    ?.toString()

            currentSong =
                songs.find {
                    it.uri == uri
                }
        }

        isPlaying =
            mediaController.isPlaying
    }

    private fun updateProgress() {

        val mediaController =
            controller ?: return

        currentPosition =
            mediaController.currentPosition
                .coerceAtLeast(0L)

        duration =
            mediaController.duration
                .takeIf {
                    it > 0
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
                MediaItem.fromUri(it.uri)
            }

        val startIndex =
            songs.indexOfFirst {
                it.uri == song.uri
            }.coerceAtLeast(0)

        mediaController.setMediaItems(
            mediaItems,
            startIndex,
            0L
        )

        mediaController.prepare()

        mediaController.play()

        currentSong =
            song

        isPlaying = true
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
    }

    private fun nextSong() {

        val mediaController =
            controller ?: return

        if (
            mediaController.hasNextMediaItem()
        ) {

            mediaController.seekToNextMediaItem()

            mediaController.play()

        } else if (
            mediaController.repeatMode ==
            Player.REPEAT_MODE_ALL
        ) {

            mediaController.seekToNextMediaItem()

            mediaController.play()
        }
    }

    private fun previousSong() {

        val mediaController =
            controller ?: return

        if (
            mediaController.currentPosition >
            3000L
        ) {

            mediaController.seekTo(0L)

            return
        }

        if (
            mediaController.hasPreviousMediaItem()
        ) {

            mediaController
                .seekToPreviousMediaItem()

            mediaController.play()
        }
    }

    private fun shuffleSongs() {

        val mediaController =
            controller ?: return

        if (songs.isEmpty()) {
            return
        }

        val shuffledSongs =
            songs.shuffled()

        val mediaItems =
            shuffledSongs.map {
                MediaItem.fromUri(it.uri)
            }

        mediaController.setMediaItems(
            mediaItems,
            0,
            0L
        )

        mediaController.shuffleModeEnabled =
            true

        mediaController.prepare()

        mediaController.play()

        currentSong =
            shuffledSongs.firstOrNull()

        isPlaying = true

        shuffleEnabled = true
    }

    private fun toggleShuffle() {

        val mediaController =
            controller ?: return

        mediaController.shuffleModeEnabled =
            !mediaController.shuffleModeEnabled

        shuffleEnabled =
            mediaController.shuffleModeEnabled
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

        updateCurrentSong()
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

        controller = null

        super.onDestroy()
    }
}

/*
 * ============================================================
 * MARCA
 *
 * ÚNICA PARTE DE LA INTERFAZ QUE UTILIZA:
 *
 * ROJO - BLANCO - AZUL
 * ============================================================
 */

@Composable
fun TerereBrand(
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
    ) {

        Row {

            Text(
                text = "TERERÉ",
                color = ParaguayRed,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = " MUSIC",
                color = ParaguayWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = " PY",
                color = ParaguayBlue,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
        ) {

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            ParaguayRed
                        )
            )

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            ParaguayWhite
                        )
            )

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            ParaguayBlue
                        )
            )
        }
    }
}

/*
 * ============================================================
 * PANTALLA PRINCIPAL
 * ============================================================
 */

@Composable
fun BlackWalkMusicScreen(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onOpenPlayer: () -> Unit
) {

    var searchText by remember {
        mutableStateOf("")
    }

    var sortMode by remember {
        mutableStateOf(
            SongSort.TITLE
        )
    }

    var showSortMenu by remember {
        mutableStateOf(false)
    }

    val filteredSongs =
        remember(
            songs,
            searchText,
            sortMode
        ) {

            val query =
                searchText.trim()

            val filtered =
                if (
                    query.isEmpty()
                ) {

                    songs

                } else {

                    songs.filter { song ->

                        song.title.contains(
                            query,
                            ignoreCase = true
                        ) ||

                        song.artist.contains(
                            query,
                            ignoreCase = true
                        ) ||

                        song.album.contains(
                            query,
                            ignoreCase = true
                        )
                    }
                }

            when (sortMode) {

                SongSort.TITLE ->
                    filtered.sortedBy {
                        it.title.lowercase()
                    }

                SongSort.ARTIST ->
                    filtered.sortedBy {
                        it.artist.lowercase()
                    }

                SongSort.ALBUM ->
                    filtered.sortedBy {
                        it.album.lowercase()
                    }

                SongSort.DURATION ->
                    filtered.sortedByDescending {
                        it.duration
                    }

                SongSort.NEWEST ->
                    filtered.sortedByDescending {
                        it.dateAdded
                    }

                SongSort.OLDEST ->
                    filtered.sortedBy {
                        it.dateAdded
                    }
            }
        }

    MaterialTheme(
        colorScheme =
            darkColorScheme(
                background =
                    CyberBlack,

                surface =
                    CyberSurface,

                primary =
                    CyberGreen,

                onPrimary =
                    Color.Black,

                secondary =
                    CyberGreen,

                onSecondary =
                    Color.Black
            )
    ) {

        Surface(
            modifier =
                Modifier.fillMaxSize(),

            color =
                CyberBlack
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

                /*
                 * CABECERA
                 */

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    TerereBrand(
                        modifier =
                            Modifier.weight(1f)
                    )

                    IconButton(
                        onClick =
                            onShuffle
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Shuffle,

                            contentDescription =
                                "Aleatorio",

                            tint =
                                CyberGreen
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )

                /*
                 * BUSCADOR CYBERPUNK
                 */

                OutlinedTextField(
                    value =
                        searchText,

                    onValueChange = {
                        searchText = it
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color =
                                    if (
                                        searchText.isNotEmpty()
                                    )
                                        CyberBorderBright
                                    else
                                        CyberBorder,
                                shape =
                                    RoundedCornerShape(14.dp)
                            ),

                    singleLine = true,

                    leadingIcon = {

                        Icon(
                            imageVector =
                                Icons.Default.Search,

                            contentDescription =
                                null,

                            tint =
                                CyberGreen
                        )
                    },

                    trailingIcon = {

                        if (
                            searchText.isNotEmpty()
                        ) {

                            IconButton(
                                onClick = {
                                    searchText = ""
                                }
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.Clear,

                                    contentDescription =
                                        "Limpiar",

                                    tint =
                                        CyberGray
                                )
                            }
                        }
                    },

                    placeholder = {

                        Text(
                            text =
                                "BUSCAR CANCIÓN, ARTISTA O ÁLBUM",

                            color =
                                CyberGrayDark,

                            fontSize =
                                12.sp
                        )
                    },

                    colors =
                        OutlinedTextFieldDefaults.colors(

                            focusedTextColor =
                                CyberWhite,

                            unfocusedTextColor =
                                CyberWhite,

                            focusedBorderColor =
                                CyberGreen,

                            unfocusedBorderColor =
                                CyberBorder,

                            cursorColor =
                                CyberGreen,

                            focusedLabelColor =
                                CyberGreen,

                            unfocusedLabelColor =
                                CyberGray
                        ),

                    shape =
                        RoundedCornerShape(14.dp)
                )

                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )

                /*
                 * CABECERA DE BIBLIOTECA
                 */

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text =
                                "BIBLIOTECA",

                            color =
                                CyberWhite,

                            fontSize =
                                23.sp,

                            fontWeight =
                                FontWeight.Bold,

                            letterSpacing =
                                2.sp
                        )

                        Text(
                            text =
                                if (
                                    searchText.isEmpty()
                                ) {

                                    "${songs.size} CANCIONES"

                                } else {

                                    "${filteredSongs.size} RESULTADOS"
                                },

                            color =
                                CyberGreen,

                            fontSize =
                                11.sp,

                            letterSpacing =
                                1.sp
                        )
                    }

                    Box {

                        IconButton(
                            onClick = {

                                showSortMenu =
                                    !showSortMenu
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Sort,

                                contentDescription =
                                    "Ordenar",

                                tint =
                                    CyberGreen
                            )
                        }

                        DropdownMenu(
                            expanded =
                                showSortMenu,

                            onDismissRequest = {

                                showSortMenu =
                                    false
                            },

                            containerColor =
                                CyberSurface
                        ) {

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Más recientes",
                                        color =
                                            CyberWhite
                                    )
                                },

                                onClick = {

                                    sortMode =
                                        SongSort.NEWEST

                                    showSortMenu =
                                        false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Más antiguas",
                                        color =
                                            CyberWhite
                                    )
                                },

                                onClick = {

                                    sortMode =
                                        SongSort.OLDEST

                                    showSortMenu =
                                        false
                                }
                            )

                            HorizontalDivider(
                                color =
                                    CyberBorder
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Título",
                                        color =
                                            CyberWhite
                                    )
                                },

                                onClick = {

                                    sortMode =
                                        SongSort.TITLE

                                    showSortMenu =
                                        false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Artista",
                                        color =
                                            CyberWhite
                                    )
                                },

                                onClick = {

                                    sortMode =
                                        SongSort.ARTIST

                                    showSortMenu =
                                        false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Álbum",
                                        color =
                                            CyberWhite
                                    )
                                },

                                onClick = {

                                    sortMode =
                                        SongSort.ALBUM

                                    showSortMenu =
                                        false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Duración",
                                        color =
                                            CyberWhite
                                    )
                                },

                                onClick = {

                                    sortMode =
                                        SongSort.DURATION

                                    showSortMenu =
                                        false
                                }
                            )
                        }
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                /*
                 * LISTA DE CANCIONES
                 */

                if (
                    filteredSongs.isEmpty()
                ) {

                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth(),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            GenericAlbumArt(
                                size = 80.dp
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(14.dp)
                            )

                            Text(
                                text =
                                    if (
                                        searchText.isEmpty()
                                    ) {

                                        "NO SE ENCONTRARON CANCIONES"

                                    } else {

                                        "NO HAY RESULTADOS"
                                    },

                                color =
                                    CyberGray,

                                fontSize =
                                    12.sp,

                                letterSpacing =
                                    1.sp
                            )
                        }
                    }

                } else {

                    LazyColumn(
                        modifier =
                            Modifier.weight(1f),

                        contentPadding =
                            PaddingValues(
                                bottom = 8.dp
                            )
                    ) {

                        items(
                            items =
                                filteredSongs,

                            key = {
                                it.id
                            }
                        ) { song ->

                            SongItem(
                                song =
                                    song,

                                isCurrent =
                                    currentSong?.id ==
                                        song.id,

                                onClick = {
                                    onSongClick(song)
                                }
                            )
                        }
                    }
                }

                /*
                 * MINI PLAYER
                 */

                if (
                    currentSong != null
                ) {

                    MiniPlayer(
                        song =
                            currentSong,

                        isPlaying =
                            isPlaying,

                        onPlayPause =
                            onPlayPause,

                        onNext =
                            onNext,

                        onPrevious =
                            onPrevious,

                        onOpenPlayer =
                            onOpenPlayer
                    )
                }
            }
        }
    }
}

/*
 * ============================================================
 * ITEM DE CANCIÓN
 * ============================================================
 */

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

        AlbumArt(
            song =
                song,

            size =
                58.dp
        )

        Spacer(
            modifier =
                Modifier.width(14.dp)
        )

        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                text =
                    song.title,

                color =
                    if (isCurrent)
                        CyberGreenBright
                    else
                        CyberWhite,

                fontSize =
                    16.sp,

                fontWeight =
                    if (isCurrent)
                        FontWeight.Bold
                    else
                        FontWeight.Normal,

                maxLines =
                    2
            )

            Text(
                text =
                    song.artist,

                color =
                    CyberGray,

                fontSize =
                    13.sp,

                maxLines =
                    1
            )

            if (
                song.album.isNotBlank()
            ) {

                Text(
                    text =
                        song.album,

                    color =
                        CyberGrayDark,

                    fontSize =
                        11.sp,

                    maxLines =
                        1
                )
            }
        }

        if (
            isCurrent
        ) {

            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            CyberGreen
                        )
                        .border(
                            1.dp,
                            CyberGreenBright,
                            CircleShape
                        )
            )
        }
    }
}

/*
 * ============================================================
 * CARÁTULA
 * ============================================================
 */

@Composable
fun AlbumArt(
    song: Song,
    size: Dp,
    modifier: Modifier = Modifier
) {

    val albumArtUri =
        "content://media/external/audio/albumart/${song.albumId}"

    Box(
        modifier =
            modifier
                .size(size)
                .clip(
                    RoundedCornerShape(10.dp)
                )
                .background(
                    CyberSurface2
                )
                .border(
                    1.dp,
                    CyberBorder,
                    RoundedCornerShape(10.dp)
                ),

        contentAlignment =
            Alignment.Center
    ) {

        SubcomposeAsyncImage(
            model =
                albumArtUri,

            contentDescription =
                "Carátula de ${song.album}",

            modifier =
                Modifier.fillMaxSize(),

            contentScale =
                ContentScale.Crop,

            loading = {

                GenericAlbumArt(
                    size =
                        size
                )
            },

            error = {

                GenericAlbumArt(
                    size =
                        size
                )
            }
        )
    }
}

/*
 * ============================================================
 * CARÁTULA CYBERPUNK
 * ============================================================
 */

@Composable
fun GenericAlbumArt(
    size: Dp
) {

    Box(
        modifier =
            Modifier
                .size(size)
                .clip(
                    RoundedCornerShape(10.dp)
                )
                .background(
                    CyberBlack
                )
                .border(
                    1.dp,
                    CyberBorderBright,
                    RoundedCornerShape(10.dp)
                ),

        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector =
                    Icons.Default.MusicNote,

                contentDescription =
                    null,

                tint =
                    CyberGreen,

                modifier =
                    Modifier.size(
                        size * 0.42f
                    )
            )

            Text(
                text =
                    "T•PY",

                color =
                    CyberGreenBright,

                fontSize =
                    (size.value * 0.11f).sp,

                fontWeight =
                    FontWeight.Bold,

                letterSpacing =
                    1.sp
            )
        }
    }
}

/*
 * ============================================================
 * MINI PLAYER
 * ============================================================
 */

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenPlayer: () -> Unit
) {

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onOpenPlayer()
                },

        color =
            CyberSurface,

        shape =
            RoundedCornerShape(18.dp),

        border =
            BorderStroke(
                1.dp,
                CyberBorderBright
            )
    ) {

        Column(
            modifier =
                Modifier.padding(10.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                AlbumArt(
                    song =
                        song,

                    size =
                        55.dp
                )

                Spacer(
                    modifier =
                        Modifier.width(12.dp)
                )

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            song.title,

                        color =
                            CyberWhite,

                        fontWeight =
                            FontWeight.Bold,

                        maxLines =
                            1
                    )

                    Text(
                        text =
                            song.artist,

                        color =
                            CyberGray,

                        fontSize =
                            12.sp,

                        maxLines =
                            1
                    )
                }

                Icon(
                    imageVector =
                        Icons.Default.ExpandLess,

                    contentDescription =
                        "Abrir reproductor",

                    tint =
                        CyberGreen
                )
            }

            Spacer(
                modifier =
                    Modifier.height(3.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.Center,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                IconButton(
                    onClick =
                        onPrevious
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.SkipPrevious,

                        contentDescription =
                            "Anterior",

                        tint =
                            CyberWhite
                    )
                }

                IconButton(
                    onClick =
                        onPlayPause
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

                        tint =
                            CyberGreen,

                        modifier =
                            Modifier.size(34.dp)
                    )
                }

                IconButton(
                    onClick =
                        onNext
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.SkipNext,

                        contentDescription =
                            "Siguiente",

                        tint =
                            CyberWhite
                    )
                }
            }
        }
    }
}

/*
 * ============================================================
 * REPRODUCTOR COMPLETO
 * ============================================================
 */

@Composable
fun FullPlayerScreen(
    song: Song,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit
) {

    val safeDuration =
        duration.coerceAtLeast(1L)

    val safePosition =
        currentPosition.coerceIn(
            0L,
            safeDuration
        )

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    CyberBlack
                )
                .padding(
                    horizontal = 22.dp,
                    vertical = 18.dp
                )
    ) {

        /*
         * CABECERA
         */

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick =
                    onBack
            ) {

                Icon(
                    imageVector =
                        Icons.Default.ArrowBack,

                    contentDescription =
                        "Volver",

                    tint =
                        CyberGreen
                )
            }

            Text(
                text =
                    "TERERÉ // NOW PLAYING",

                color =
                    CyberGreen,

                fontSize =
                    11.sp,

                letterSpacing =
                    2.sp,

                modifier =
                    Modifier.weight(1f)
            )
        }

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        /*
         * CARÁTULA PRINCIPAL
         */

        AlbumArt(
            song =
                song,

            size =
                310.dp,

            modifier =
                Modifier.align(
                    Alignment.CenterHorizontally
                )
        )

        Spacer(
            modifier =
                Modifier.height(28.dp)
        )

        /*
         * INFORMACIÓN DE LA CANCIÓN
         */

        Text(
            text =
                song.title,

            color =
                CyberWhite,

            fontSize =
                25.sp,

            fontWeight =
                FontWeight.Bold,

            maxLines =
                2
        )

        Spacer(
            modifier =
                Modifier.height(5.dp)
        )

        Text(
            text =
                song.artist,

            color =
                CyberGreen,

            fontSize =
                16.sp,

            maxLines =
                1
        )

        if (
            song.album.isNotBlank()
        ) {

            Text(
                text =
                    song.album,

                color =
                    CyberGray,

                fontSize =
                    13.sp,

                maxLines =
                    1
            )
        }

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        /*
         * BARRA DE PROGRESO
         */

        Slider(
            value =
                safePosition.toFloat(),

            onValueChange = {

                onSeek(
                    it.toLong()
                )
            },

            valueRange =
                0f..safeDuration.toFloat(),

            modifier =
                Modifier.fillMaxWidth(),

            colors =
                SliderDefaults.colors(

                    thumbColor =
                        CyberGreen,

                    activeTrackColor =
                        CyberGreen,

                    inactiveTrackColor =
                        CyberBorder
                )
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(
                text =
                    formatTime(
                        safePosition
                    ),

                color =
                    CyberGray,

                fontSize =
                    12.sp
            )

            Text(
                text =
                    formatTime(
                        safeDuration
                    ),

                color =
                    CyberGray,

                fontSize =
                    12.sp
            )
        }

        Spacer(
            modifier =
                Modifier.height(22.dp)
        )

        /*
         * CONTROLES
         */

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceEvenly,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick =
                    onShuffle
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Shuffle,

                    contentDescription =
                        "Aleatorio",

                    tint =
                        if (shuffleEnabled)
                            CyberGreen
                        else
                            CyberGray
                )
            }

            IconButton(
                onClick =
                    onPrevious
            ) {

                Icon(
                    imageVector =
                        Icons.Default.SkipPrevious,

                    contentDescription =
                        "Anterior",

                    tint =
                        CyberWhite,

                    modifier =
                        Modifier.size(38.dp)
                )
            }

            /*
             * BOTÓN PRINCIPAL
             */

            IconButton(
                onClick =
                    onPlayPause,

                modifier =
                    Modifier
                        .size(72.dp)
                        .background(
                            CyberGreen,
                            CircleShape
                        )
                        .border(
                            2.dp,
                            CyberGreenBright,
                            CircleShape
                        )
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

                    tint =
                        Color.Black,

                    modifier =
                        Modifier.size(39.dp)
                )
            }

            IconButton(
                onClick =
                    onNext
            ) {

                Icon(
                    imageVector =
                        Icons.Default.SkipNext,

                    contentDescription =
                        "Siguiente",

                    tint =
                        CyberWhite,

                    modifier =
                        Modifier.size(38.dp)
                )
            }

            IconButton(
                onClick =
                    onRepeat
            ) {

                Icon(
                    imageVector =
                        when (repeatMode) {

                            Player.REPEAT_MODE_ONE ->
                                Icons.Default.RepeatOne

                            else ->
                                Icons.Default.Repeat
                        },

                    contentDescription =
                        "Repetición",

                    tint =
                        if (
                            repeatMode !=
                            Player.REPEAT_MODE_OFF
                        )
                            CyberGreen
                        else
                            CyberGray
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        /*
         * MARCA
         *
         * Esta es la única parte donde vuelven
         * a aparecer rojo / blanco / azul.
         */

        TerereBrand(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 35.dp
                    )
        )
    }
}

/*
 * ============================================================
 * FORMATO DE TIEMPO
 * ============================================================
 */

private fun formatTime(
    milliseconds: Long
): String {

    val totalSeconds =
        (milliseconds / 1000L)
            .coerceAtLeast(0L)

    val minutes =
        totalSeconds / 60L

    val seconds =
        totalSeconds % 60L

    return "%d:%02d".format(
        minutes,
        seconds
    )
}
