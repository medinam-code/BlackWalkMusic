package com.blackwalkmusic

import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

    // ÚNICA cola cacheada.
    // NO existe getCurrentQueue().
    private var currentQueue by mutableStateOf<List<Song>>(emptyList())

    private var isPlaying by mutableStateOf(false)
    private var currentPosition by mutableLongStateOf(0L)
    private var duration by mutableLongStateOf(0L)
    private var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF)
    private var shuffleEnabled by mutableStateOf(false)
    private var favoriteIds by mutableStateOf<Set<Long>>(emptySet())

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
            updateCurrentSong()
            updateCurrentQueue()
            updateProgress()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updateProgress()
        }

        override fun onRepeatModeChanged(mode: Int) {
            repeatMode = mode
        }

        override fun onShuffleModeEnabledChanged(
            shuffleModeEnabled: Boolean
        ) {
            shuffleEnabled = shuffleModeEnabled
            updateCurrentQueue()
        }

        override fun onTimelineChanged(
            timeline: androidx.media3.common.Timeline,
            reason: Int
        ) {
            updateCurrentSong()
            updateCurrentQueue()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        favoriteIds = FavoritesManager.getFavorites(this)

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

            if (showFullPlayer && currentSong != null) {

                FullPlayerScreen(
                    song = currentSong!!,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    repeatMode = repeatMode,
                    shuffleEnabled = shuffleEnabled,
                    isFavorite = favoriteIds.contains(currentSong!!.id),
                    queue = currentQueue,

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
                    },

                    onFavorite = {
                        currentSong?.let {
                            toggleFavorite(it)
                        }
                    },

                    onQueueSongClick = {
                        playSongFromCurrentQueue(it)
                    }
                )

            } else {

                BlackWalkMusicScreen(
                    songs = songs,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    favoriteIds = favoriteIds,

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

        val sessionToken = SessionToken(
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
                controller = controllerFuture.get()

                controller?.addListener(
                    playerListener
                )

                controller?.let {
                    isPlaying = it.isPlaying
                    repeatMode = it.repeatMode
                    shuffleEnabled = it.shuffleModeEnabled
                }

                updateCurrentSong()
                updateCurrentQueue()
                updateProgress()
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun updateCurrentSong() {

        val player = controller ?: return

        val uri = player.currentMediaItem
            ?.localConfiguration
            ?.uri
            ?.toString()

        currentSong = songs.find {
            it.uri == uri
        }

        isPlaying = player.isPlaying
    }

    /*
     * ESTA ES LA ÚNICA FUNCIÓN PARA ACTUALIZAR LA COLA.
     *
     * IMPORTANTE:
     * NO crear getCurrentQueue().
     */
    private fun updateCurrentQueue() {

        val player = controller

        if (player == null || songs.isEmpty()) {
            currentQueue = emptyList()
            return
        }

        val count = player.mediaItemCount

        if (count <= 0) {
            currentQueue = emptyList()
            return
        }

        val songsByUri = songs.associateBy {
            it.uri
        }

        val result = ArrayList<Song>(count)

        for (index in 0 until count) {

            val uri = player
                .getMediaItemAt(index)
                .localConfiguration
                ?.uri
                ?.toString()
                ?: continue

            songsByUri[uri]?.let {
                result.add(it)
            }
        }

        currentQueue = result
    }

    private fun updateProgress() {

        val player = controller ?: return

        currentPosition =
            player.currentPosition.coerceAtLeast(0L)

        duration =
            player.duration
                .takeIf { it > 0L }
                ?: currentSong?.duration
                ?: 0L
    }

    private fun playSong(song: Song) {

        val player = controller ?: return

        if (songs.isEmpty()) return

        val mediaItems = songs.map {
            MediaItem.fromUri(it.uri)
        }

        val index = songs.indexOfFirst {
            it.uri == song.uri
        }.coerceAtLeast(0)

        player.setMediaItems(
            mediaItems,
            index,
            0L
        )

        player.shuffleModeEnabled = false
        player.prepare()
        player.play()

        currentSong = song
        isPlaying = true
        shuffleEnabled = false

        updateCurrentQueue()
    }

    private fun playSongFromCurrentQueue(song: Song) {

        val player = controller ?: return

        for (index in 0 until player.mediaItemCount) {

            val uri = player
                .getMediaItemAt(index)
                .localConfiguration
                ?.uri
                ?.toString()

            if (uri == song.uri) {

                player.seekTo(
                    index,
                    0L
                )

                player.play()

                currentSong = song
                isPlaying = true

                return
            }
        }
    }

    private fun playPause() {

        val player = controller ?: return

        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    private fun nextSong() {

        val player = controller ?: return

        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            player.play()
        }
    }

    private fun previousSong() {

        val player = controller ?: return

        if (player.currentPosition > 3000L) {
            player.seekTo(0L)
            return
        }

        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
            player.play()
        }
    }

    private fun shuffleSongs() {

        val player = controller ?: return

        if (songs.isEmpty()) return

        val shuffled = songs.shuffled()

        player.setMediaItems(
            shuffled.map {
                MediaItem.fromUri(it.uri)
            },
            0,
            0L
        )

        player.shuffleModeEnabled = true
        player.prepare()
        player.play()

        currentSong = shuffled.firstOrNull()
        isPlaying = true
        shuffleEnabled = true

        updateCurrentQueue()
    }

    private fun toggleShuffle() {

        val player = controller ?: return

        player.shuffleModeEnabled =
            !player.shuffleModeEnabled

        shuffleEnabled =
            player.shuffleModeEnabled

        updateCurrentQueue()
    }

    private fun toggleRepeat() {

        val player = controller ?: return

        val newMode =
            when (player.repeatMode) {

                Player.REPEAT_MODE_OFF ->
                    Player.REPEAT_MODE_ALL

                Player.REPEAT_MODE_ALL ->
                    Player.REPEAT_MODE_ONE

                else ->
                    Player.REPEAT_MODE_OFF
            }

        player.repeatMode = newMode
        repeatMode = newMode
    }

    private fun toggleFavorite(song: Song) {

        FavoritesManager.toggleFavorite(
            this,
            song.id
        )

        favoriteIds =
            FavoritesManager.getFavorites(this)
    }

    private fun requestMusicPermission() {

        val permissions =
            if (Build.VERSION.SDK_INT >= 33) {

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
            FavoritesManager.getFavorites(this)

        updateCurrentSong()
        updateCurrentQueue()
    }

    override fun onDestroy() {

        controller?.removeListener(
            playerListener
        )

        if (::controllerFuture.isInitialized) {
            MediaController.releaseFuture(
                controllerFuture
            )
        }

        controller = null

        super.onDestroy()
    }
}

/* ============================================================
   MARCA
   ============================================================ */

@Composable
fun TerereBrand(
    modifier: Modifier = Modifier
) {

    Column(modifier) {

        Text(
            "TERERÉ",
            color = ParaguayRed,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )

        Text(
            "MUSIC",
            color = ParaguayWhite,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 18.dp)
        )

        Text(
            "PY",
            color = ParaguayBlue,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 48.dp)
        )

        Spacer(Modifier.height(5.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
        ) {

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(ParaguayRed)
            )

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(ParaguayWhite)
            )

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(ParaguayBlue)
            )
        }
    }
}

/* ============================================================
   PANTALLA PRINCIPAL
   ============================================================ */

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

    var search by remember {
        mutableStateOf("")
    }

    var sort by remember {
        mutableStateOf(SongSort.TITLE)
    }

    var favoritesOnly by remember {
        mutableStateOf(false)
    }

    var sortMenu by remember {
        mutableStateOf(false)
    }

    val filteredSongs = remember(
        songs,
        search,
        sort,
        favoritesOnly,
        favoriteIds
    ) {

        val query = search.trim()

        val result = songs.filter { song ->

            val matchesSearch =
                query.isEmpty() ||
                    song.title.contains(
                        query,
                        true
                    ) ||
                    song.artist.contains(
                        query,
                        true
                    ) ||
                    song.album.contains(
                        query,
                        true
                    )

            val matchesFavorite =
                !favoritesOnly ||
                    favoriteIds.contains(song.id)

            matchesSearch && matchesFavorite
        }

        when (sort) {

            SongSort.TITLE ->
                result.sortedBy {
                    it.title.lowercase()
                }

            SongSort.ARTIST ->
                result.sortedBy {
                    it.artist.lowercase()
                }

            SongSort.ALBUM ->
                result.sortedBy {
                    it.album.lowercase()
                }

            SongSort.DURATION ->
                result.sortedByDescending {
                    it.duration
                }

            SongSort.NEWEST ->
                result.sortedByDescending {
                    it.dateAdded
                }

            SongSort.OLDEST ->
                result.sortedBy {
                    it.dateAdded
                }
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = AmoledBlack,
            surface = AmoledSurface,
            primary = ParaguayWhite,
            onPrimary = Color.Black
        )
    ) {

        Surface(
            Modifier.fillMaxSize(),
            color = AmoledBlack
        ) {

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    TerereBrand(
                        Modifier.weight(1f)
                    )

                    IconButton(onClick = onShuffle) {
                        Icon(
                            Icons.Default.Shuffle,
                            "Aleatorio",
                            tint = ParaguayWhite
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = search,
                    onValueChange = {
                        search = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            null,
                            tint = ParaguayWhite
                        )
                    },
                    trailingIcon = {
                        if (search.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    search = ""
                                }
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    "Limpiar",
                                    tint = TextGray
                                )
                            }
                        }
                    },
                    placeholder = {
                        Text(
                            "Buscar canción, artista o álbum",
                            color = TextGray
                        )
                    },
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = ParaguayWhite,
                            unfocusedBorderColor = BorderGray,
                            cursorColor = ParaguayWhite
                        ),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        Modifier.weight(1f)
                    ) {

                        Text(
                            if (favoritesOnly)
                                "FAVORITOS"
                            else
                                "BIBLIOTECA",
                            color = TextWhite,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "${filteredSongs.size} canciones",
                            color = TextGray,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            favoritesOnly =
                                !favoritesOnly
                        }
                    ) {

                        Icon(
                            if (favoritesOnly)
                                Icons.Default.Favorite
                            else
                                Icons.Default.FavoriteBorder,
                            "Favoritos",
                            tint =
                                if (favoritesOnly)
                                    ParaguayRed
                                else
                                    TextGray
                        )
                    }

                    Box {

                        IconButton(
                            onClick = {
                                sortMenu = true
                            }
                        ) {
                            Icon(
                                Icons.Default.Sort,
                                "Ordenar",
                                tint = TextWhite
                            )
                        }

                        DropdownMenu(
                            expanded = sortMenu,
                            onDismissRequest = {
                                sortMenu = false
                            },
                            containerColor = AmoledSurface
                        ) {

                            SortItem(
                                "Más recientes",
                                SongSort.NEWEST
                            ) {
                                sort = it
                                sortMenu = false
                            }

                            SortItem(
                                "Más antiguas",
                                SongSort.OLDEST
                            ) {
                                sort = it
                                sortMenu = false
                            }

                            SortItem(
                                "Título",
                                SongSort.TITLE
                            ) {
                                sort = it
                                sortMenu = false
                            }

                            SortItem(
                                "Artista",
                                SongSort.ARTIST
                            ) {
                                sort = it
                                sortMenu = false
                            }

                            SortItem(
                                "Álbum",
                                SongSort.ALBUM
                            ) {
                                sort = it
                                sortMenu = false
                            }

                            SortItem(
                                "Duración",
                                SongSort.DURATION
                            ) {
                                sort = it
                                sortMenu = false
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                if (filteredSongs.isEmpty()) {

                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {

                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            GenericAlbumArt(80.dp)

                            Spacer(Modifier.height(14.dp))

                            Text(
                                if (favoritesOnly)
                                    "NO HAY FAVORITOS"
                                else
                                    "NO SE ENCONTRARON CANCIONES",
                                color = TextGray,
                                fontSize = 12.sp
                            )
                        }
                    }

                } else {

                    LazyColumn(
                        Modifier.weight(1f),
                        contentPadding =
                            PaddingValues(bottom = 8.dp)
                    ) {

                        items(
                            filteredSongs,
                            key = { it.id }
                        ) { song ->

                            SongItem(
                                song = song,
                                isCurrent =
                                    currentSong?.id == song.id,
                                isFavorite =
                                    favoriteIds.contains(song.id),
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

                currentSong?.let { song ->

                    MiniPlayer(
                        song = song,
                        isPlaying = isPlaying,
                        isFavorite =
                            favoriteIds.contains(song.id),
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onOpenPlayer = onOpenPlayer,
                        onFavorite = {
                            onFavorite(song)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SortItem(
    text: String,
    mode: SongSort,
    onSelected: (SongSort) -> Unit
) {

    DropdownMenuItem(
        text = {
            Text(
                text,
                color = TextWhite
            )
        },
        onClick = {
            onSelected(mode)
        }
    )
}

/* ============================================================
   SONG ITEM
   ============================================================ */

@Composable
fun SongItem(
    song: Song,
    isCurrent: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit
) {

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        AlbumArt(
            song = song,
            size = 56.dp
        )

        Spacer(Modifier.width(13.dp))

        Column(
            Modifier.weight(1f)
        ) {

            Text(
                song.title,
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight =
                    if (isCurrent)
                        FontWeight.Bold
                    else
                        FontWeight.Normal,
                maxLines = 2
            )

            Text(
                song.artist,
                color = TextGray,
                fontSize = 13.sp,
                maxLines = 1
            )

            if (song.album.isNotBlank()) {

                Text(
                    song.album,
                    color = TextGrayDark,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }

        IconButton(onClick = onFavorite) {

            Icon(
                if (isFavorite)
                    Icons.Default.Favorite
                else
                    Icons.Default.FavoriteBorder,
                "Favorito",
                tint =
                    if (isFavorite)
                        ParaguayRed
                    else
                        TextGray
            )
        }

        if (isCurrent) {

            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(ParaguayWhite)
            )
        }
    }
}

/* ============================================================
   ALBUM ART
   ============================================================ */

@Composable
fun AlbumArt(
    song: Song,
    size: Dp,
    modifier: Modifier = Modifier
) {

    val uri =
        "content://media/external/audio/albumart/${song.albumId}"

    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(AmoledSurface2)
            .border(
                1.dp,
                BorderGray,
                RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {

        SubcomposeAsyncImage(
            model = uri,
            contentDescription =
                "Carátula de ${song.album}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            loading = {
                GenericAlbumArt(size)
            },
            error = {
                GenericAlbumArt(size)
            }
        )
    }
}

@Composable
fun GenericAlbumArt(
    size: Dp
) {

    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(AmoledSurface)
            .border(
                1.dp,
                BorderGray,
                RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {

        Icon(
            Icons.Default.MusicNote,
            null,
            tint = ParaguayWhite,
            modifier = Modifier.size(size * .42f)
        )
    }
}

/* ============================================================
   MINI PLAYER
   ============================================================ */

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
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenPlayer),
        color = AmoledSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderGray)
    ) {

        Column(
            Modifier.padding(9.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                AlbumArt(
                    song = song,
                    size = 52.dp
                )

                Spacer(Modifier.width(11.dp))

                Column(
                    Modifier.weight(1f)
                ) {

                    Text(
                        song.title,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Text(
                        song.artist,
                        color = TextGray,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = onFavorite
                ) {

                    Icon(
                        if (isFavorite)
                            Icons.Default.Favorite
                        else
                            Icons.Default.FavoriteBorder,
                        "Favorito",
                        tint =
                            if (isFavorite)
                                ParaguayRed
                            else
                                TextGray
                    )
                }

                Icon(
                    Icons.Default.PlayArrow,
                    "Abrir reproductor",
                    tint = TextWhite
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.Center,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                IconButton(onClick = onPrevious) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        "Anterior",
                        tint = TextWhite
                    )
                }

                IconButton(onClick = onPlayPause) {
                    Icon(
                        if (isPlaying)
                            Icons.Default.Pause
                        else
                            Icons.Default.PlayArrow,
                        if (isPlaying)
                            "Pausar"
                        else
                            "Reproducir",
                        tint = ParaguayWhite,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = onNext) {
                    Icon(
                        Icons.Default.SkipNext,
                        "Siguiente",
                        tint = TextWhite
                    )
                }
            }
        }
    }
}

/* ============================================================
   FULL PLAYER
   ============================================================ */

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

    var showQueue by remember {
        mutableStateOf(false)
    }

    if (showQueue) {

        QueueScreen(
            currentSong = song,
            queue = queue,
            onBack = {
                showQueue = false
            },
            onSongClick = onQueueSongClick
        )

        return
    }

    val safeDuration =
        duration.coerceAtLeast(1L)

    val safePosition =
        currentPosition.coerceIn(
            0L,
            safeDuration
        )

    Column(
        Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(
                horizontal = 22.dp,
                vertical = 16.dp
            )
    ) {

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    "Volver",
                    tint = TextWhite
                )
            }

            Text(
                "REPRODUCIENDO",
                color = TextGray,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onFavorite) {
                Icon(
                    if (isFavorite)
                        Icons.Default.Favorite
                    else
                        Icons.Default.FavoriteBorder,
                    "Favorito",
                    tint =
                        if (isFavorite)
                            ParaguayRed
                        else
                            TextWhite
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        AlbumArt(
            song = song,
            size = 300.dp,
            modifier =
                Modifier.align(
                    Alignment.CenterHorizontally
                )
        )

        Spacer(Modifier.height(25.dp))

        Text(
            song.title,
            color = TextWhite,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2
        )

        Spacer(Modifier.height(5.dp))

        Text(
            song.artist,
            color = TextGray,
            fontSize = 16.sp,
            maxLines = 1
        )

        if (song.album.isNotBlank()) {

            Text(
                song.album,
                color = TextGrayDark,
                fontSize = 13.sp,
                maxLines = 1
            )
        }

        Spacer(Modifier.height(18.dp))

        Slider(
            value = safePosition.toFloat(),
            onValueChange = {
                onSeek(it.toLong())
            },
            valueRange =
                0f..safeDuration.toFloat(),
            modifier = Modifier.fillMaxWidth(),
            colors =
                SliderDefaults.colors(
                    thumbColor = ParaguayWhite,
                    activeTrackColor = ParaguayWhite,
                    inactiveTrackColor = BorderGray
                )
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(
                formatTime(safePosition),
                color = TextGray,
                fontSize = 11.sp
            )

            Text(
                formatTime(safeDuration),
                color = TextGray,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(15.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceEvenly,
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(onClick = onShuffle) {
                Icon(
                    Icons.Default.Shuffle,
                    "Aleatorio",
                    tint =
                        if (shuffleEnabled)
                            TextWhite
                        else
                            TextGray
                )
            }

            IconButton(onClick = onPrevious) {
                Icon(
                    Icons.Default.SkipPrevious,
                    "Anterior",
                    tint = TextWhite,
                    modifier = Modifier.size(38.dp)
                )
            }

            IconButton(
                onClick = onPlayPause,
                modifier =
                    Modifier
                        .size(70.dp)
                        .background(
                            TextWhite,
                            CircleShape
                        )
            ) {

                Icon(
                    if (isPlaying)
                        Icons.Default.Pause
                    else
                        Icons.Default.PlayArrow,
                    "Reproducir",
                    tint = Color.Black,
                    modifier = Modifier.size(38.dp)
                )
            }

            IconButton(onClick = onNext) {
                Icon(
                    Icons.Default.SkipNext,
                    "Siguiente",
                    tint = TextWhite,
                    modifier = Modifier.size(38.dp)
                )
            }

            IconButton(onClick = onRepeat) {
                Icon(
                    if (
                        repeatMode ==
                        Player.REPEAT_MODE_ONE
                    )
                        Icons.Default.RepeatOne
                    else
                        Icons.Default.Repeat,
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

        Spacer(Modifier.weight(1f))

        Surface(
            Modifier
                .fillMaxWidth()
                .clickable {
                    showQueue = true
                },
            color = AmoledSurface,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, BorderGray)
        ) {

            Row(
                Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    Icons.Default.MusicNote,
                    null,
                    tint = ParaguayWhite
                )

                Spacer(Modifier.width(10.dp))

                Column(
                    Modifier.weight(1f)
                ) {

                    Text(
                        "REPRODUCCIÓN ACTUAL",
                        color = TextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "${queue.size} canciones en cola",
                        color = TextGray,
                        fontSize = 10.sp
                    )
                }

                Text(
                    "ABRIR",
                    color = TextWhite,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        TerereBrand(
            Modifier.fillMaxWidth()
        )
    }
}

/* ============================================================
   QUEUE
   ============================================================ */

@Composable
private fun QueueScreen(
    currentSong: Song,
    queue: List<Song>,
    onBack: () -> Unit,
    onSongClick: (Song) -> Unit
) {

    Column(
        Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(
                horizontal = 18.dp,
                vertical = 16.dp
            )
    ) {

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    "Volver",
                    tint = TextWhite
                )
            }

            Column(
                Modifier.weight(1f)
            ) {

                Text(
                    "REPRODUCCIÓN ACTUAL",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "${queue.size} canciones",
                    color = TextGray,
                    fontSize = 12.sp
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {

            Box(
                Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(ParaguayRed)
            )

            Box(
                Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(ParaguayWhite)
            )

            Box(
                Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(ParaguayBlue)
            )
        }

        Spacer(Modifier.height(12.dp))

        if (queue.isEmpty()) {

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    "NO HAY REPRODUCCIÓN ACTUAL",
                    color = TextGray,
                    fontSize = 12.sp
                )
            }

        } else {

            LazyColumn(
                Modifier.weight(1f),
                contentPadding =
                    PaddingValues(bottom = 20.dp)
            ) {

                items(
                    queue,
                    key = { it.id }
                ) { queueSong ->

                    val isCurrent =
                        queueSong.id == currentSong.id

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSongClick(queueSong)
                            }
                            .padding(vertical = 9.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        AlbumArt(
                            song = queueSong,
                            size = 55.dp
                        )

                        Spacer(Modifier.width(12.dp))

                        Column(
                            Modifier.weight(1f)
                        ) {

                            Text(
                                queueSong.title,
                                color =
                                    if (isCurrent)
                                        TextWhite
                                    else
                                        TextGray,
                                fontSize = 15.sp,
                                fontWeight =
                                    if (isCurrent)
                                        FontWeight.Bold
                                    else
                                        FontWeight.Normal,
                                maxLines = 2
                            )

                            Text(
                                queueSong.artist,
                                color = TextGrayDark,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }

                        if (isCurrent) {

                            Icon(
                                Icons.Default.PlayArrow,
                                "Reproduciendo",
                                tint = ParaguayWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        TerereBrand(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 35.dp)
        )
    }
}

/* ============================================================
   TIME
   ============================================================ */

private fun formatTime(
    milliseconds: Long
): String {

    val seconds =
        (milliseconds / 1000L)
            .coerceAtLeast(0L)

    val minutes = seconds / 60L
    val remaining = seconds % 60L

    return "%d:%02d".format(
        minutes,
        remaining
    )
}
