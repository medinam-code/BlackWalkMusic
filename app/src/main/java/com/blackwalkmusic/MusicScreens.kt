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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.media3.common.Player
import coil.compose.AsyncImage
import kotlin.math.max

/*
 * ============================================================
 * COLORES — TERERÉ MUSIC PY
 * ============================================================
 */

private val TerereBackground = Color(0xFF07090D)
private val TerereSurface = Color(0xFF0E1117)
private val TerereSurface2 = Color(0xFF151922)
private val TerereSurface3 = Color(0xFF1D222C)

private val TerereWhite = Color(0xFFF5F7FA)
private val TerereGray = Color(0xFFA6ABB5)

private val TerereRed = Color(0xFFE53935)
private val TerereBlue = Color(0xFF1976D2)

private val TerereGreen = Color(0xFF63B96C)

private fun albumArtUri(albumId: Long): Uri {
    return Uri.parse(
        "content://media/external/audio/albumart/$albumId"
    )
}

private fun formatTime(milliseconds: Long): String {

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

    var selectedTab by remember {
        mutableIntStateOf(0)
    }

    var showSortMenu by remember {
        mutableStateOf(false)
    }

    var sortMode by remember {
        mutableIntStateOf(0)
    }

    /*
     * ========================================================
     * FILTRADO Y ORDEN
     * ========================================================
     */

    val filteredSongs = remember(
        songs,
        searchText,
        selectedTab,
        favoriteIds,
        sortMode
    ) {

        var result =
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

        /*
         * Pestañas.
         *
         * 0 = Canciones
         * 1 = Artistas
         * 2 = Álbumes
         * 3 = Listas
         *
         * Para Artistas y Álbumes mostramos la biblioteca
         * agrupada visualmente más adelante.
         *
         * Listas utiliza Favoritos como primera lista local.
         */

        if (selectedTab == 3) {

            result =
                result.filter {
                    favoriteIds.contains(
                        it.id
                    )
                }
        }

        when (sortMode) {

            0 -> {
                result.sortedBy {
                    it.title.lowercase()
                }
            }

            1 -> {
                result.sortedBy {
                    it.artist.lowercase()
                }
            }

            2 -> {
                result.sortedBy {
                    it.album.lowercase()
                }
            }

            3 -> {
                result.sortedByDescending {
                    it.dateAdded
                }
            }

            4 -> {
                result.sortedByDescending {
                    it.duration
                }
            }

            else -> result
        }
    }

    /*
     * ========================================================
     * LIST STATE
     * ========================================================
     */

    val listState =
        rememberLazyListState()

    /*
     * ========================================================
     * DESPLAZAMIENTO AUTOMÁTICO
     *
     * Cuando Media3 cambia realmente de canción,
     * currentSong cambia.
     *
     * Buscamos esa canción dentro de la lista visible y
     * desplazamos la biblioteca hasta ella.
     * ========================================================
     */

    LaunchedEffect(
        currentSong?.id,
        selectedTab,
        searchText,
        sortMode
    ) {

        val currentId =
            currentSong?.id
                ?: return@LaunchedEffect

        val index =
            filteredSongs.indexOfFirst {
                it.id == currentId
            }

        if (index >= 0) {

            /*
             * +1 porque el primer elemento de LazyColumn
             * es el encabezado de la biblioteca.
             */

            listState.animateScrollToItem(
                index + 1
            )
        }
    }

    Surface(
        modifier =
            Modifier.fillMaxSize(),

        color =
            TerereBackground
    ) {

        Scaffold(

            modifier =
                Modifier.fillMaxSize(),

            containerColor =
                TerereBackground,

            contentColor =
                TerereWhite,

            topBar = {

                if (searchVisible) {

                    SearchBar(
                        searchText =
                            searchText,

                        onSearchTextChange = {
                            searchText = it
                        },

                        onClose = {

                            searchVisible =
                                false

                            searchText =
                                ""
                        }
                    )

                } else {

                    TerereTopBar(

                        songCount =
                            songs.size,

                        onSearch = {
                            searchVisible = true
                        },

                        onSort = {
                            showSortMenu = true
                        },

                        showSortMenu =
                            showSortMenu,

                        onDismissSort = {
                            showSortMenu = false
                        },

                        onSortSelected = { mode ->

                            sortMode = mode

                            showSortMenu = false
                        }
                    )
                }
            },

            bottomBar = {

                if (currentSong != null) {

                    MiniPlayer(

                        song =
                            currentSong,

                        isPlaying =
                            isPlaying,

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

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            paddingValues
                        )
                        .background(
                            TerereBackground
                        )
            ) {

                /*
                 * ====================================================
                 * CABECERA DE IDENTIDAD
                 * ====================================================
                 */

                TerereHeader(
                    songCount =
                        songs.size
                )

                /*
                 * ====================================================
                 * PESTAÑAS
                 * ====================================================
                 */

                LibraryTabs(
                    selectedTab =
                        selectedTab,

                    onTabSelected = {
                        selectedTab = it
                    }
                )

                /*
                 * ====================================================
                 * CONTENIDO
                 * ====================================================
                 */

                if (filteredSongs.isEmpty()) {

                    EmptyMusicState(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .weight(1f)
                    )

                } else {

                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(
                                    TerereBackground
                                ),

                        state =
                            listState
                    ) {

                        /*
                         * =================================================
                         * BOTÓN ALEATORIO
                         * =================================================
                         */

                        item(
                            key = "shuffle_header"
                        ) {

                            ShuffleHeader(
                                onClick =
                                    onShuffle
                            )
                        }

                        /*
                         * =================================================
                         * LISTA
                         * =================================================
                         */

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

                        item(
                            key = "bottom_space"
                        ) {

                            Spacer(
                                modifier =
                                    Modifier
                                        .height(
                                            92.dp
                                        )
                            )
                        }
                    }
                }
            }
        }
    }
}

/*
 * ============================================================
 * CABECERA SUPERIOR
 * ============================================================
 */

@Composable
private fun TerereTopBar(
    songCount: Int,
    onSearch: () -> Unit,
    onSort: () -> Unit,
    showSortMenu: Boolean,
    onDismissSort: () -> Unit,
    onSortSelected: (Int) -> Unit
) {

    TopAppBar(

        title = {

            Column {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            "TERERÉ",

                        color =
                            TerereRed,

                        fontSize =
                            20.sp,

                        fontWeight =
                            FontWeight.Black,

                        letterSpacing =
                            0.5.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                5.dp
                            )
                    )

                    Text(
                        text =
                            "MUSIC",

                        color =
                            TerereWhite,

                        fontSize =
                            20.sp,

                        fontWeight =
                            FontWeight.Black
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                5.dp
                            )
                    )

                    Text(
                        text =
                            "PY",

                        color =
                            TerereBlue,

                        fontSize =
                            20.sp,

                        fontWeight =
                            FontWeight.Black
                    )
                }

                Text(
                    text =
                        "$songCount canciones",

                    color =
                        TerereGray,

                    fontSize =
                        11.sp
                )
            }
        },

        actions = {

            IconButton(
                onClick =
                    onSearch
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Search,

                    contentDescription =
                        "Buscar",

                    tint =
                        TerereWhite
                )
            }

            Box {

                IconButton(
                    onClick =
                        onSort
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.MoreVert,

                        contentDescription =
                            "Más opciones",

                        tint =
                            TerereWhite
                    )
                }

                DropdownMenu(

                    expanded =
                        showSortMenu,

                    onDismissRequest =
                        onDismissSort
                ) {

                    DropdownMenuItem(

                        text = {
                            Text(
                                "Título"
                            )
                        },

                        onClick = {
                            onSortSelected(
                                0
                            )
                        }
                    )

                    DropdownMenuItem(

                        text = {
                            Text(
                                "Artista"
                            )
                        },

                        onClick = {
                            onSortSelected(
                                1
                            )
                        }
                    )

                    DropdownMenuItem(

                        text = {
                            Text(
                                "Álbum"
                            )
                        },

                        onClick = {
                            onSortSelected(
                                2
                            )
                        }
                    )

                    DropdownMenuItem(

                        text = {
                            Text(
                                "Más recientes"
                            )
                        },

                        onClick = {
                            onSortSelected(
                                3
                            )
                        }
                    )

                    DropdownMenuItem(

                        text = {
                            Text(
                                "Mayor duración"
                            )
                        },

                        onClick = {
                            onSortSelected(
                                4
                            )
                        }
                    )
                }
            }
        },

        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor =
                    TerereBackground,

                titleContentColor =
                    TerereWhite,

                actionIconContentColor =
                    TerereWhite
            )
    )
}

/*
 * ============================================================
 * BUSCADOR
 * ============================================================
 */

@Composable
private fun SearchBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onClose: () -> Unit
) {

    Surface(
        modifier =
            Modifier.fillMaxWidth(),

        color =
            TerereSurface
    ) {

        Row(
            modifier =
                Modifier
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
                        Icons.Default.ArrowBack,

                    contentDescription =
                        "Volver",

                    tint =
                        TerereWhite
                )
            }

            OutlinedTextField(

                value =
                    searchText,

                onValueChange =
                    onSearchTextChange,

                modifier =
                    Modifier.weight(1f),

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

                placeholder = {

                    Text(
                        text =
                            "Buscar canción, artista o álbum"
                    )
                }
            )
        }
    }
}

/*
 * ============================================================
 * IDENTIDAD PRINCIPAL
 * ============================================================
 */

@Composable
private fun TerereHeader(
    songCount: Int
) {

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 8.dp
                ),

        shape =
            RoundedCornerShape(
                20.dp
            ),

        color =
            TerereSurface
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 14.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            /*
             * LOGO
             *
             * Por ahora lo generamos directamente con Compose.
             * Así no dependemos de una imagen externa y el proyecto
             * compila aunque todavía no hayas agregado el logo final.
             */

            Surface(

                modifier =
                    Modifier.size(
                        62.dp
                    ),

                shape =
                    CircleShape,

                color =
                    TerereSurface3()
            ) {

                Box(
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.LocalCafe,

                        contentDescription =
                            "Tereré Music PY",

                        tint =
                            TerereGreen,

                        modifier =
                            Modifier.size(
                                38.dp
                            )
                    )
                }
            }

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

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            "TERERÉ",

                        color =
                            TerereRed,

                        fontSize =
                            18.sp,

                        fontWeight =
                            FontWeight.Black
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                4.dp
                            )
                    )

                    Text(
                        text =
                            "MUSIC",

                        color =
                            TerereWhite,

                        fontSize =
                            18.sp,

                        fontWeight =
                            FontWeight.Black
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                4.dp
                            )
                    )

                    Text(
                        text =
                            "PY",

                        color =
                            TerereBlue,

                        fontSize =
                            18.sp,

                        fontWeight =
                            FontWeight.Black
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            3.dp
                        )
                )

                Text(
                    text =
                        "Tu música. Tu ritmo. Paraguay.",

                    color =
                        TerereGray,

                    fontSize =
                        12.sp
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            2.dp
                        )
                )

                Text(
                    text =
                        "$songCount canciones disponibles",

                    color =
                        TerereGray,

                    fontSize =
                        10.sp
                )
            }
        }
    }
}

@Composable
private fun TerereSurface3(): Color {
    return Color(0xFF20262F)
}

/*
 * ============================================================
 * PESTAÑAS
 * ============================================================
 */

@Composable
private fun LibraryTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {

    val tabs =
        listOf(
            "Canciones",
            "Artistas",
            "Álbumes",
            "Listas"
        )

    TabRow(

        selectedTabIndex =
            selectedTab,

        containerColor =
            TerereBackground,

        contentColor =
            TerereWhite,

        divider = {}
    ) {

        tabs.forEachIndexed { index, title ->

            Tab(

                selected =
                    selectedTab == index,

                onClick = {
                    onTabSelected(
                        index
                    )
                },

                text = {

                    Text(
                        text =
                            title,

                        fontSize =
                            12.sp,

                        fontWeight =
                            if (
                                selectedTab ==
                                    index
                            ) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                    )
                },

                icon = {

                    when (index) {

                        0 -> {
                            Icon(
                                imageVector =
                                    Icons.Default.MusicNote,

                                contentDescription =
                                    null,

                                modifier =
                                    Modifier.size(
                                        17.dp
                                    )
                            )
                        }

                        1 -> {
                            Icon(
                                imageVector =
                                    Icons.Default.Person,

                                contentDescription =
                                    null,

                                modifier =
                                    Modifier.size(
                                        17.dp
                                    )
                            )
                        }

                        2 -> {
                            Icon(
                                imageVector =
                                    Icons.Default.Album,

                                contentDescription =
                                    null,

                                modifier =
                                    Modifier.size(
                                        17.dp
                                    )
                            )
                        }

                        3 -> {
                            Icon(
                                imageVector =
                                    Icons.Default.List,

                                contentDescription =
                                    null,

                                modifier =
                                    Modifier.size(
                                        17.dp
                                    )
                            )
                        }
                    }
                }
            )
        }
    }
}

/*
 * ============================================================
 * BOTÓN ALEATORIO
 * ============================================================
 */

@Composable
private fun ShuffleHeader(
    onClick: () -> Unit
) {

    Surface(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 10.dp
                )
                .clickable {
                    onClick()
                },

        shape =
            RoundedCornerShape(
                14.dp
            ),

        color =
            TerereSurface2
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 13.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Surface(

                modifier =
                    Modifier.size(
                        40.dp
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

                    Icon(
                        imageVector =
                            Icons.Default.Shuffle,

                        contentDescription =
                            "Aleatorio",

                        tint =
                            Color.White
                    )
                }
            }

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
                        "Aleatorio",

                    color =
                        TerereWhite,

                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        "Reproducir la biblioteca al azar",

                    color =
                        TerereGray,

                    fontSize =
                        11.sp
                )
            }

            Icon(
                imageVector =
                    Icons.Default.PlayArrow,

                contentDescription =
                    null,

                tint =
                    TerereWhite
            )
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
                TerereSurface3
            } else {
                TerereBackground
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

            /*
             * INDICADOR ACTUAL
             */

            Box(
                modifier =
                    Modifier.width(
                        3.dp
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                if (isCurrent) {

                    Box(
                        modifier =
                            Modifier
                                .width(3.dp)
                                .height(42.dp)
                                .clip(
                                    RoundedCornerShape(
                                        3.dp
                                    )
                                )
                                .background(
                                    if (isPlaying) {
                                        TerereRed
                                    } else {
                                        TerereBlue
                                    }
                                )
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.width(
                        8.dp
                    )
            )

            AlbumArt(

                albumId =
                    song.albumId,

                modifier =
                    Modifier.size(
                        56.dp
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
                        if (isCurrent) {
                            TerereWhite
                        } else {
                            TerereWhite
                        },

                    fontWeight =
                        if (
                            isCurrent
                        ) {
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
                        if (isCurrent) {
                            TerereBlue
                        } else {
                            TerereGray
                        },

                    fontSize =
                        12.sp,

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis
                )

                Text(

                    text =
                        song.album,

                    color =
                        TerereGray,

                    fontSize =
                        10.sp,

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis
                )
            }

            if (isPlaying) {

                Icon(

                    imageVector =
                        Icons.Default.MusicNote,

                    contentDescription =
                        "Reproduciendo",

                    tint =
                        TerereRed,

                    modifier =
                        Modifier.size(
                            18.dp
                        )
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
                            TerereGray
                        }
                )
            }

            Text(

                text =
                    formatTime(
                        song.duration
                    ),

                color =
                    TerereGray,

                fontSize =
                    10.sp
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

        color =
            TerereSurface,

        tonalElevation =
            10.dp
    ) {

        Column {

            /*
             * Barra visual.
             *
             * Posteriormente la conectaremos directamente con
             * currentPosition/duration de MainActivity.
             */

            LinearProgressIndicator(

                progress = {
                    0f
                },

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            2.dp
                        ),

                color =
                    TerereRed,

                trackColor =
                    TerereSurface3
            )

            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(
                            horizontal = 8.dp,
                            vertical = 5.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                AlbumArt(

                    albumId =
                        song.albumId,

                    modifier =
                        Modifier.size(
                            50.dp
                        )
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            10.dp
                        )
                )

                Column(
                    modifier =
                        Modifier.weight(
                            1f
                        )
                ) {

                    Text(

                        text =
                            song.title,

                        color =
                            TerereWhite,

                        fontWeight =
                            FontWeight.Bold,

                        fontSize =
                            13.sp,

                        maxLines =
                            1,

                        overflow =
                            TextOverflow.Ellipsis
                    )

                    Text(

                        text =
                            song.artist,

                        color =
                            TerereGray,

                        fontSize =
                            10.sp,

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
                            TerereWhite,

                        modifier =
                            Modifier.size(
                                23.dp
                            )
                    )
                }

                Surface(

                    modifier =
                        Modifier
                            .size(
                                42.dp
                            )
                            .clickable {
                                onPlayPause()
                            },

                    shape =
                        CircleShape,

                    color =
                        TerereRed
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
                                Color.White,

                            modifier =
                                Modifier.size(
                                    24.dp
                                )
                        )
                    }
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
                            TerereWhite,

                        modifier =
                            Modifier.size(
                                23.dp
                            )
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

    Surface(

        modifier =
            Modifier.fillMaxSize(),

        color =
            TerereBackground
    ) {

        Scaffold(

            modifier =
                Modifier.fillMaxSize(),

            containerColor =
                TerereBackground,

            contentColor =
                TerereWhite,

            topBar = {

                TopAppBar(

                    title = {

                        Column {

                            Text(
                                text =
                                    "TERERÉ MUSIC PY",

                                color =
                                    TerereWhite,

                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                text =
                                    "Reproduciendo",

                                color =
                                    TerereGray,

                                fontSize =
                                    10.sp
                            )
                        }
                    },

                    navigationIcon = {

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
                                    TerereWhite
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
                                    "Cola",

                                tint =
                                    TerereWhite
                            )
                        }
                    },

                    colors =
                        TopAppBarDefaults
                            .topAppBarColors(
                                containerColor =
                                    TerereBackground,

                                titleContentColor =
                                    TerereWhite,

                                navigationIconContentColor =
                                    TerereWhite,

                                actionIconContentColor =
                                    TerereWhite
                            )
                )
            }

        ) { paddingValues ->

            Column(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            TerereBackground
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
                        Modifier.height(
                            12.dp
                        )
                )

                AlbumArt(

                    albumId =
                        song.albumId,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                330.dp
                            )
                            .clip(
                                RoundedCornerShape(
                                    24.dp
                                )
                            )
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            20.dp
                        )
                )

                Text(

                    text =
                        song.title,

                    color =
                        TerereWhite,

                    fontSize =
                        24.sp,

                    fontWeight =
                        FontWeight.Bold,

                    maxLines =
                        2,

                    overflow =
                        TextOverflow.Ellipsis
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )

                Text(

                    text =
                        song.artist,

                    color =
                        TerereBlue,

                    fontSize =
                        15.sp,

                    fontWeight =
                        FontWeight.SemiBold,

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis
                )

                Text(

                    text =
                        song.album,

                    color =
                        TerereGray,

                    fontSize =
                        12.sp,

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            13.dp
                        )
                )

                /*
                 * =================================================
                 * PROGRESO
                 * =================================================
                 */

                androidx.compose.material3.Slider(

                    value =
                        sliderPosition.coerceIn(
                            0f,
                            safeDuration.toFloat()
                        ),

                    onValueChange = {
                        sliderPosition =
                            it
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
                            TerereGray,

                        fontSize =
                            10.sp
                    )

                    Text(

                        text =
                            formatTime(
                                duration
                            ),

                        color =
                            TerereGray,

                        fontSize =
                            10.sp
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            10.dp
                        )
                )

                /*
                 * =================================================
                 * CONTROLES
                 * =================================================
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
                                if (
                                    shuffleEnabled
                                ) {
                                    TerereBlue
                                } else {
                                    TerereWhite
                                }
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
                                TerereWhite,

                            modifier =
                                Modifier.size(
                                    38.dp
                                )
                        )
                    }

                    Surface(

                        modifier =
                            Modifier
                                .size(
                                    72.dp
                                )
                                .clickable {
                                    onPlayPause()
                                },

                        shape =
                            CircleShape,

                        color =
                            TerereRed
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
                                    Color.White,

                                modifier =
                                    Modifier.size(
                                        40.dp
                                    )
                            )
                        }
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
                                TerereWhite,

                            modifier =
                                Modifier.size(
                                    38.dp
                                )
                        )
                    }

                    IconButton(
                        onClick =
                            onRepeat
                    ) {

                        Icon(

                            imageVector =
                                if (
                                    repeatMode ==
                                        Player.REPEAT_MODE_ONE
                                ) {
                                    androidx.compose.material.icons
                                        .filled.RepeatOne
                                } else {
                                    androidx.compose.material.icons
                                        .filled.Repeat
                                },

                            contentDescription =
                                "Repetición",

                            tint =
                                if (
                                    repeatMode !=
                                        Player.REPEAT_MODE_OFF
                                ) {
                                    TerereBlue
                                } else {
                                    TerereWhite
                                }
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
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
                                TerereWhite
                            },

                        modifier =
                            Modifier.size(
                                30.dp
                            )
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
                TerereSurface,

            contentColor =
                TerereWhite,

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
                    TerereSurface
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
                        vertical = 12.dp
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
                    TerereRed
            )

            Spacer(
                modifier =
                    Modifier.width(
                        10.dp
                    )
            )

            Text(

                text =
                    "Cola de reproducción",

                color =
                    TerereWhite,

                fontSize =
                    20.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.width(
                        8.dp
                    )
            )

            Text(

                text =
                    "${queue.size}",

                color =
                    TerereGray,

                fontSize =
                    12.sp
            )
        }

        if (queue.isEmpty()) {

            Text(

                text =
                    "La cola está vacía.",

                color =
                    TerereGray,

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
                        .height(
                            420.dp
                        )
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
                                TerereSurface3
                            } else {
                                TerereSurface
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
                                    Modifier.size(
                                        48.dp
                                    )
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(
                                        10.dp
                                    )
                            )

                            Column(

                                modifier =
                                    Modifier.weight(
                                        1f
                                    )
                            ) {

                                Text(

                                    text =
                                        queueSong.title,

                                    color =
                                        TerereWhite,

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
                                        TerereGray,

                                    fontSize =
                                        11.sp,

                                    maxLines =
                                        1,

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
            albumArtUri(
                albumId
            )
        }

    Box(

        modifier =
            modifier
                .clip(
                    RoundedCornerShape(
                        12.dp
                    )
                )
                .background(
                    TerereSurface3
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
                TerereGray,

            modifier =
                Modifier.size(
                    34.dp
                )
        )

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
                            12.dp
                        )
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

        modifier =
            modifier,

        contentAlignment =
            Alignment.Center
    ) {

        Column(

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Center
        ) {

            Surface(

                modifier =
                    Modifier.size(
                        82.dp
                    ),

                shape =
                    CircleShape,

                color =
                    TerereSurface2
            ) {

                Box(
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(

                        imageVector =
                            Icons.Default.MusicNote,

                        contentDescription =
                            null,

                        tint =
                            TerereRed,

                        modifier =
                            Modifier.size(
                                48.dp
                            )
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )

            Text(

                text =
                    "No hay música disponible",

                color =
                    TerereWhite,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(
                        5.dp
                    )
            )

            Text(

                text =
                    "Agrega archivos de música al dispositivo.",

                color =
                    TerereGray,

                fontSize =
                    12.sp
            )
        }
    }
}
