@file:OptIn(
androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.blackwalkmusic

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.media3.common.Player
import kotlin.math.max

private val BlackBackground = Color(0xFF0B0B0F)
private val BlackSurface = Color(0xFF15151A)
private val BlackSurface2 = Color(0xFF1D1D23)

private fun albumArtUri(albumId: Long): Uri {
return Uri.parse(
"content://media/external/audio/albumart/$albumId"
)
}

private fun formatTime(milliseconds: Long): String {
val totalSeconds =
(milliseconds / 1000L).coerceAtLeast(0L)

val minutes =
    totalSeconds / 60L

val seconds =
    totalSeconds % 60L

return "%d:%02d".format(
    minutes,
    seconds
)

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
var searchVisible by remember {
mutableStateOf(false)
}

var searchText by remember {
    mutableStateOf("")
}

val filteredSongs = remember(
    songs,
    searchText
) {
    if (searchText.isBlank()) {
        songs
    } else {
        val query =
            searchText.trim()

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
}

Surface(
    modifier = Modifier.fillMaxSize(),
    color = BlackBackground
) {

    Scaffold(
        containerColor = BlackBackground,

        topBar = {

            if (searchVisible) {

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = BlackSurface
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 8.dp,
                                vertical = 6.dp
                            ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        IconButton(
                            onClick = {
                                searchVisible = false
                                searchText = ""
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Close,
                                contentDescription =
                                    "Cerrar búsqueda",
                                tint = Color.White
                            )
                        }

                        OutlinedTextField(
                            value = searchText,

                            onValueChange = {
                                searchText = it
                            },

                            modifier =
                                Modifier.weight(1f),

                            singleLine = true,

                            placeholder = {
                                Text(
                                    "Buscar canción, artista o álbum"
                                )
                            }
                        )
                    }
                }

            } else {

                TopAppBar(

                    title = {

                        Column {

                            Text(
                                text =
                                    "BlackWalk Music",
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                text =
                                    "${songs.size} canciones",
                                style =
                                    MaterialTheme.typography
                                        .labelSmall
                            )
                        }
                    },

                    actions = {

                        IconButton(
                            onClick = {
                                searchVisible = true
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Search,
                                contentDescription =
                                    "Buscar"
                            )
                        }
                    }
                )
            }
        },

        bottomBar = {

            if (currentSong != null) {

                MiniPlayer(
                    song = currentSong,

                    isPlaying = isPlaying,

                    onClick = onOpenPlayer,

                    onPlayPause =
                        onPlayPause,

                    onNext =
                        onNext,

                    onPrevious =
                        onPrevious
                )
            }
        }
    ) { paddingValues ->

        if (filteredSongs.isEmpty()) {

            EmptyMusicState(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(
                            BlackBackground
                        )
            )

        } else {

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            BlackBackground
                        )
                        .padding(
                            paddingValues
                        )
                        .navigationBarsPadding()
            ) {

                item {

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = 10.dp
                                )
                    ) {

                        Surface(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onShuffle()
                                    },

                            shape =
                                RoundedCornerShape(
                                    14.dp
                                ),

                            color =
                                BlackSurface2
                        ) {

                            Row(
                                modifier =
                                    Modifier.padding(
                                        14.dp
                                    ),

                                verticalAlignment =
                                    Alignment.CenterVertically,

                                horizontalArrangement =
                                    Arrangement.Center
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.Shuffle,
                                    contentDescription =
                                        "Aleatorio"
                                )

                                Spacer(
                                    modifier =
                                        Modifier.width(8.dp)
                                )

                                Text(
                                    text = "Aleatorio",
                                    fontWeight =
                                        FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                items(
                    items = filteredSongs,
                    key = { song ->
                        song.id
                    }
                ) { song ->

                    SongRow(
                        song = song,

                        isCurrent =
                            currentSong?.id ==
                                song.id,

                        isPlaying =
                            isPlaying &&
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

                item {
                    Spacer(
                        modifier =
                            Modifier.height(90.dp)
                    )
                }
            }
        }
    }
}

}

/*

* ============================================================
* CANCIÓN
* ============================================================
  */

@Composable
private fun SongRow(
song: Song,
isCurrent: Boolean,
isPlaying: Boolean,
isFavorite: Boolean,
onClick: () -> Unit,
onFavorite: () -> Unit
) {

Surface(
    modifier =
        Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },

    color =
        if (isCurrent) {
            Color(0xFF25252D)
        } else {
            BlackBackground
        }
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 7.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        AlbumArt(
            albumId = song.albumId,
            modifier =
                Modifier.size(54.dp)
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
                    if (isPlaying) {
                        "▶ ${song.title}"
                    } else {
                        song.title
                    },

                color =
                    Color.White,

                fontWeight =
                    if (isCurrent) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },

                maxLines = 1,

                overflow =
                    TextOverflow.Ellipsis
            )

            Text(
                text = song.artist,

                color =
                    Color.LightGray,

                style =
                    MaterialTheme.typography
                        .bodySmall,

                maxLines = 1,

                overflow =
                    TextOverflow.Ellipsis
            )

            Text(
                text = song.album,

                color =
                    Color.Gray,

                style =
                    MaterialTheme.typography
                        .labelSmall,

                maxLines = 1,

                overflow =
                    TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onFavorite
        ) {

            Icon(
                imageVector =
                    if (isFavorite) {
                        Icons.Default.Favorite
                    } else {
                        Icons.Default.FavoriteBorder
                    },

                contentDescription =
                    "Favorito",

                tint =
                    if (isFavorite) {
                        MaterialTheme.colorScheme
                            .primary
                    } else {
                        Color.LightGray
                    }
            )
        }

        Text(
            text =
                formatTime(song.duration),

            color =
                Color.Gray,

            style =
                MaterialTheme.typography
                    .labelSmall
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
private fun MiniPlayer(
song: Song,
isPlaying: Boolean,
onClick: () -> Unit,
onPlayPause: () -> Unit,
onNext: () -> Unit,
onPrevious: () -> Unit
) {

Surface(
    modifier =
        Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },

    color = BlackSurface,

    tonalElevation = 8.dp
) {

    Column {

        LinearProgressIndicator(
            progress = {
                0f
            },

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        horizontal = 10.dp,
                        vertical = 6.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            AlbumArt(
                albumId = song.albumId,
                modifier =
                    Modifier.size(48.dp)
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
                    text = song.title,

                    color =
                        Color.White,

                    fontWeight =
                        FontWeight.Bold,

                    maxLines = 1,

                    overflow =
                        TextOverflow.Ellipsis
                )

                Text(
                    text = song.artist,

                    color =
                        Color.LightGray,

                    style =
                        MaterialTheme.typography
                            .labelSmall,

                    maxLines = 1,

                    overflow =
                        TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onPrevious
            ) {

                Icon(
                    imageVector =
                        Icons.Default.SkipPrevious,

                    contentDescription =
                        "Anterior"
                )
            }

            IconButton(
                onClick = onPlayPause
            ) {

                Icon(
                    imageVector =
                        if (isPlaying) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },

                    contentDescription =
                        if (isPlaying) {
                            "Pausar"
                        } else {
                            "Reproducir"
                        }
                )
            }

            IconButton(
                onClick = onNext
            ) {

                Icon(
                    imageVector =
                        Icons.Default.SkipNext,

                    contentDescription =
                        "Siguiente"
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

var sliderPosition by remember(
    currentPosition
) {
    mutableFloatStateOf(
        currentPosition.toFloat()
    )
}

val safeDuration =
    max(duration, 1L)

val safePosition =
    currentPosition.coerceIn(
        0L,
        safeDuration
    )

/*
 * Cuando cambia realmente la canción o avanza
 * la reproducción, actualizamos el slider.
 */
if (
    !sliderPosition.isFinite() ||
    kotlin.math.abs(
        sliderPosition -
            currentPosition.toFloat()
    ) > 1500f
) {

    sliderPosition =
        safePosition.toFloat()
}

Surface(
    modifier =
        Modifier.fillMaxSize(),

    color = BlackBackground
) {

    Scaffold(

        containerColor =
            BlackBackground,

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        text = "Reproduciendo",
                        fontWeight =
                            FontWeight.Bold
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.ArrowBack,

                            contentDescription =
                                "Volver"
                        )
                    }
                },

                actions = {

                    IconButton(
                        onClick = {
                            showQueue = true
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.QueueMusic,

                            contentDescription =
                                "Cola"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        BlackBackground
                    )
                    .padding(
                        paddingValues
                    )
                    .padding(
                        horizontal = 22.dp
                    )
                    .navigationBarsPadding(),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Spacer(
                modifier =
                    Modifier.height(25.dp)
            )

            /*
             * CARÁTULA GRANDE
             */
            AlbumArt(
                albumId = song.albumId,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(330.dp)
                        .clip(
                            RoundedCornerShape(
                                24.dp
                            )
                        )
            )

            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )

            Text(
                text = song.title,

                color = Color.White,

                fontSize = 24.sp,

                fontWeight =
                    FontWeight.Bold,

                maxLines = 2,

                overflow =
                    TextOverflow.Ellipsis
            )

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            Text(
                text = song.artist,

                color =
                    Color.LightGray,

                style =
                    MaterialTheme.typography
                        .bodyLarge,

                maxLines = 1,

                overflow =
                    TextOverflow.Ellipsis
            )

            Text(
                text = song.album,

                color =
                    Color.Gray,

                style =
                    MaterialTheme.typography
                        .bodyMedium,

                maxLines = 1,

                overflow =
                    TextOverflow.Ellipsis
            )

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            /*
             * SLIDER
             */
            Slider(

                value =
                    sliderPosition.coerceIn(
                        0f,
                        safeDuration.toFloat()
                    ),

                onValueChange = {
                    sliderPosition = it
                },

                onValueChangeFinished = {
                    onSeek(
                        sliderPosition.toLong()
                    )
                },

                valueRange =
                    0f..safeDuration.toFloat(),

                modifier =
                    Modifier.fillMaxWidth()
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
                        Color.Gray,

                    style =
                        MaterialTheme.typography
                            .labelSmall
                )

                Text(
                    text =
                        formatTime(
                            duration
                        ),

                    color =
                        Color.Gray,

                    style =
                        MaterialTheme.typography
                            .labelSmall
                )
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
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
                    onClick = onShuffle
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Shuffle,

                        contentDescription =
                            "Aleatorio",

                        tint =
                            if (shuffleEnabled) {
                                MaterialTheme.colorScheme
                                    .primary
                            } else {
                                Color.White
                            }
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

                        modifier =
                            Modifier.size(38.dp)
                    )
                }

                Surface(

                    modifier =
                        Modifier
                            .size(72.dp)
                            .clickable {
                                onPlayPause()
                            },

                    shape =
                        CircleShape,

                    color =
                        MaterialTheme.colorScheme
                            .primary
                ) {

                    Box(
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(

                            imageVector =
                                if (isPlaying) {
                                    Icons.Default.Pause
                                } else {
                                    Icons.Default.PlayArrow
                                },

                            contentDescription =
                                if (isPlaying) {
                                    "Pausar"
                                } else {
                                    "Reproducir"
                                },

                            tint =
                                MaterialTheme.colorScheme
                                    .onPrimary,

                            modifier =
                                Modifier.size(40.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onNext
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.SkipNext,

                        contentDescription =
                            "Siguiente",

                        modifier =
                            Modifier.size(38.dp)
                    )
                }

                IconButton(
                    onClick = onRepeat
                ) {

                    Icon(
                        imageVector =
                            if (
                                repeatMode ==
                                    Player.REPEAT_MODE_ONE
                            ) {
                                Icons.Default.RepeatOne
                            } else {
                                Icons.Default.Repeat
                            },

                        contentDescription =
                            "Repetición",

                        tint =
                            if (
                                repeatMode !=
                                    Player.REPEAT_MODE_OFF
                            ) {
                                MaterialTheme.colorScheme
                                    .primary
                            } else {
                                Color.White
                            }
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            IconButton(
                onClick = onFavorite
            ) {

                Icon(
                    imageVector =
                        if (isFavorite) {
                            Icons.Default.Favorite
                        } else {
                            Icons.Default.FavoriteBorder
                        },

                    contentDescription =
                        "Favorito",

                    tint =
                        if (isFavorite) {
                            MaterialTheme.colorScheme
                                .primary
                        } else {
                            Color.White
                        },

                    modifier =
                        Modifier.size(30.dp)
                )
            }
        }
    }
}

/*
 * ========================================================
 * COLA
 * ========================================================
 */

if (showQueue) {

    ModalBottomSheet(

        onDismissRequest = {
            showQueue = false
        },

        containerColor =
            BlackSurface,

        sheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = false
            )
    ) {

        QueueSheet(
            queue = queue,

            currentSong = song,

            onSongClick = { queueSong ->

                onQueueSongClick(
                    queueSong
                )

                showQueue = false
            }
        )
    }
}

}

/*

* ============================================================
* COLA
* ============================================================
  */

@Composable
private fun QueueSheet(
queue: List<Song>,
currentSong: Song,
onSongClick: (Song) -> Unit
) {

Column(
    modifier =
        Modifier
            .fillMaxWidth()
            .background(
                BlackSurface
            )
            .padding(
                bottom = 20.dp
            )
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp,
                    vertical = 10.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Icon(
            imageVector =
                Icons.Default.QueueMusic,

            contentDescription = null
        )

        Spacer(
            modifier =
                Modifier.width(10.dp)
        )

        Text(
            text =
                "Cola de reproducción",

            color =
                Color.White,

            fontSize = 20.sp,

            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.width(8.dp)
        )

        Text(
            text =
                "${queue.size}",

            color =
                Color.Gray,

            style =
                MaterialTheme.typography
                    .labelMedium
        )
    }

    if (queue.isEmpty()) {

        Text(
            text =
                "La cola está vacía.",

            color =
                Color.LightGray,

            modifier =
                Modifier.padding(20.dp)
        )

    } else {

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(420.dp)
        ) {

            items(
                items = queue,
                key = { queueSong ->
                    queueSong.id
                }
            ) { queueSong ->

                val isCurrent =
                    queueSong.id ==
                        currentSong.id

                Surface(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSongClick(
                                    queueSong
                                )
                            },

                    color =
                        if (isCurrent) {
                            Color(0xFF292933)
                        } else {
                            BlackSurface
                        }
                ) {

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = 8.dp
                                ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        AlbumArt(
                            albumId =
                                queueSong.albumId,

                            modifier =
                                Modifier.size(48.dp)
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
                                    queueSong.title,

                                color =
                                    Color.White,

                                fontWeight =
                                    if (isCurrent) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    },

                                maxLines = 1,

                                overflow =
                                    TextOverflow.Ellipsis
                            )

                            Text(
                                text =
                                    queueSong.artist,

                                color =
                                    Color.Gray,

                                style =
                                    MaterialTheme.typography
                                        .labelSmall,

                                maxLines = 1,

                                overflow =
                                    TextOverflow.Ellipsis
                            )
                        }

                        if (isCurrent) {

                            Icon(
                                imageVector =
                                    Icons.Default.MusicNote,

                                contentDescription =
                                    "Reproduciendo",

                                tint =
                                    MaterialTheme.colorScheme
                                        .primary
                            )
                        }
                    }
                }
            }
        }
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
albumId: Long,
modifier: Modifier
) {

val uri =
    remember(albumId) {
        albumArtUri(albumId)
    }

Box(
    modifier =
        modifier
            .clip(
                RoundedCornerShape(12.dp)
            )
            .background(
                Color(0xFF25252D)
            ),

    contentAlignment =
        Alignment.Center
) {

    Icon(
        imageVector =
            Icons.Default.MusicNote,

        contentDescription = null,

        tint =
            Color.Gray,

        modifier =
            Modifier.size(42.dp)
    )

    AsyncImage(

        model = uri,

        contentDescription =
            "Carátula",

        modifier =
            Modifier
                .fillMaxSize()
                .clip(
                    RoundedCornerShape(12.dp)
                ),

        contentScale =
            ContentScale.Crop
    )
}

}

/*

* ============================================================
* SIN MÚSICA
* ============================================================
  */

@Composable
private fun EmptyMusicState(
modifier: Modifier = Modifier
) {

Box(
    modifier = modifier,

    contentAlignment =
        Alignment.Center
) {

    Column(

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Icon(
            imageVector =
                Icons.Default.MusicNote,

            contentDescription = null,

            tint =
                Color.White,

            modifier =
                Modifier.size(64.dp)
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Text(
            text =
                "No hay música disponible",

            color =
                Color.White,

            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(4.dp)
        )

        Text(
            text =
                "Agrega archivos de música al dispositivo.",

            color =
                Color.Gray
        )
    }
}

}
