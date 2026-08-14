package com.blackwalkmusic

import android.content.ComponentName
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

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
                    nextSong()
                },

                onPrevious = {
                    previousSong()
                },

                onShuffle = {
                    shuffleSongs()
                }
            )
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

                updateCurrentSong()

            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun updateCurrentSong() {

        val mediaController =
            controller ?: return

        val mediaItem =
            mediaController.currentMediaItem

        val currentUri =
            mediaItem
                ?.localConfiguration
                ?.uri
                ?.toString()

        currentSong =
            songs.find {
                it.uri == currentUri
            }

        isPlaying =
            mediaController.isPlaying
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

        currentSong = song
        isPlaying = true
    }

    private fun playPause() {

        val mediaController =
            controller ?: return

        if (mediaController.isPlaying) {

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
        }
    }

    private fun previousSong() {

        val mediaController =
            controller ?: return

        if (
            mediaController.currentPosition > 3000L
        ) {

            mediaController.seekTo(0L)
            return
        }

        if (
            mediaController.hasPreviousMediaItem()
        ) {

            mediaController.seekToPreviousMediaItem()
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

        mediaController.prepare()
        mediaController.play()

        currentSong =
            shuffledSongs.firstOrNull()

        isPlaying = true
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

private enum class SongSort {

    TITLE,
    ARTIST,
    ALBUM,
    DURATION,
    NEWEST,
    OLDEST
}

@Composable
fun AlbumArtwork(
    song: Song,
    modifier: Modifier = Modifier
) {

    val context =
        LocalContext.current

    var artworkBytes by remember(
        song.uri
    ) {
        mutableStateOf<ByteArray?>(null)
    }

    LaunchedEffect(
        song.uri
    ) {

        artworkBytes =
            withContext(
                Dispatchers.IO
            ) {

                try {

                    val retriever =
                        MediaMetadataRetriever()

                    retriever.setDataSource(
                        context,
                        Uri.parse(song.uri)
                    )

                    val picture =
                        retriever.embeddedPicture

                    retriever.release()

                    picture

                } catch (
                    e: Exception
                ) {

                    null
                }
            }
    }

    if (
        artworkBytes != null
    ) {

        val bitmap =
            remember(
                artworkBytes
            ) {

                android.graphics.BitmapFactory
                    .decodeByteArray(
                        artworkBytes,
                        0,
                        artworkBytes!!.size
                    )
            }

        if (bitmap != null) {

            Image(

                bitmap =
                    bitmap.asImageBitmap(),

                contentDescription =
                    "Carátula de ${song.title}",

                contentScale =
                    ContentScale.Crop,

                modifier =
                    modifier
                        .clip(
                            RoundedCornerShape(
                                10.dp
                            )
                        )
            )

        } else {

            ArtworkPlaceholder(
                modifier
            )
        }

    } else {

        ArtworkPlaceholder(
            modifier
        )
    }
}

@Composable
private fun ArtworkPlaceholder(
    modifier: Modifier
) {

    Box(

        modifier =
            modifier
                .clip(
                    RoundedCornerShape(
                        10.dp
                    )
                )
                .background(
                    Color(0xFF202020)
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
                Color.Gray,

            modifier =
                Modifier.size(
                    30.dp
                )
        )
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
    onPrevious: () -> Unit,
    onShuffle: () -> Unit
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

        colorScheme =
            darkColorScheme(

                background =
                    Color.Black,

                surface =
                    Color(0xFF111111),

                primary =
                    Color.White
            )
    ) {

        Surface(

            modifier =
                Modifier.fillMaxSize(),

            color =
                Color.Black
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

                    Column(

                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(

                            text =
                                "BLACKWALK",

                            color =
                                Color.White,

                            fontSize =
                                28.sp,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(

                            text =
                                "MUSIC",

                            color =
                                Color.Gray,

                            fontSize =
                                13.sp,

                            letterSpacing =
                                4.sp
                        )
                    }

                    IconButton(

                        onClick =
                            onShuffle
                    ) {

                        Icon(

                            imageVector =
                                Icons.Default.Shuffle,

                            contentDescription =
                                "Reproducción aleatoria",

                            tint =
                                Color.White
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            18.dp
                        )
                )

                OutlinedTextField(

                    value =
                        searchText,

                    onValueChange = {
                        searchText = it
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    singleLine =
                        true,

                    leadingIcon = {

                        Icon(

                            imageVector =
                                Icons.Default.Search,

                            contentDescription =
                                null
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
                                        "Limpiar búsqueda"
                                )
                            }
                        }
                    },

                    placeholder = {

                        Text(
                            text =
                                "Buscar canción, artista o álbum"
                        )
                    },

                    colors =
                        OutlinedTextFieldDefaults.colors(

                            focusedTextColor =
                                Color.White,

                            unfocusedTextColor =
                                Color.White,

                            focusedBorderColor =
                                Color.White,

                            unfocusedBorderColor =
                                Color(0xFF444444),

                            focusedLeadingIconColor =
                                Color.White,

                            unfocusedLeadingIconColor =
                                Color.Gray,

                            focusedTrailingIconColor =
                                Color.White,

                            unfocusedTrailingIconColor =
                                Color.Gray,

                            focusedPlaceholderColor =
                                Color.Gray,

                            unfocusedPlaceholderColor =
                                Color.Gray,

                            cursorColor =
                                Color.White
                        ),

                    shape =
                        RoundedCornerShape(
                            14.dp
                        )
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            14.dp
                        )
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
                                "Biblioteca",

                            color =
                                Color.White,

                            fontSize =
                                24.sp,

                            fontWeight =
                                FontWeight.Bold
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
                                Color.Gray,

                            fontSize =
                                13.sp
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
                                    Color.White
                            )
                        }

                        DropdownMenu(

                            expanded =
                                showSortMenu,

                            onDismissRequest = {

                                showSortMenu =
                                    false
                            }
                        ) {

                            DropdownMenuItem(

                                text = {
                                    Text(
                                        "Más recientes"
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
                                        "Más antiguas"
                                    )
                                },

                                onClick = {

                                    sortMode =
                                        SongSort.OLDEST

                                    showSortMenu =
                                        false
                                }
                            )

                            HorizontalDivider()

                            DropdownMenuItem(

                                text = {
                                    Text(
                                        "Título"
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
                                        "Artista"
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
                                        "Álbum"
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
                                        "Duración"
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
                        Modifier.height(
                            8.dp
                        )
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

                            Icon(

                                imageVector =
                                    Icons.Default.MusicNote,

                                contentDescription =
                                    null,

                                tint =
                                    Color.DarkGray,

                                modifier =
                                    Modifier.size(
                                        60.dp
                                    )
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        12.dp
                                    )
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
                                    Color.Gray
                            )
                        }
                    }

                } else {

                    LazyColumn(

                        modifier =
                            Modifier.weight(1f)
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
                            onPrevious
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

        AlbumArtwork(

            song =
                song,

            modifier =
                Modifier.size(
                    58.dp
                )
        )

        Spacer(
            modifier =
                Modifier.width(
                    14.dp
                )
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
                        Color.White
                    else
                        Color(0xFFE8E8E8),

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
                    Color.Gray,

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
                        Color.DarkGray,

                    fontSize =
                        11.sp,

                    maxLines =
                        1
                )
            }
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
                Modifier.padding(
                    12.dp
                )
        ) {

            Row(

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                AlbumArtwork(

                    song =
                        song,

                    modifier =
                        Modifier.size(
                            55.dp
                        )
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            12.dp
                        )
                )

                Column(

                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(

                        text =
                            song.title,

                        color =
                            Color.White,

                        fontWeight =
                            FontWeight.Bold,

                        maxLines =
                            1
                    )

                    Text(

                        text =
                            song.artist,

                        color =
                            Color.Gray,

                        fontSize =
                            12.sp,

                        maxLines =
                            1
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

                    onClick =
                        onPrevious
                ) {

                    Icon(

                        imageVector =
                            Icons.Default.SkipPrevious,

                        contentDescription =
                            "Anterior",

                        tint =
                            Color.White
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
                            Color.White,

                        modifier =
                            Modifier.size(
                                34.dp
                            )
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
                            Color.White
                    )
                }
            }
        }
    }
}
