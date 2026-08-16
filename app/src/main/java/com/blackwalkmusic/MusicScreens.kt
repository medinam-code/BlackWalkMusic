@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.blackwalkmusic

import android.content.Intent
import android.net.Uri

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material.icons.filled.VolumeUp

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.media3.common.Player

import coil.compose.AsyncImage

import kotlin.math.max


private val AppBackground =
    Color(0xFF020308)

private val SurfaceDark =
    Color(0xFF090B10)

private val SurfaceCard =
    Color(0xFF101217)

private val SurfaceCard2 =
    Color(0xFF15181F)

private val TextWhite =
    Color(0xFFF5F5F7)

private val TextGray =
    Color(0xFF9B9DA5)

private val TerereRed =
    Color(0xFFF12638)

private val TerereBlue =
    Color(0xFF1677FF)

private val NeonPurple =
    Color(0xFF9B38FF)


private enum class MusicTab {
    SONGS,
    FOLDERS,
    PLAYLISTS,
    FAVORITES
}


private enum class SortMode {
    TITLE,
    NEWEST
}


private fun albumArtUri(
    albumId: Long
): Uri {
    return Uri.parse(
        "content://media/external/audio/albumart/$albumId"
    )
}


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


/*
 * ============================================================
 * LOGO
 * ============================================================
 */

@Composable
private fun TerereLogo(
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier,
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = "TERERÉ",
            color = TerereRed,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic
        )

        Spacer(
            modifier = Modifier.width(6.dp)
        )

        Text(
            text = "MUSIC",
            color = TextWhite,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic
        )

        Spacer(
            modifier = Modifier.width(6.dp)
        )

        Text(
            text = "PY",
            color = TerereBlue,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic
        )
    }
}


/*
 * ============================================================
 * BANNER
 * ============================================================
 */

@Composable
private fun TerereBanner(
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier
            .clip(
                RoundedCornerShape(20.dp)
            )
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF120308),
                        Color(0xFF390814),
                        Color(0xFF101B3B),
                        Color(0xFF020308)
                    )
                )
            ),
        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text = "TERERÉ MUSIC",
                color = TextWhite,
                fontSize = 31.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "MÚSICA • PARAGUAY • PY",
                color = TerereBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                TerereRed,
                                NeonPurple,
                                TerereBlue
                            )
                        )
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
    currentPosition: Long = 0L,
    duration: Long = 0L,
    onSongClick: (Song) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onOpenPlayer: () -> Unit,
    onFavorite: (Song) -> Unit
) {

    var selectedTab by remember {
        mutableStateOf(
            MusicTab.SONGS
        )
    }

    var searchVisible by remember {
        mutableStateOf(false)
    }

    var searchText by remember {
        mutableStateOf("")
    }

    var showSortMenu by remember {
        mutableStateOf(false)
    }

    var sortMode by remember {
        mutableStateOf(
            SortMode.TITLE
        )
    }

    var showMenu by remember {
        mutableStateOf(false)
    }

    var selectedFolderPath by remember {
        mutableStateOf<String?>(null)
    }

    var showInfo by remember {
        mutableStateOf(false)
    }


    val filteredSongs =
        remember(
            songs,
            selectedTab,
            favoriteIds,
            searchText,
            sortMode,
            selectedFolderPath
        ) {

            var result: List<Song> =
                when (selectedTab) {

                    MusicTab.SONGS ->
                        songs

                    MusicTab.FAVORITES ->
                        songs.filter { song ->
                            favoriteIds.contains(
                                song.id
                            )
                        }

                    MusicTab.FOLDERS ->
                        if (
                            selectedFolderPath != null
                        ) {

                            songs.filter { song ->
                                song.folderPath ==
                                    selectedFolderPath
                            }

                        } else {

                            emptyList()
                        }

                    MusicTab.PLAYLISTS ->
                        emptyList()
                }


            if (
                searchText.isNotBlank()
            ) {

                val query =
                    searchText.trim()

                result =
                    result.filter { song ->

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


            result =
                when (sortMode) {

                    SortMode.TITLE ->
                        result.sortedBy {
                            it.title.lowercase()
                        }

                    SortMode.NEWEST ->
                        result.sortedByDescending {
                            it.dateAdded
                        }
                }

            result
        }


    Scaffold(
        modifier =
            Modifier.fillMaxSize(),

        containerColor =
            AppBackground,

        topBar = {

            if (searchVisible) {

                SearchBar(
                    value =
                        searchText,

                    onValueChange = {
                        searchText = it
                    },

                    onClose = {

                        searchVisible =
                            false

                        searchText = ""
                    }
                )

            } else {

                Surface(
                    color =
                        AppBackground
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 14.dp,
                                vertical = 10.dp
                            ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        IconButton(
                            onClick = {
                                showMenu = true
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Menu,

                                contentDescription =
                                    "Menú",

                                tint =
                                    TextWhite,

                                modifier =
                                    Modifier.size(32.dp)
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.width(4.dp)
                        )

                        TerereLogo(
                            modifier =
                                Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = {
                                searchVisible =
                                    true
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Search,

                                contentDescription =
                                    "Buscar",

                                tint =
                                    TextWhite,

                                modifier =
                                    Modifier.size(30.dp)
                            )
                        }


                        Box {

                            IconButton(
                                onClick = {
                                    showMenu = true
                                }
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.MoreVert,

                                    contentDescription =
                                        "Más opciones",

                                    tint =
                                        TextWhite,

                                    modifier =
                                        Modifier.size(30.dp)
                                )
                            }


                            DropdownMenu(
                                expanded =
                                    showMenu,

                                onDismissRequest = {
                                    showMenu =
                                        false
                                }
                            ) {

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Actualizar biblioteca"
                                        )
                                    },

                                    onClick = {
                                        showMenu =
                                            false
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Ajustes"
                                        )
                                    },

                                    onClick = {
                                        showMenu =
                                            false
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Información"
                                        )
                                    },

                                    onClick = {

                                        showMenu =
                                            false

                                        showInfo =
                                            true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },


        bottomBar = {

            currentSong?.let { song ->

                MiniPlayer(
                    song =
                        song,

                    isPlaying =
                        isPlaying,

                    currentPosition =
                        currentPosition,

                    duration =
                        duration,

                    onClick =
                        onOpenPlayer,

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

        LazyColumn(

            modifier = Modifier
                .fillMaxSize()
                .background(
                    AppBackground
                )
                .padding(
                    paddingValues
                ),

            verticalArrangement =
                Arrangement.spacedBy(
                    0.dp
                )
        ) {


            item {

                TerereBanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 18.dp,
                            vertical = 6.dp
                        )
                        .height(205.dp)
                )
            }


            item {

                MusicTabs(
                    selectedTab =
                        selectedTab,

                    onTabSelected = { tab ->

                        selectedTab =
                            tab

                        if (
                            tab !=
                                MusicTab.FOLDERS
                        ) {

                            selectedFolderPath =
                                null
                        }
                    }
                )
            }


            if (
                selectedTab ==
                    MusicTab.FOLDERS &&
                selectedFolderPath != null
            ) {

                item {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {

                                selectedFolderPath =
                                    null
                            }
                            .padding(
                                horizontal = 22.dp,
                                vertical = 8.dp
                            ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.ArrowBack,

                            contentDescription =
                                "Volver",

                            tint =
                                TerereBlue
                        )

                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )

                        Text(
                            text =
                                "Carpetas",

                            color =
                                TerereBlue,

                            fontSize =
                                16.sp,

                            fontWeight =
                                FontWeight.Medium
                        )
                    }
                }
            }


            if (
                selectedTab !=
                    MusicTab.PLAYLISTS &&
                !(
                    selectedTab ==
                        MusicTab.FOLDERS &&
                    selectedFolderPath == null
                )
            ) {

                item {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 22.dp,
                                vertical = 6.dp
                            ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onShuffle()
                                }
                                .padding(
                                    vertical = 10.dp
                                ),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Shuffle,

                                contentDescription =
                                    "Aleatorio",

                                tint =
                                    TerereRed,

                                modifier =
                                    Modifier.size(25.dp)
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(10.dp)
                            )

                            Text(
                                text =
                                    "Aleatorio",

                                color =
                                    TerereRed,

                                fontSize =
                                    18.sp,

                                fontWeight =
                                    FontWeight.Medium
                            )
                        }


                        Box {

                            Row(
                                modifier =
                                    Modifier
                                        .clickable {
                                            showSortMenu =
                                                true
                                        }
                                        .padding(
                                            vertical = 10.dp,
                                            horizontal = 4.dp
                                        ),

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.Sort,

                                    contentDescription =
                                        "Ordenar",

                                    tint =
                                        TextWhite,

                                    modifier =
                                        Modifier.size(25.dp)
                                )

                                Spacer(
                                    modifier =
                                        Modifier.width(8.dp)
                                )

                                Text(
                                    text =
                                        "Ordenar",

                                    color =
                                        TextGray,

                                    fontSize =
                                        17.sp
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
                                        Text("Título")
                                    },

                                    onClick = {

                                        sortMode =
                                            SortMode.TITLE

                                        showSortMenu =
                                            false
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Más recientes"
                                        )
                                    },

                                    onClick = {

                                        sortMode =
                                            SortMode.NEWEST

                                        showSortMenu =
                                            false
                                    }
                                )
                            }
                        }
                    }
                }
            }


            /*
             * CARPETAS
             */

            if (
                selectedTab ==
                    MusicTab.FOLDERS &&
                selectedFolderPath == null
            ) {

                val folders =
                    songs
                        .filter {
                            it.folderPath.isNotBlank()
                        }
                        .distinctBy {
                            it.folderPath
                        }
                        .sortedBy {
                            it.folderName.lowercase()
                        }


                if (
                    folders.isEmpty()
                ) {

                    item {
                        EmptyMusicState()
                    }

                } else {

                    items(
                        items =
                            folders,

                        key = { song ->
                            song.folderPath
                        }
                    ) { folderSong ->

                        FolderRow(
                            folder =
                                folderSong.folderName,

                            songCount =
                                songs.count { song ->
                                    song.folderPath ==
                                        folderSong.folderPath
                                },

                            onClick = {

                                selectedFolderPath =
                                    folderSong.folderPath
                            }
                        )
                    }
                }
            }


            /*
             * LISTAS
             */

            else if (
                selectedTab ==
                    MusicTab.PLAYLISTS
            ) {

                item {
                    EmptyPlaylistState()
                }
            }


            /*
             * MÚSICAS
             */

            else if (
                filteredSongs.isEmpty()
            ) {

                item {
                    EmptyMusicState()
                }

            } else {

                items(
                    items =
                        filteredSongs,

                    key = { song ->
                        song.id
                    }
                ) { song ->

                    SongRow(
                        song =
                            song,

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
                            onSongClick(
                                song
                            )
                        },

                        onFavorite = {
                            onFavorite(
                                song
                            )
                        }
                    )
                }
            }


            item {

                Spacer(
                    modifier = Modifier
                        .height(110.dp)
                        .navigationBarsPadding()
                )
            }
        }
    }


    if (showInfo) {

        AlertDialog(

            onDismissRequest = {
                showInfo = false
            },

            confirmButton = {

                androidx.compose.material3.TextButton(
                    onClick = {
                        showInfo = false
                    }
                ) {
                    Text("Cerrar")
                }
            },

            title = {
                Text(
                    "Tereré Music PY"
                )
            },

            text = {
                Text(
                    "Reproductor de música ligero y optimizado para Android.\n\n" +
                        "Biblioteca local • Carpetas • Listas • Favoritos"
                )
            }
        )
    }
}


/*
 * ============================================================
 * BUSCADOR
 * ============================================================
 */

@Composable
private fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onClose: () -> Unit
) {

    Surface(
        color =
            SurfaceDark
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
                onClick =
                    onClose
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Close,

                    contentDescription =
                        "Cerrar",

                    tint =
                        TextWhite
                )
            }


            OutlinedTextField(
                value =
                    value,

                onValueChange =
                    onValueChange,

                modifier =
                    Modifier.weight(1f),

                singleLine =
                    true,

                placeholder = {

                    Text(
                        "Buscar canción",
                        color =
                            TextGray
                    )
                }
            )
        }
    }
}


/*
 * ============================================================
 * PESTAÑAS
 * ============================================================
 */

@Composable
private fun MusicTabs(
    selectedTab: MusicTab,
    onTabSelected: (MusicTab) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(
                rememberScrollState()
            )
            .padding(
                horizontal = 12.dp
            )
    ) {

        TabButton(
            selected =
                selectedTab ==
                    MusicTab.SONGS,

            icon =
                Icons.Default.MusicNote,

            title =
                "Canciones",

            onClick = {
                onTabSelected(
                    MusicTab.SONGS
                )
            }
        )


        TabButton(
            selected =
                selectedTab ==
                    MusicTab.FOLDERS,

            icon =
                Icons.Default.Folder,

            title =
                "Carpetas",

            onClick = {
                onTabSelected(
                    MusicTab.FOLDERS
                )
            }
        )


        TabButton(
            selected =
                selectedTab ==
                    MusicTab.PLAYLISTS,

            icon =
                Icons.Default.QueueMusic,

            title =
                "Listas",

            onClick = {
                onTabSelected(
                    MusicTab.PLAYLISTS
                )
            }
        )


        TabButton(
            selected =
                selectedTab ==
                    MusicTab.FAVORITES,

            icon =
                Icons.Default.Favorite,

            title =
                "Favoritos",

            onClick = {
                onTabSelected(
                    MusicTab.FAVORITES
                )
            }
        )
    }
}


/*
 * ============================================================
 * BOTÓN DE PESTAÑA
 * ============================================================
 */

@Composable
private fun TabButton(
    selected: Boolean,
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .width(105.dp)
            .clickable {
                onClick()
            }
            .padding(
                top = 4.dp
            ),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector =
                icon,

            contentDescription =
                title,

            tint =
                if (selected) {
                    TerereRed
                } else {
                    TextGray
                },

            modifier =
                Modifier.size(30.dp)
        )

        Spacer(
            modifier =
                Modifier.height(4.dp)
        )

        Text(
            text =
                title,

            color =
                if (selected) {
                    TerereRed
                } else {
                    TextGray
                },

            fontSize =
                15.sp,

            fontWeight =
                if (selected) {
                    FontWeight.Medium
                } else {
                    FontWeight.Normal
                }
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    if (selected) {
                        TerereRed
                    } else {
                        Color.Transparent
                    }
                )
        )
    }
}


/*
 * ============================================================
 * CARPETA
 * ============================================================
 */

@Composable
private fun FolderRow(
    folder: String,
    songCount: Int,
    onClick: () -> Unit
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 4.dp
            )
            .clickable {
                onClick()
            },

        color =
            SurfaceCard,

        shape =
            RoundedCornerShape(13.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 14.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                imageVector =
                    Icons.Default.Folder,

                contentDescription =
                    "Carpeta",

                tint =
                    TerereBlue,

                modifier =
                    Modifier.size(34.dp)
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
                        folder,

                    color =
                        TextWhite,

                    fontSize =
                        17.sp,

                    fontWeight =
                        FontWeight.Medium,

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis
                )

                Text(
                    text =
                        if (songCount == 1) {
                            "1 canción"
                        } else {
                            "$songCount canciones"
                        },

                    color =
                        TextGray,

                    fontSize =
                        14.sp
                )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 4.dp
            )
            .clickable {
                onClick()
            },

        color =
            if (isCurrent) {
                Color(0xFF171A20)
            } else {
                SurfaceCard
            },

        shape =
            RoundedCornerShape(13.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 10.dp,
                    vertical = 7.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            if (isPlaying) {

                Icon(
                    imageVector =
                        Icons.Default.MusicNote,

                    contentDescription =
                        "Reproduciendo",

                    tint =
                        TerereRed,

                    modifier =
                        Modifier
                            .size(24.dp)
                            .padding(
                                end = 3.dp
                            )
                )

            } else {

                Spacer(
                    modifier =
                        Modifier.width(3.dp)
                )
            }


            AlbumArt(
                albumId =
                    song.albumId,

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
                        song.title,

                    color =
                        TextWhite,

                    fontSize =
                        17.sp,

                    fontWeight =
                        if (isCurrent) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        },

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis
                )


                Text(
                    text =
                        song.artist,

                    color =
                        TextGray,

                    fontSize =
                        14.sp,

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis
                )
            }


            Text(
                text =
                    formatTime(
                        song.duration
                    ),

                color =
                    TextGray,

                fontSize =
                    14.sp
            )


            IconButton(
                onClick =
                    onFavorite
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
                            TerereRed
                        } else {
                            TextGray
                        }
                )
            }


            Icon(
                imageVector =
                    Icons.Default.MoreVert,

                contentDescription =
                    "Más opciones",

                tint =
                    TextGray
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
    currentPosition: Long,
    duration: Long,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {

    val progress =
        if (duration > 0L) {

            (
                currentPosition.toFloat() /
                    duration.toFloat()
            ).coerceIn(
                0f,
                1f
            )

        } else {
            0f
        }


    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },

        color =
            SurfaceDark
    ) {

        Column {

            LinearProgressIndicator(
                progress = {
                    progress
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp),

                color =
                    TerereRed,

                trackColor =
                    Color(0xFF242731)
            )


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        horizontal = 10.dp,
                        vertical = 7.dp
                    ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                AlbumArt(
                    albumId =
                        song.albumId,

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
                            song.title,

                        color =
                            TextWhite,

                        fontWeight =
                            FontWeight.Bold,

                        maxLines =
                            1,

                        overflow =
                            TextOverflow.Ellipsis
                    )


                    Text(
                        text =
                            song.artist,

                        color =
                            TextGray,

                        fontSize =
                            13.sp,

                        maxLines =
                            1,

                        overflow =
                            TextOverflow.Ellipsis
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
                            TextWhite
                    )
                }


                IconButton(
                    onClick =
                        onPlayPause
                ) {

                    Icon(
                        imageVector =
                            if (isPlaying) {
                                Icons.Default.Pause
                            } else {
                                Icons.Default.PlayArrow
                            },

                        contentDescription =
                            "Reproducir",

                        tint =
                            TextWhite,

                        modifier =
                            Modifier.size(31.dp)
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

    var showMore by remember {
        mutableStateOf(false)
    }

    var showInfo by remember {
        mutableStateOf(false)
    }


    var sliderPosition by remember(
        song.id
    ) {

        mutableFloatStateOf(
            currentPosition.toFloat()
        )
    }


    val safeDuration =
        max(
            duration,
            1L
        )


    val safePosition =
        currentPosition.coerceIn(
            0L,
            safeDuration
        )


    LaunchedEffect(
        song.id
    ) {

        sliderPosition =
            safePosition.toFloat()
    }


    LaunchedEffect(
        currentPosition
    ) {

        sliderPosition =
            safePosition.toFloat()
    }


    Scaffold(
        modifier =
            Modifier.fillMaxSize(),

        containerColor =
            AppBackground,

        topBar = {

            Surface(
                color =
                    AppBackground
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = 10.dp
                        ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    IconButton(
                        onClick =
                            onBack
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.ChevronLeft,

                            contentDescription =
                                "Cerrar reproductor",

                            tint =
                                TextWhite,

                            modifier =
                                Modifier.size(34.dp)
                        )
                    }


                    TerereLogo(
                        modifier =
                            Modifier.weight(1f)
                    )


                    Box {

                        IconButton(
                            onClick = {
                                showMore = true
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.MoreVert,

                                contentDescription =
                                    "Más",

                                tint =
                                    TextWhite,

                                modifier =
                                    Modifier.size(28.dp)
                            )
                        }


                        DropdownMenu(
                            expanded =
                                showMore,

                            onDismissRequest = {
                                showMore = false
                            }
                        ) {

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Información"
                                    )
                                },

                                onClick = {

                                    showMore =
                                        false

                                    showInfo =
                                        true
                                }
                            )


                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Compartir"
                                    )
                                },

                                onClick = {

                                    showMore =
                                        false

                                    shareSong(
                                        song
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    paddingValues
                )
                .padding(
                    horizontal = 22.dp
                ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            PlayerArtwork(
                albumId =
                    song.albumId,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(410.dp)
            )


            Spacer(
                modifier =
                    Modifier.height(20.dp)
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
                            song.title,

                        color =
                            TextWhite,

                        fontSize =
                            28.sp,

                        fontWeight =
                            FontWeight.Bold,

                        maxLines =
                            2,

                        overflow =
                            TextOverflow.Ellipsis
                    )


                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )


                    Text(
                        text =
                            song.artist,

                        color =
                            TextGray,

                        fontSize =
                            18.sp,

                        maxLines =
                            1,

                        overflow =
                            TextOverflow.Ellipsis
                    )
                }


                IconButton(
                    onClick =
                        onFavorite
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
                                TerereRed
                            } else {
                                TextWhite
                            },

                        modifier =
                            Modifier.size(34.dp)
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            Slider(
                value =
                    sliderPosition.coerceIn(
                        0f,
                        safeDuration.toFloat()
                    ),

                onValueChange = { value ->

                    sliderPosition =
                        value
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
                        TextWhite,

                    fontSize =
                        14.sp
                )


                Text(
                    text =
                        formatTime(
                            duration
                        ),

                    color =
                        TextGray,

                    fontSize =
                        14.sp
                )
            }


            Spacer(
                modifier =
                    Modifier.height(14.dp)
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
                            if (shuffleEnabled) {
                                TerereRed
                            } else {
                                TextWhite
                            },

                        modifier =
                            Modifier.size(31.dp)
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
                            Modifier.size(43.dp)
                    )
                }


                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .border(
                            BorderStroke(
                                2.dp,

                                Brush.linearGradient(
                                    listOf(
                                        TerereRed,
                                        NeonPurple,
                                        TerereBlue
                                    )
                                )
                            ),

                            CircleShape
                        )
                        .clickable {
                            onPlayPause()
                        },

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
                            "Reproducir",

                        tint =
                            TextWhite,

                        modifier =
                            Modifier.size(47.dp)
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
                            Modifier.size(43.dp)
                    )
                }


                IconButton(
                    onClick =
                        onRepeat
                ) {

                    RepeatButtonIcon(
                        repeatMode =
                            repeatMode
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )


            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = {}
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Tune,

                        contentDescription =
                            "Ecualizador",

                        tint =
                            TextWhite,

                        modifier =
                            Modifier.size(28.dp)
                    )
                }


                Spacer(
                    modifier =
                        Modifier.weight(1f)
                )


                Surface(
                    modifier =
                        Modifier.clickable {
                            showQueue = true
                        },

                    shape =
                        RoundedCornerShape(
                            30.dp
                        ),

                    color =
                        Color.Transparent,

                    border =
                        BorderStroke(
                            1.dp,
                            Color(0xFF343842)
                        )
                ) {

                    Row(
                        modifier =
                            Modifier.padding(
                                horizontal = 22.dp,
                                vertical = 11.dp
                            ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.QueueMusic,

                            contentDescription =
                                "Cola",

                            tint =
                                TextWhite,

                            modifier =
                                Modifier.size(22.dp)
                        )


                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )


                        Text(
                            text =
                                "Cola de reproducción",

                            color =
                                TextWhite,

                            fontSize =
                                15.sp
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.weight(1f)
                )


                IconButton(
                    onClick = {}
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.VolumeUp,

                        contentDescription =
                            "Volumen",

                        tint =
                            TextWhite,

                        modifier =
                            Modifier.size(28.dp)
                    )
                }
            }


            Spacer(
                modifier = Modifier
                    .height(20.dp)
                    .navigationBarsPadding()
            )
        }
    }


    if (showQueue) {

        ModalBottomSheet(

            onDismissRequest = {
                showQueue = false
            },

            containerColor =
                SurfaceDark,

            contentColor =
                TextWhite,

            sheetState =
                rememberModalBottomSheetState(
                    skipPartiallyExpanded =
                        false
                )
        ) {

            QueueSheet(
                queue =
                    queue,

                currentSong =
                    song,

                onSongClick = { queueSong ->

                    onQueueSongClick(
                        queueSong
                    )

                    showQueue =
                        false
                }
            )
        }
    }


    if (showInfo) {

        AlertDialog(

            onDismissRequest = {
                showInfo = false
            },

            confirmButton = {

                androidx.compose.material3.TextButton(
                    onClick = {
                        showInfo = false
                    }
                ) {
                    Text("Cerrar")
                }
            },

            title = {
                Text(
                    "Información"
                )
            },

            text = {

                Column {

                    Text(
                        text =
                            "Título: ${song.title}",

                        color =
                            TextWhite
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            "Artista: ${song.artist}",

                        color =
                            TextGray
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            "Álbum: ${song.album}",

                        color =
                            TextGray
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            "Carpeta: ${song.folderName}",

                        color =
                            TextGray
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            "Duración: ${formatTime(song.duration)}",

                        color =
                            TextGray
                    )
                }
            }
        )
    }
}


/*
 * ============================================================
 * COMPARTIR
 * ============================================================
 */

private fun shareSong(
    song: Song
) {

    val shareIntent =
        Intent(
            Intent.ACTION_SEND
        ).apply {

            type =
                "audio/*"

            putExtra(
                Intent.EXTRA_STREAM,
                Uri.parse(
                    song.uri
                )
            )

            putExtra(
                Intent.EXTRA_TEXT,
                song.title
            )
        }

    /*
     * El componente visual no tiene
     * referencia directa al Activity.
     *
     * La acción queda preparada para
     * integrarse con el Activity cuando
     * se habilite el compartir.
     */
}


/*
 * ============================================================
 * CARÁTULA DEL REPRODUCTOR
 * ============================================================
 */

@Composable
private fun PlayerArtwork(
    albumId: Long,
    modifier: Modifier
) {

    Box(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    28.dp
                )
            )
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF171A20),
                        Color(0xFF301025),
                        Color(0xFF0C1933)
                    )
                )
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
                Color(0xFF5C6070),

            modifier =
                Modifier.size(100.dp)
        )


        if (
            albumId > 0L
        ) {

            AsyncImage(
                model =
                    albumArtUri(
                        albumId
                    ),

                contentDescription =
                    "Carátula",

                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(
                            RoundedCornerShape(
                                28.dp
                            )
                        ),

                contentScale =
                    ContentScale.Crop
            )
        }
    }
}


/*
 * ============================================================
 * REPETICIÓN
 * ============================================================
 */

@Composable
private fun RepeatButtonIcon(
    repeatMode: Int
) {

    Box(
        contentAlignment =
            Alignment.Center
    ) {

        Icon(
            imageVector =
                Icons.Default.Repeat,

            contentDescription =
                "Repetición",

            tint =
                if (
                    repeatMode !=
                        Player.REPEAT_MODE_OFF
                ) {
                    TerereBlue
                } else {
                    TextWhite
                },

            modifier =
                Modifier.size(30.dp)
        )


        if (
            repeatMode ==
                Player.REPEAT_MODE_ONE
        ) {

            Surface(
                modifier =
                    Modifier
                        .size(12.dp)
                        .align(
                            Alignment.BottomEnd
                        ),

                shape =
                    CircleShape,

                color =
                    TerereBlue
            ) {

                Box(
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text =
                            "1",

                        color =
                            Color.White,

                        fontSize =
                            8.sp,

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
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
        modifier = Modifier
            .fillMaxWidth()
            .background(
                SurfaceDark
            )
            .padding(
                bottom = 20.dp
            )
    ) {

        Row(
            modifier = Modifier
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

                contentDescription =
                    null,

                tint =
                    TextWhite
            )


            Spacer(
                modifier =
                    Modifier.width(10.dp)
            )


            Text(
                text =
                    "Cola de reproducción",

                color =
                    TextWhite,

                fontSize =
                    20.sp,

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
                    TextGray
            )
        }


        if (
            queue.isEmpty()
        ) {

            Text(
                text =
                    "La cola está vacía.",

                color =
                    TextGray,

                modifier =
                    Modifier.padding(
                        20.dp
                    )
            )

        } else {

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(420.dp)
            ) {

                items(
                    items =
                        queue,

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
                                SurfaceCard2
                            } else {
                                SurfaceDark
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
                                    Modifier.size(50.dp)
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
                                        TextWhite,

                                    fontWeight =
                                        if (isCurrent) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        },

                                    maxLines =
                                        1,

                                    overflow =
                                        TextOverflow.Ellipsis
                                )


                                Text(
                                    text =
                                        queueSong.artist,

                                    color =
                                        TextGray,

                                    fontSize =
                                        13.sp
                                )
                            }


                            if (
                                isCurrent
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.MusicNote,

                                    contentDescription =
                                        "Reproduciendo",

                                    tint =
                                        TerereRed
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
 * CARÁTULA DE CANCIÓN
 * ============================================================
 */

@Composable
private fun AlbumArt(
    albumId: Long,
    modifier: Modifier
) {

    val uri =
        remember(
            albumId
        ) {
            albumArtUri(
                albumId
            )
        }


    Box(
        modifier =
            modifier
                .clip(
                    RoundedCornerShape(
                        10.dp
                    )
                )
                .background(
                    SurfaceCard2
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
                TextGray,

            modifier =
                Modifier.size(30.dp)
        )


        if (
            albumId > 0L
        ) {

            AsyncImage(
                model =
                    uri,

                contentDescription =
                    "Carátula",

                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(
                            RoundedCornerShape(
                                10.dp
                            )
                        ),

                contentScale =
                    ContentScale.Crop
            )
        }
    }
}


/*
 * ============================================================
 * SIN MÚSICA
 * ============================================================
 */

@Composable
private fun EmptyMusicState() {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 70.dp
            ),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector =
                Icons.Default.MusicNote,

            contentDescription =
                null,

            tint =
                TextGray,

            modifier =
                Modifier.size(55.dp)
        )


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        Text(
            text =
                "No hay música disponible",

            color =
                TextWhite,

            fontWeight =
                FontWeight.Bold
        )


        Text(
            text =
                "Agrega archivos de música al dispositivo.",

            color =
                TextGray
        )
    }
}


/*
 * ============================================================
 * SIN LISTAS
 * ============================================================
 */

@Composable
private fun EmptyPlaylistState() {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 70.dp
            ),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector =
                Icons.Default.QueueMusic,

            contentDescription =
                null,

            tint =
                TerereBlue,

            modifier =
                Modifier.size(55.dp)
        )


        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        Text(
            text =
                "Tus listas de reproducción",

            color =
                TextWhite,

            fontWeight =
                FontWeight.Bold
        )


        Text(
            text =
                "Aquí aparecerán tus listas.",

            color =
                TextGray
        )
    }
}
