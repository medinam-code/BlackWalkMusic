package com.blackwalkmusic

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * COLORES
 * ============================================================
 */

private val AmoledBlack = Color(0xFF000000)
private val AmoledSurface = Color(0xFF090909)
private val AmoledSurface2 = Color(0xFF111111)

private val TextWhite = Color(0xFFF5F5F5)
private val TextGray = Color(0xFF8A8A8A)
private val TextGrayDark = Color(0xFF555555)

private val ParaguayRed = Color(0xFFD90012)
private val ParaguayWhite = Color(0xFFF5F5F5)
private val ParaguayBlue = Color(0xFF0038A8)

private val SectionLineGray = Color(0xFF303030)

/*
 * ============================================================
 * ORDENAMIENTO
 * ============================================================
 */

private enum class SongSort {
    TITLE,
    ARTIST,
    ALBUM,
    DURATION,
    NEWEST,
    OLDEST
}

/*
 * ============================================================
 * MAIN ACTIVITY
 * ============================================================
 */

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
                updateProgress()
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

        /*
         * La interfaz se dibuja inmediatamente.
         *
         * No cargamos música dentro de setContent.
         * Esto evita que un problema del reproductor
         * deje la pantalla bloqueada en gris.
         */

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

        /*
         * Primero mostramos la interfaz.
         * Después iniciamos permisos y servicio.
         */

        requestMusicPermission()

        connectToMusicService()
    }

    /*
     * ========================================================
     * MEDIA CONTROLLER
     * ========================================================
     */

    private fun connectToMusicService() {

        try {

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

                    try {

                        controller =
                            controllerFuture.get()

                        controller?.addListener(
                            playerListener
                        )

                        controller?.let { player ->

                            isPlaying =
                                player.isPlaying

                            repeatMode =
                                player.repeatMode

                            shuffleEnabled =
                                player.shuffleModeEnabled
                        }

                        updateCurrentSong()
                        updateProgress()

                    } catch (_: Exception) {
                        /*
                         * Si Media3 falla, la interfaz
                         * sigue funcionando.
                         */
                    }

                },
                ContextCompat.getMainExecutor(this)
            )

        } catch (_: Exception) {
            /*
             * Evita que un problema del servicio
             * cierre la aplicación.
             */
        }
    }

    private fun updateCurrentSong() {

        val mediaController =
            controller ?: return

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
                    it > 0L
                }
                ?: currentSong?.duration
                ?: 0L
    }

    /*
     * ========================================================
     * REPRODUCCIÓN
     * ========================================================
     */

    private fun playSong(
        song: Song
    ) {

        val mediaController =
            controller ?: return

        if (songs.isEmpty()) {
            return
        }

        try {

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

            currentSong = song
            isPlaying = true

        } catch (_: Exception) {
            isPlaying = false
        }
    }

    private fun playPause() {

        val mediaController =
            controller ?: return

        try {

            if (mediaController.isPlaying) {
                mediaController.pause()
            } else {
                mediaController.play()
            }

        } catch (_: Exception) {
        }
    }

    private fun nextSong() {

        val mediaController =
            controller ?: return

        try {

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

        } catch (_: Exception) {
        }
    }

    private fun previousSong() {

        val mediaController =
            controller ?: return

        try {

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

        } catch (_: Exception) {
        }
    }

    private fun shuffleSongs() {

        val mediaController =
            controller ?: return

        if (songs.isEmpty()) {
            return
        }

        try {

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

        } catch (_: Exception) {
        }
    }

    private fun toggleShuffle() {

        val mediaController =
            controller ?: return

        try {

            mediaController.shuffleModeEnabled =
                !mediaController.shuffleModeEnabled

            shuffleEnabled =
                mediaController.shuffleModeEnabled

        } catch (_: Exception) {
        }
    }

    private fun toggleRepeat() {

        val mediaController =
            controller ?: return

        try {

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

        } catch (_: Exception) {
        }
    }

    /*
     * ========================================================
     * PERMISOS
     * ========================================================
     */

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

        try {

            permissionLauncher.launch(
                permissions
            )

        } catch (_: Exception) {
            loadMusic()
        }
    }

    private fun loadMusic() {

        try {

            songs =
                MusicRepository.getSongs(
                    contentResolver
                )

            updateCurrentSong()

        } catch (_: Exception) {

            songs = emptyList()
        }
    }

    /*
     * ========================================================
     * DESTROY
     * ========================================================
     */

    override fun onDestroy() {

        try {

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

        } catch (_: Exception) {
        }

        controller = null

        super.onDestroy()
    }
}

/*
 * ============================================================
 * TEMA AMOLED
 * ============================================================
 */

private val AmoledColorScheme =
    darkColorScheme(
        primary = ParaguayBlue,
        onPrimary = Color.White,
        background = AmoledBlack,
        onBackground = TextWhite,
        surface = AmoledBlack,
        onSurface = TextWhite
    )

/*
 * ============================================================
 * MARCA
 * ============================================================
 */

@Composable
private fun TerereBrand(
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
    ) {

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = "TERERÉ",
                color = ParaguayRed,
                fontSize = 27.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = " MUSIC",
                color = ParaguayWhite,
                fontSize = 27.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = " PY",
                color = ParaguayBlue,
                fontSize = 27.sp,
                fontWeight = FontWeight.Black
            )
        }

        ParaguayLine(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp)
        )
    }
}

/*
 * ============================================================
 * LÍNEA PARAGUAYA
 * ============================================================
 */

@Composable
private fun ParaguayLine(
    modifier: Modifier = Modifier
) {

    Row(
        modifier =
            modifier.height(3.dp)
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

/*
 * ============================================================
 * TÍTULO DE SECCIÓN
 * ============================================================
 */

@Composable
private fun SectionTitle(
    title: String
) {

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Text(
            text = title,
            color = TextWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(
            modifier =
                Modifier.height(7.dp)
        )

        ParaguayLine(
            modifier =
                Modifier.fillMaxWidth()
        )
    }
}

/*
 * ============================================================
 * PANTALLA PRINCIPAL
 * ============================================================
 */

@Composable
private fun BlackWalkMusicScreen(
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
                if (query.isEmpty()) {
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
        colorScheme = AmoledColorScheme
    ) {

        Surface(
            modifier =
                Modifier.fillMaxSize(),

            color =
                AmoledBlack
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
                                ParaguayBlue
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )

                /*
                 * BUSCADOR
                 */

                OutlinedTextField(
                    value =
                        searchText,

                    onValueChange = {
                        searchText = it
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    singleLine = true,

                    leadingIcon = {

                        Icon(
                            imageVector =
                                Icons.Default.Search,

                            contentDescription =
                                null,

                            tint =
                                ParaguayBlue
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
                                        TextGray
                                )
                            }
                        }
                    },

                    placeholder = {

                        Text(
                            text =
                                "Buscar canción, artista o álbum",

                            color =
                                TextGrayDark
                        )
                    },

                    colors =
                        OutlinedTextFieldDefaults.colors(

                            focusedTextColor =
                                TextWhite,

                            unfocusedTextColor =
                                TextWhite,

                            focusedBorderColor =
                                ParaguayBlue,

                            unfocusedBorderColor =
                                SectionLineGray,

                            cursorColor =
                                ParaguayBlue
                        ),

                    shape =
                        RoundedCornerShape(12.dp)
                )

                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )

                /*
                 * BIBLIOTECA
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

                        SectionTitle(
                            title = "BIBLIOTECA"
                        )

                        Spacer(
                            modifier =
                                Modifier.height(5.dp)
                        )

                        Text(
                            text =
                                if (
                                    searchText.isEmpty()
                                ) {
                                    "${songs.size} canciones"
                                } else {
                                    "${filteredSongs.size} resultados"
                                },

                            color =
                                TextGray,

                            fontSize =
                                12.sp
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
                                    TextWhite
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
                                AmoledSurface
                        ) {

                            SortItem(
                                text = "Título",
                                onClick = {
                                    sortMode =
                                        SongSort.TITLE
                                    showSortMenu =
                                        false
                                }
                            )

                            SortItem(
                                text = "Artista",
                                onClick = {
                                    sortMode =
                                        SongSort.ARTIST
                                    showSortMenu =
                                        false
                                }
                            )

                            SortItem(
                                text = "Álbum",
                                onClick = {
                                    sortMode =
                                        SongSort.ALBUM
                                    showSortMenu =
                                        false
                                }
                            )

                            SortItem(
                                text = "Duración",
                                onClick = {
                                    sortMode =
                                        SongSort.DURATION
                                    showSortMenu =
                                        false
                                }
                            )

                            HorizontalDivider(
                                color =
                                    SectionLineGray
                            )

                            SortItem(
                                text = "Más recientes",
                                onClick = {
                                    sortMode =
                                        SongSort.NEWEST
                                    showSortMenu =
                                        false
                                }
                            )

                            SortItem(
                                text = "Más antiguas",
                                onClick = {
                                    sortMode =
                                        SongSort.OLDEST
                                    showSortMenu =
                                        false
                                }
                            )
                        }
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                /*
                 * LISTA
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
                                    Modifier.height(12.dp)
                            )

                            Text(
                                text =
                                    if (
                                        searchText.isEmpty()
                                    ) {
                                        "No se encontraron canciones"
                                    } else {
                                        "No hay resultados"
                                    },

                                color =
                                    TextGray,

                                fontSize =
                                    13.sp
                            )
                        }
                    }

                } else {

                    LazyColumn(
                        modifier =
                            Modifier.weight(1f),

                        contentPadding =
                            PaddingValues(
                                bottom = 10.dp
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
                 * REPRODUCCIÓN
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

@Composable
private fun SortItem(
    text: String,
    onClick: () -> Unit
) {

    DropdownMenuItem(
        text = {
            Text(
                text = text,
                color = TextWhite
            )
        },
        onClick = onClick
    )
}

/*
 * ============================================================
 * CANCIÓN
 * ============================================================
 */

@Composable
private fun SongItem(
    song: Song,
    isCurrent: Boolean,
    onClick: () -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick
                )
                .padding(
                    vertical = 8.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        AlbumArt(
            song = song,
            size = 56.dp
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
                        ParaguayBlue
                    else
                        TextWhite,

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
                    TextGray,

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
                        TextGrayDark,

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
                            ParaguayBlue
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
private fun AlbumArt(
    song: Song,
    size: Dp
) {

    val albumArtUri =
        "content://media/external/audio/albumart/${song.albumId}"

    Box(
        modifier =
            Modifier
                .size(size)
                .clip(
                    RoundedCornerShape(10.dp)
                )
                .background(
                    AmoledSurface2
                ),

        contentAlignment =
            Alignment.Center
    ) {

        SubcomposeAsyncImage(
            model =
                albumArtUri,

            contentDescription =
                "Carátula",

            modifier =
                Modifier.fillMaxSize(),

            contentScale =
                ContentScale.Crop,

            loading = {

                GenericAlbumArt(
                    size = size
                )
            },

            error = {

                GenericAlbumArt(
                    size = size
                )
            }
        )
    }
}

/*
 * ============================================================
 * CARÁTULA GENÉRICA
 * ============================================================
 */

@Composable
private fun GenericAlbumArt(
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
                    AmoledSurface2
                ),

        contentAlignment =
            Alignment.Center
    ) {

        Icon(
            imageVector =
                Icons.Default.MusicNote,

            contentDescription =
                null,

            tint =
                ParaguayBlue,

            modifier =
                Modifier.size(
                    size * 0.45f
                )
        )
    }
}

/*
 * ============================================================
 * MINI PLAYER
 * ============================================================
 */

@Composable
private fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenPlayer: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick =
                        onOpenPlayer
                )
    ) {

        SectionTitle(
            title = "REPRODUCCIÓN"
        )

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        AmoledSurface,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(10.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            AlbumArt(
                song = song,
                size = 54.dp
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
                        TextWhite,

                    fontWeight =
                        FontWeight.Bold,

                    maxLines =
                        1
                )

                Text(
                    text =
                        song.artist,

                    color =
                        TextGray,

                    fontSize =
                        12.sp,

                    maxLines =
                        1
                )
            }

            IconButton(
                onClick = onPrevious
            ) {

                Icon(
                    imageVector =
                        Icons.Default.SkipPrevious,

                    contentDescription =
                        "Anterior",

                    tint =
                        TextWhite
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

                    tint =
                        ParaguayBlue,

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

                    tint =
                        TextWhite
                )
            }

            Icon(
                imageVector =
                    Icons.Default.ExpandLess,

                contentDescription =
                    "Abrir reproductor",

                tint =
                    TextGray
            )
        }
    }
}

/*
 * ============================================================
 * REPRODUCTOR COMPLETO
 * ============================================================
 */

@Composable
private fun FullPlayerScreen(
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

    MaterialTheme(
        colorScheme =
            AmoledColorScheme
    ) {

        Surface(
            modifier =
                Modifier.fillMaxSize(),

            color =
                AmoledBlack
        ) {

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 22.dp,
                            vertical = 18.dp
                        )
            ) {

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
                                TextWhite
                        )
                    }

                    Text(
                        text =
                            "REPRODUCCIÓN",

                        color =
                            TextWhite,

                        fontSize =
                            16.sp,

                        fontWeight =
                            FontWeight.Bold,

                        modifier =
                            Modifier.weight(1f)
                    )
                }

                ParaguayLine()

                Spacer(
                    modifier =
                        Modifier.height(35.dp)
                )

                AlbumArt(
                    song =
                        song,

                    size =
                        290.dp
                )

                Spacer(
                    modifier =
                        Modifier.height(28.dp)
                )

                Text(
                    text =
                        song.title,

                    color =
                        TextWhite,

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
                        ParaguayBlue,

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
                            TextGray,

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
                                ParaguayBlue,

                            activeTrackColor =
                                ParaguayBlue,

                            inactiveTrackColor =
                                SectionLineGray
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
                            TextGray,

                        fontSize =
                            12.sp
                    )

                    Text(
                        text =
                            formatTime(
                                safeDuration
                            ),

                        color =
                            TextGray,

                        fontSize =
                            12.sp
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(24.dp)
                )

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
                                    ParaguayBlue
                                else
                                    TextGray
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
                                TextWhite,

                            modifier =
                                Modifier.size(38.dp)
                        )
                    }

                    IconButton(
                        onClick =
                            onPlayPause,

                        modifier =
                            Modifier
                                .size(70.dp)
                                .background(
                                    TextWhite,
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
                                AmoledBlack,

                            modifier =
                                Modifier.size(38.dp)
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
                                TextWhite,

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
                                "Repetir",

                            tint =
                                if (
                                    repeatMode !=
                                    Player.REPEAT_MODE_OFF
                                )
                                    ParaguayBlue
                                else
                                    TextGray
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(25.dp)
                )

                TerereBrand(
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        }
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
