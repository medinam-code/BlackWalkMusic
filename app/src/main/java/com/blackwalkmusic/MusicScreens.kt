package com.blackwalkmusic

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import kotlin.math.max

private val Black = Color(0xFF08090C)
private val DarkSurface = Color(0xFF111318)
private val DarkSurface2 = Color(0xFF181B21)
private val Accent = Color(0xFF7C4DFF)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF9CA3AF)

@Composable
fun BlackWalkMusicScreen(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    favoriteIds: Set<Long>,
    currentPosition: Long,
    duration: Long,
    onSongClick: (Song) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onOpenPlayer: () -> Unit,
    onFavorite: (Song) -> Unit
) {
    var searchVisible by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var showFolders by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }

    val filteredSongs = remember(songs, searchText) {
        if (searchText.isBlank()) {
            songs
        } else {
            val query = searchText.trim().lowercase()

            songs.filter {
                it.title.lowercase().contains(query) ||
                    it.artist.lowercase().contains(query) ||
                    it.album.lowercase().contains(query) ||
                    it.folderName.lowercase().contains(query)
            }
        }
    }

    val progress = if (duration > 0L) {
        (currentPosition.toFloat() / duration.toFloat())
            .coerceIn(0f, 1f)
    } else {
        0f
    }

    ScaffoldBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {

            if (searchVisible) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 12.dp,
                            end = 12.dp,
                            top = 12.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = {
                            searchText = it
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = {
                            Text("Buscar música")
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Buscar"
                            )
                        }
                    )

                    IconButton(
                        onClick = {
                            searchText = ""
                            searchVisible = false
                        }
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Cerrar búsqueda"
                        )
                    }
                }
            } else {
                TopBar(
                    songCount = songs.size,
                    onSearch = {
                        searchVisible = true
                    },
                    onFolders = {
                        showFolders = true
                    },
                    onSort = {
                        showSort = true
                    }
                )
            }

            if (filteredSongs.isEmpty()) {
                EmptyMusicState(
                    hasSearch = searchText.isNotBlank()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 10.dp,
                        end = 10.dp,
                        top = 8.dp,
                        bottom = if (currentSong != null) {
                            92.dp
                        } else {
                            16.dp
                        }
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = filteredSongs,
                        key = { song -> song.id }
                    ) { song ->

                        SongRow(
                            song = song,
                            isCurrent = currentSong?.id == song.id,
                            isPlaying = isPlaying,
                            isFavorite = favoriteIds.contains(song.id),
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

            if (currentSong != null) {
                MiniPlayer(
                    song = currentSong,
                    isPlaying = isPlaying,
                    progress = progress,
                    onOpen = onOpenPlayer,
                    onPlayPause = onPlayPause,
                    onPrevious = onPrevious,
                    onNext = onNext
                )
            }
        }
    }

    if (showFolders) {
        FolderDialog(
            songs = songs,
            onDismiss = {
                showFolders = false
            },
            onFolderSelected = {
                showFolders = false
            }
        )
    }

    if (showSort) {
        SortDialog(
            onDismiss = {
                showSort = false
            }
        )
    }
}

@Composable
private fun ScaffoldBackground(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0B0C10),
                        Black
                    )
                )
            )
    ) {
        content()
    }
}

@Composable
private fun TopBar(
    songCount: Int,
    onSearch: () -> Unit,
    onFolders: () -> Unit,
    onSort: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 8.dp,
                top = 16.dp,
                bottom = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "BlackWalkMusic",
                color = TextPrimary,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "$songCount canciones",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        IconButton(onClick = onSearch) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Buscar",
                tint = TextPrimary
            )
        }

        IconButton(onClick = onFolders) {
            Icon(
                Icons.Default.Folder,
                contentDescription = "Carpetas",
                tint = TextPrimary
            )
        }

        IconButton(onClick = onSort) {
            Icon(
                Icons.Default.Sort,
                contentDescription = "Ordenar",
                tint = TextPrimary
            )
        }
    }
}

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
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = if (isCurrent) {
            Accent.copy(alpha = 0.18f)
        } else {
            DarkSurface.copy(alpha = 0.72f)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 10.dp,
                    vertical = 9.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        if (isCurrent) {
                            Accent.copy(alpha = 0.35f)
                        } else {
                            DarkSurface2
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (
                        isCurrent && isPlaying
                    ) {
                        Icons.Default.PlayArrow
                    } else {
                        Icons.Default.MusicNote
                    },
                    contentDescription = null,
                    tint = if (isCurrent) {
                        Accent
                    } else {
                        TextSecondary
                    }
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    color = if (isCurrent) {
                        Color.White
                    } else {
                        TextPrimary
                    },
                    fontSize = 15.sp,
                    fontWeight = if (isCurrent) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${song.artist} • ${song.album}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onFavorite
            ) {
                Icon(
                    imageVector = if (isFavorite) {
                        Icons.Default.Favorite
                    } else {
                        Icons.Default.FavoriteBorder
                    },
                    contentDescription = "Favorito",
                    tint = if (isFavorite) {
                        Color(0xFFFF4D6D)
                    } else {
                        TextSecondary
                    }
                )
            }
        }
    }
}

@Composable
private fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        color = DarkSurface2
    ) {
        Column {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 7.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Accent.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Accent
                    )
                }

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = song.title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = song.artist,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onPrevious
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Anterior",
                        tint = TextPrimary
                    )
                }

                IconButton(
                    onClick = onPlayPause
                ) {
                    Icon(
                        if (isPlaying) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
                        contentDescription = "Reproducir",
                        tint = TextPrimary
                    )
                }

                IconButton(
                    onClick = onNext
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Siguiente",
                        tint = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyMusicState(
    hasSearch: Boolean
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = TextSecondary
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = if (hasSearch) {
                    "No se encontró música"
                } else {
                    "No hay canciones"
                },
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (hasSearch) {
                    "Prueba con otro nombre"
                } else {
                    "Agrega música al dispositivo"
                },
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var sliderPosition by remember(
        currentPosition,
        duration
    ) {
        mutableFloatStateOf(
            if (duration > 0L) {
                currentPosition
                    .toFloat()
                    .div(duration.toFloat())
                    .coerceIn(0f, 1f)
            } else {
                0f
            }
        )
    }

    var showQueue by remember {
        mutableStateOf(false)
    }

    var showShare by remember {
        mutableStateOf(false)
    }

    val positionText =
        formatTime(currentPosition)

    val durationText =
        formatTime(duration)

    ScaffoldBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(
                    horizontal = 18.dp
                )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 14.dp,
                        bottom = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = onBack
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = TextPrimary
                    )
                }

                Text(
                    text = "REPRODUCIENDO",
                    modifier = Modifier.weight(1f),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = {
                        showQueue = true
                    }
                ) {
                    Icon(
                        Icons.Default.QueueMusic,
                        contentDescription = "Cola",
                        tint = TextPrimary
                    )
                }

                IconButton(
                    onClick = {
                        showShare = true
                    }
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Compartir",
                        tint = TextPrimary
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Box(
                        modifier = Modifier
                            .size(270.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Accent,
                                        Color(0xFF272A35)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(105.dp),
                            tint = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(28.dp)
                    )

                    Text(
                        text = song.title,
                        color = TextPrimary,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = song.artist,
                        color = TextSecondary,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = song.album,
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Slider(
                value = sliderPosition,
                onValueChange = {
                    sliderPosition = it
                },
                onValueChangeFinished = {
                    val newPosition =
                        if (duration > 0L) {
                            (
                                sliderPosition *
                                    duration.toFloat()
                                ).toLong()
                            } else {
                                0L
                            }

                    onSeek(
                        newPosition
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = positionText,
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Text(
                    text = durationText,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {

                PlayerSmallButton(
                    icon = Icons.Default.Shuffle,
                    active = shuffleEnabled,
                    onClick = onShuffle
                )

                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Anterior",
                        modifier = Modifier.size(34.dp),
                        tint = TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Accent)
                        .clickable(
                            onClick = onPlayPause
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
                        contentDescription = "Play",
                        modifier = Modifier.size(38.dp),
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Siguiente",
                        modifier = Modifier.size(34.dp),
                        tint = TextPrimary
                    )
                }

                PlayerSmallButton(
                    icon = Icons.Default.Repeat,
                    active =
                        repeatMode != Player.REPEAT_MODE_OFF,
                    onClick = onRepeat
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = onFavorite
                ) {
                    Icon(
                        if (isFavorite) {
                            Icons.Default.Favorite
                        } else {
                            Icons.Default.FavoriteBorder
                        },
                        contentDescription = "Favorito",
                        modifier = Modifier.size(27.dp),
                        tint = if (isFavorite) {
                            Color(0xFFFF4D6D)
                        } else {
                            TextSecondary
                        }
                    )
                }
            }
        }
    }

    if (showQueue) {
        QueueSheet(
            queue = queue,
            currentSong = song,
            onDismiss = {
                showQueue = false
            },
            onSongClick = {
                onQueueSongClick(it)
                showQueue = false
            }
        )
    }

    if (showShare) {
        AlertDialog(
            onDismissRequest = {
                showShare = false
            },
            title = {
                Text("Compartir")
            },
            text = {
                Text(
                    "Comparte \"${song.title}\" desde el menú de compartir de Android."
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showShare = false
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun PlayerSmallButton(
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) {
                Accent
            } else {
                TextSecondary
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSheet(
    queue: List<Song>,
    currentSong: Song,
    onDismiss: () -> Unit,
    onSongClick: (Song) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 18.dp,
                        vertical = 10.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.QueueMusic,
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Text(
                    text = "Cola de reproducción",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "La cola está vacía",
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = queue,
                        key = { it.id }
                    ) { item ->

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSongClick(item)
                                }
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = 10.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint =
                                    if (item.id ==
                                        currentSong.id
                                    ) {
                                        Accent
                                    } else {
                                        TextSecondary
                                    }
                            )

                            Spacer(
                                modifier = Modifier.width(12.dp)
                            )

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = item.title,
                                    fontWeight =
                                        if (
                                            item.id ==
                                            currentSong.id
                                        ) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        },
                                    maxLines = 1,
                                    overflow =
                                        TextOverflow.Ellipsis
                                )

                                Text(
                                    text = item.artist,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow =
                                        TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderDialog(
    songs: List<Song>,
    onDismiss: () -> Unit,
    onFolderSelected: (String) -> Unit
) {
    val folders = remember(songs) {
        MusicRepository.getFolders(songs)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Carpetas")
        },
        text = {
            if (folders.isEmpty()) {
                Text("No se encontraron carpetas.")
            } else {
                LazyColumn(
                    modifier = Modifier.height(300.dp)
                ) {
                    items(
                        items = folders,
                        key = { it }
                    ) { folder ->

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onFolderSelected(folder)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null
                            )

                            Spacer(
                                modifier = Modifier.width(10.dp)
                            )

                            Text(
                                text =
                                    folder.substringAfterLast(
                                        "/"
                                    )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = onDismiss
            ) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
private fun SortDialog(
    onDismiss: () -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Ordenar música")
        },
        text = {
            Column {
                Text(
                    "La biblioteca está ordenada alfabéticamente por título.",
                    color = TextSecondary
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Box {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            expanded = true
                        }
                    ) {
                        Icon(
                            Icons.Default.Sort,
                            contentDescription = null
                        )

                        Spacer(
                            modifier = Modifier.width(6.dp)
                        )

                        Text("Título")
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = {
                            expanded = false
                        }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text("Título")
                            },
                            onClick = {
                                expanded = false
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text("Artista")
                            },
                            onClick = {
                                expanded = false
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text("Álbum")
                            },
                            onClick = {
                                expanded = false
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text("Más recientes")
                            },
                            onClick = {
                                expanded = false
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = onDismiss
            ) {
                Text("Cerrar")
            }
        }
    )
}

private fun formatTime(
    milliseconds: Long
): String {
    val safe =
        max(
            0L,
            milliseconds
        )

    val totalSeconds =
        safe / 1000L

    val minutes =
        totalSeconds / 60L

    val seconds =
        totalSeconds % 60L

    return "%d:%02d".format(
        minutes,
        seconds
    )
}
