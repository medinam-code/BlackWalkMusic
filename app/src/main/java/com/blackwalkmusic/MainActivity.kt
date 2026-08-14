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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
 * SONG
 * ============================================================
 *
 * Esta clase debe existir una sola vez en el proyecto.
 *
 * Si ya tienes otra clase Song en otro archivo, elimina esta
 * definición y conserva solamente la que ya tengas.
 *
 * ============================================================
 */



/*
 * ============================================================
 * PALETA AMOLED
 * ============================================================
 */

private val AmoledBlack = Color(0xFF000000)
private val AmoledSurface = Color(0xFF080808)
private val AmoledSurface2 = Color(0xFF101010)

private val TextWhite = Color(0xFFF2F2F2)
private val TextGray = Color(0xFF8C8C8C)
private val TextGrayDark = Color(0xFF555555)

private val ParaguayRed = Color(0xFFD90012)
private val ParaguayWhite = Color(0xFFF5F5F5)
private val ParaguayBlue = Color(0xFF0038A8)

private val BorderGray = Color(0xFF222222)

/*
 * ============================================================
 * ORDEN
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

    private lateinit var controllerFuture:
        ListenableFuture<MediaController>

    private var controller:
        MediaController? = null

    private var songs by mutableStateOf<List<Song>>(
        emptyList()
    )

    private var currentSong by mutableStateOf<Song?>(
        null
    )

    /*
     * ========================================================
     * COLA CACHEADA
     *
     * IMPORTANTE:
     *
     * Antes se ejecutaba getCurrentQueue() directamente
     * dentro de setContent.
     *
     * Eso podía recalcular toda la cola durante las
     * recomposiciones y afectar el scroll.
     *
     * Ahora la cola se mantiene en memoria y solamente se
     * actualiza cuando realmente cambia.
     * ========================================================
     */

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

    /*
     * ========================================================
     * PERMISOS
     * ========================================================
     */

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            loadMusic()
        }

    /*
     * ========================================================
     * PLAYER LISTENER
     * ========================================================
     */

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

                updateCurrentQueue()

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
                shuffleEnabled =
                    shuffleModeEnabled
            }

            override fun onTimelineChanged(
                timeline:
                    androidx.media3.common.Timeline,
                reason: Int
            ) {

                updateCurrentSong()

                updateCurrentQueue()
            }
        }

    /*
     * ============================================================
     * ON CREATE
     * ============================================================
     */

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        favoriteIds =
            FavoritesManager.getFavorites(this)

        setContent {

            var showFullPlayer by remember {
                mutableStateOf(false)
            }

            /*
             * Actualización del progreso.
             *
             * 500 ms mantiene el contador fluido sin utilizar
             * una frecuencia excesiva.
             */

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

                        controller?.seekTo(
                            position
                        )

                        currentPosition =
                            position
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

                    onQueueSongClick = { queueSong ->

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

                            showFullPlayer = true
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

    /*
     * ============================================================
     * MEDIA SESSION
     * ============================================================
     */

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

                updateCurrentQueue()

                updateProgress()

            },
            ContextCompat.getMainExecutor(this)
        )
    }

    /*
     * ============================================================
     * CURRENT SONG
     * ============================================================
     */

    private fun updateCurrentSong() {

        val mediaController =
            controller ?: return

        val mediaItem =
            mediaController.currentMediaItem

        val uri =
            mediaItem
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

    /*
     * ============================================================
     * UPDATE QUEUE
     *
     * Esta función se llama únicamente cuando cambia realmente
     * el contenido/orden de la cola.
     *
     * NO se ejecuta durante cada frame del scroll.
     * ============================================================
     */

    private fun updateCurrentQueue() {

        val mediaController =
            controller

        if (
            mediaController == null ||
            songs.isEmpty()
        ) {

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

        /*
         * Índice por URI.
         *
         * Evita hacer songs.find{} repetidamente.
         */

        val songsByUri =
            songs.associateBy {
                it.uri
            }

        val result =
            ArrayList<Song>(count)

        for (index in 0 until count) {

            val uri =
                mediaController
                    .getMediaItemAt(index)
                    .localConfiguration
                    ?.uri
                    ?.toString()
                    ?: continue

            val song =
                songsByUri[uri]

            if (song != null) {
                result.add(song)
            }
        }

        currentQueue =
            result
    }

    /*
     * ============================================================
     * PROGRESS
     * ============================================================
     */

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

    /*
     * ============================================================
     * PLAY SONG
     * ============================================================
     */

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

        mediaController.shuffleModeEnabled =
            false

        mediaController.prepare()

        mediaController.play()

        currentSong =
            song

        isPlaying =
            true

        shuffleEnabled =
            false

        /*
         * Actualizamos la cola una sola vez.
         */

        updateCurrentQueue()
    }

    /*
     * ============================================================
     * PLAY SONG FROM CURRENT QUEUE
     * ============================================================
     */

    private fun playSongFromCurrentQueue(
        song: Song
    ) {

        val mediaController =
            controller ?: return

        val count =
            mediaController.mediaItemCount

        for (index in 0 until count) {

            val uri =
                mediaController
                    .getMediaItemAt(index)
                    .localConfiguration
                    ?.uri
                    ?.toString()

            if (uri == song.uri) {

                mediaController.seekTo(
                    index,
                    0L
                )

                mediaController.play()

                currentSong =
                    song

                isPlaying =
                    true

                return
            }
        }
    }

    /*
     * ============================================================
     * PLAY / PAUSE
     * ============================================================
     */

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

    /*
     * ============================================================
     * NEXT
     * ============================================================
     */

    private fun nextSong() {

        val mediaController =
            controller ?: return

        if (
            mediaController.hasNextMediaItem()
        ) {

            mediaController.seekToNextMediaItem()

            mediaController.play()
        }
    }

    /*
     * ============================================================
     * PREVIOUS
     * ============================================================
     */

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

    /*
     * ============================================================
     * SHUFFLE
     * ============================================================
     */

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

        isPlaying =
            true

        shuffleEnabled =
            true

        updateCurrentQueue()
    }

    /*
     * ============================================================
     * TOGGLE SHUFFLE
     * ============================================================
     */

    private fun toggleShuffle() {

        val mediaController =
            controller ?: return

        mediaController.shuffleModeEnabled =
            !mediaController.shuffleModeEnabled

        shuffleEnabled =
            mediaController.shuffleModeEnabled

        updateCurrentQueue()
    }

    /*
     * ============================================================
     * REPEAT
     * ============================================================
     */

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

    /*
     * ============================================================
     * FAVORITES
     * ============================================================
     */

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

    /*
     * ============================================================
     * LEGACY QUEUE FUNCTION
     *
     * Se conserva por compatibilidad, pero la interfaz ya no
     * la llama durante las recomposiciones.
     * ============================================================
     */

    private fun getCurrentQueue(): List<Song> {

        val mediaController =
            controller ?: return emptyList()

        val count =
            mediaController.mediaItemCount

        if (
            count <= 0 ||
            songs.isEmpty()
        ) {

            return emptyList()
        }

        val songsByUri =
            songs.associateBy {
                it.uri
            }

        val result =
            ArrayList<Song>(count)

        for (index in 0 until count) {

            val uri =
                mediaController
                    .getMediaItemAt(index)
                    .localConfiguration
                    ?.uri
                    ?.toString()
                    ?: continue

            songsByUri[uri]?.let {
                result.add(it)
            }
        }

        return result
    }

    /*
     * ============================================================
     * PERMISSIONS
     * ============================================================
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

        permissionLauncher.launch(
            permissions
        )
    }

    /*
     * ============================================================
     * LOAD MUSIC
     * ============================================================
     */

    private fun loadMusic() {

        songs =
            MusicRepository.getSongs(
                contentResolver
            )

        favoriteIds =
            FavoritesManager.getFavorites(
                this
            )

        updateCurrentSong()

        updateCurrentQueue()
    }

    /*
     * ============================================================
     * DESTROY
     * ============================================================
     */

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
 * ============================================================
 */

@Composable
fun TerereBrand(
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
    ) {

        Text(
            text = "TERERÉ",
            color = ParaguayRed,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )

        Text(
            text = "MUSIC",
            color = ParaguayWhite,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            modifier =
                Modifier.padding(
                    start = 18.dp
                )
        )

        Text(
            text = "PY",
            color = ParaguayBlue,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            modifier =
                Modifier.padding(
                    start = 48.dp
                )
        )

        Spacer(
            modifier =
                Modifier.height(5.dp)
        )

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
    favoriteIds: Set<Long>,
    onSongClick: (Song) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onOpenPlayer: () -> Unit,
    onFavorite: (Song) -> Unit
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

    var showFavoritesOnly by remember {
        mutableStateOf(false)
    }

    val filteredSongs =
        remember(
            songs,
            searchText,
            sortMode,
            showFavoritesOnly,
            favoriteIds
        ) {

            val query =
                searchText.trim()

            val filtered =
                songs.filter { song ->

                    val matchesSearch =
                        query.isEmpty() ||
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

                    val matchesFavorite =
                        !showFavoritesOnly ||
                            favoriteIds.contains(
                                song.id
                            )

                    matchesSearch &&
                        matchesFavorite
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
                    AmoledBlack,

                surface =
                    AmoledSurface,

                primary =
                    ParaguayWhite,

                onPrimary =
                    Color.Black
            )
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
                                ParaguayWhite
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

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
                                ParaguayWhite
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
                                TextGray
                        )
                    },

                    colors =
                        OutlinedTextFieldDefaults.colors(

                            focusedTextColor =
                                TextWhite,

                            unfocusedTextColor =
                                TextWhite,

                            focusedBorderColor =
                                ParaguayWhite,

                            unfocusedBorderColor =
                                BorderGray,

                            cursorColor =
                                ParaguayWhite
                        ),

                    shape =
                        RoundedCornerShape(14.dp)
                )

                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )

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
                                if (
                                    showFavoritesOnly
                                )
                                    "FAVORITOS"
                                else
                                    "BIBLIOTECA",

                            color =
                                TextWhite,

                            fontSize =
                                22.sp,

                            fontWeight =
                                FontWeight.Bold,

                            letterSpacing =
                                1.sp
                        )

                        Text(
                            text =
                                "${filteredSongs.size} canciones",

                            color =
                                TextGray,

                            fontSize =
                                12.sp
                        )
                    }

                    IconButton(
                        onClick = {

                            showFavoritesOnly =
                                !showFavoritesOnly
                        }
                    ) {

                        Icon(
                            imageVector =
                                if (
                                    showFavoritesOnly
                                )
                                    Icons.Default.Favorite
                                else
                                    Icons.Default.FavoriteBorder,

                            contentDescription =
                                "Favoritos",

                            tint =
                                if (
                                    showFavoritesOnly
                                )
                                    ParaguayRed
                                else
                                    TextGray
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

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Más recientes",
                                        color =
                                            TextWhite
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
                                            TextWhite
                                    )
                                },

                                onClick = {

                                    sortMode =
                                        SongSort.OLDEST

                                    showSortMenu =
                                        false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Título",
                                        color =
                                            TextWhite
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
                                            TextWhite
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
                                            TextWhite
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
                                            TextWhite
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
                        Modifier.height(6.dp)
                )

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
                                        showFavoritesOnly
                                    )
                                        "NO HAY FAVORITOS"
                                    else
                                        "NO SE ENCONTRARON CANCIONES",

                                color =
                                    TextGray,

                                fontSize =
                                    12.sp
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

                                isFavorite =
                                    favoriteIds.contains(
                                        song.id
                                    ),

                                onClick = {
                                    onSongClick(song)
                                },

                                onFavorite = {
                                    onFavorite(song)
                                }
                            )
                        }
                    }
                }

                if (
                    currentSong != null
                ) {

                    MiniPlayer(
                        song =
                            currentSong,

                        isPlaying =
                            isPlaying,

                        isFavorite =
                            favoriteIds.contains(
                                currentSong.id
                            ),

                        onPlayPause =
                            onPlayPause,

                        onNext =
                            onNext,

                        onPrevious =
                            onPrevious,

                        onOpenPlayer =
                            onOpenPlayer,

                        onFavorite = {
                            onFavorite(currentSong)
                        }
                    )
                }
            }
        }
    }
}

/*
 * ============================================================
 * SONG ITEM
 * ============================================================
 */

@Composable
fun SongItem(
    song: Song,
    isCurrent: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                }
                .padding(
                    vertical = 8.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        AlbumArt(
            song =
                song,

            size =
                56.dp
        )

        Spacer(
            modifier =
                Modifier.width(13.dp)
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
                        ParaguayWhite
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

        IconButton(
            onClick =
                onFavorite
        ) {

            Icon(
                imageVector =
                    if (isFavorite)
                        Icons.Default.Favorite
                    else
                        Icons.Default.FavoriteBorder,

                contentDescription =
                    "Favorito",

                tint =
                    if (isFavorite)
                        ParaguayRed
                    else
                        TextGray
            )
        }

        if (
            isCurrent
        ) {

            Box(
                modifier =
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            ParaguayWhite
                        )
            )
        }
    }
}

/*
 * ============================================================
 * ALBUM ART
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
                    AmoledSurface2
                )
                .border(
                    1.dp,
                    BorderGray,
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
 * GENERIC ALBUM ART
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
                    AmoledSurface
                )
                .border(
                    1.dp,
                    BorderGray,
                    RoundedCornerShape(10.dp)
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
                ParaguayWhite,

            modifier =
                Modifier.size(
                    size * 0.42f
                )
        )
    }
}

/*
 * ============================================================
 * MINI PLAYER
 *
 * IMPORTANTE:
 *
 * Toda la tarjeta abre el reproductor únicamente al tocarla.
 *
 * No existe ningún gesto de arrastre.
 * ============================================================
 */

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenPlayer: () -> Unit,
    onFavorite: () -> Unit
) {

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onOpenPlayer()
                },

        color =
            AmoledSurface,

        shape =
            RoundedCornerShape(16.dp),

        border =
            androidx.compose.foundation.BorderStroke(
                1.dp,
                BorderGray
            )
    ) {

        Column(
            modifier =
                Modifier.padding(9.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                AlbumArt(
                    song =
                        song,

                    size =
                        52.dp
                )

                Spacer(
                    modifier =
                        Modifier.width(11.dp)
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
                    onClick =
                        onFavorite
                ) {

                    Icon(
                        imageVector =
                            if (isFavorite)
                                Icons.Default.Favorite
                            else
                                Icons.Default.FavoriteBorder,

                        contentDescription =
                            "Favorito",

                        tint =
                            if (isFavorite)
                                ParaguayRed
                            else
                                TextGray
                    )
                }

                Icon(
                    imageVector =
                        Icons.Default.PlayArrow,

                    contentDescription =
                        "Abrir reproductor",

                    tint =
                        TextWhite
                )
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
                    onClick =
                        onPrevious
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
                            ParaguayWhite,

                        modifier =
                            Modifier.size(32.dp)
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
                            TextWhite
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
 *
 * NO HAY SWIPE.
 * NO HAY DRAG.
 * NO HAY pointerInput.
 *
 * La reproducción actual solamente se abre tocando el botón.
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
    isFavorite: Boolean,
    queue: List<Song>,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onFavorite: () -> Unit,
    onQueueSongClick: (Song) -> Unit
) {

    /*
     * Estado local únicamente para cambiar entre:
     *
     * 1. Reproductor
     * 2. Reproducción actual
     *
     * NO se modifica mediante gestos.
     */

    var showQueue by remember {
        mutableStateOf(false)
    }

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
                    AmoledBlack
                )
    ) {

        if (!showQueue) {

            /*
             * =================================================
             * REPRODUCTOR
             * =================================================
             */

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 22.dp,
                            vertical = 16.dp
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
                            "REPRODUCIENDO",

                        color =
                            TextGray,

                        fontSize =
                            11.sp,

                        letterSpacing =
                            2.sp,

                        modifier =
                            Modifier.weight(1f)
                    )

                    IconButton(
                        onClick =
                            onFavorite
                    ) {

                        Icon(
                            imageVector =
                                if (isFavorite)
                                    Icons.Default.Favorite
                                else
                                    Icons.Default.FavoriteBorder,

                            contentDescription =
                                "Favorito",

                            tint =
                                if (isFavorite)
                                    ParaguayRed
                                else
                                    TextWhite
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                AlbumArt(
                    song =
                        song,

                    size =
                        300.dp,

                    modifier =
                        Modifier.align(
                            Alignment.CenterHorizontally
                        )
                )

                Spacer(
                    modifier =
                        Modifier.height(25.dp)
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
                        TextGray,

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
                            TextGrayDark,

                        fontSize =
                            13.sp,

                        maxLines =
                            1
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(18.dp)
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
                                ParaguayWhite,

                            activeTrackColor =
                                ParaguayWhite,

                            inactiveTrackColor =
                                BorderGray
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
                            11.sp
                    )

                    Text(
                        text =
                            formatTime(
                                safeDuration
                            ),

                        color =
                            TextGray,

                        fontSize =
                            11.sp
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(15.dp)
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
                                    TextWhite
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
                                Color.Black,

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
                                "Repetición",

                            tint =
                                if (
                                    repeatMode !=
                                    Player.REPEAT_MODE_OFF
                                )
                                    TextWhite
                                else
                                    TextGray
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.weight(1f)
                )

                /*
                 * =================================================
                 * BOTÓN REPRODUCCIÓN ACTUAL
                 *
                 * SOLO CLICK.
                 *
                 * No se puede abrir deslizando.
                 * =================================================
                 */

                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {

                                showQueue =
                                    true
                            },

                    color =
                        AmoledSurface,

                    shape =
                        RoundedCornerShape(14.dp),

                    border =
                        androidx.compose.foundation.BorderStroke(
                            1.dp,
                            BorderGray
                        )
                ) {

                    Row(
                        modifier =
                            Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.MusicNote,

                            contentDescription =
                                null,

                            tint =
                                ParaguayWhite
                        )

                        Spacer(
                            modifier =
                                Modifier.width(10.dp)
                        )

                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Text(
                                text =
                                    "REPRODUCCIÓN ACTUAL",

                                color =
                                    TextWhite,

                                fontSize =
                                    12.sp,

                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                text =
                                    "${queue.size} canciones en cola",

                                color =
                                    TextGray,

                                fontSize =
                                    10.sp
                            )
                        }

                        Text(
                            text =
                                "ABRIR",

                            color =
                                TextWhite,

                            fontSize =
                                10.sp,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                TerereBrand(
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }

        } else {

            /*
             * =================================================
             * REPRODUCCIÓN ACTUAL
             * =================================================
             */

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 18.dp,
                            vertical = 16.dp
                        )
            ) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    IconButton(
                        onClick = {

                            showQueue =
                                false
                        }
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

                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text =
                                "REPRODUCCIÓN ACTUAL",

                            color =
                                TextWhite,

                            fontSize =
                                20.sp,

                            fontWeight =
                                FontWeight.Bold,

                            letterSpacing =
                                1.sp
                        )

                        Text(
                            text =
                                "${queue.size} canciones",

                            color =
                                TextGray,

                            fontSize =
                                12.sp
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(10.dp)
                )

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 8.dp
                            )
                ) {

                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(2.dp)
                                .background(
                                    ParaguayRed
                                )
                    )

                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(2.dp)
                                .background(
                                    ParaguayWhite
                                )
                    )

                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(2.dp)
                                .background(
                                    ParaguayBlue
                                )
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                if (
                    queue.isEmpty()
                ) {

                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth(),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text =
                                "NO HAY REPRODUCCIÓN ACTUAL",

                            color =
                                TextGray,

                            fontSize =
                                12.sp
                        )
                    }

                } else {

                    /*
                     * =================================================
                     * LISTA DE REPRODUCCIÓN
                     *
                     * LazyColumn normal.
                     *
                     * NO pointerInput.
                     * NO detectVerticalDragGestures.
                     * NO draggable.
                     * NO swipe.
                     *
                     * El usuario puede desplazarse normalmente.
                     * =================================================
                     */

                    LazyColumn(
                        modifier =
                            Modifier.weight(1f),

                        contentPadding =
                            PaddingValues(
                                bottom = 20.dp
                            ),

                        verticalArrangement =
                            Arrangement.spacedBy(
                                2.dp
                            )
                    ) {

                        items(
                            items =
                                queue,

                            key = {
                                it.id
                            }
                        ) { queueSong ->

                            val isCurrent =
                                queueSong.id ==
                                    song.id

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {

                                            onQueueSongClick(
                                                queueSong
                                            )
                                        }
                                        .padding(
                                            vertical = 9.dp
                                        ),

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                AlbumArt(
                                    song =
                                        queueSong,

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
                                            queueSong.title,

                                        color =
                                            if (isCurrent)
                                                TextWhite
                                            else
                                                TextGray,

                                        fontSize =
                                            15.sp,

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
                                            queueSong.artist,

                                        color =
                                            TextGrayDark,

                                        fontSize =
                                            12.sp,

                                        maxLines =
                                            1
                                    )
                                }

                                if (
                                    isCurrent
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Default.PlayArrow,

                                        contentDescription =
                                            "Reproduciendo",

                                        tint =
                                            ParaguayWhite,

                                        modifier =
                                            Modifier
                                                .size(22.dp)
                                                .padding(
                                                    end = 2.dp
                                                )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

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
    }
}

/*
 * ============================================================
 * TIME FORMAT
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
