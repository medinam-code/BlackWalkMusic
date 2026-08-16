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
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.draw.clip
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


private val AppBackground = Color(0xFF020308)
private val SurfaceDark = Color(0xFF090B10)
private val SurfaceCard = Color(0xFF101217)
private val SurfaceCard2 = Color(0xFF15181F)
private val TextWhite = Color(0xFFF5F5F7)
private val TextGray = Color(0xFF9B9DA5)
private val TerereRed = Color(0xFFF12638)
private val TerereBlue = Color(0xFF1677FF)
private val NeonPurple = Color(0xFF9B38FF)


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


/*
 * ============================================================
 * CORRECCIÓN DEFINITIVA DE FOLDER
 * ============================================================
 */

private val Song.folder: String
    get() {
        return try {
            val path = Uri.parse(uri).path

            if (path.isNullOrBlank()) {
                "Desconocida"
            } else {
                val cleanPath = path.trimEnd('/')
                val lastSlash = cleanPath.lastIndexOf('/')

                if (lastSlash <= 0) {
                    "Desconocida"
                } else {
                    val parentPath =
                        cleanPath.substring(0, lastSlash)

                    parentPath
                        .substringAfterLast('/')
                        .ifBlank {
                            "Desconocida"
                        }
                }
            }
        } catch (_: Exception) {
            "Desconocida"
        }
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
