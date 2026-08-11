package com.blackwalkmusic

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val granted = permissions.values.any { it }

            if (granted) {
                loadMusic()
            }
        }

    private var songs by mutableStateOf<List<Song>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestMusicPermission()

        setContent {
            BlackWalkMusicScreen(songs)
        }
    }

    private fun requestMusicPermission() {

        val permissions = if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }

        val needsPermission = permissions.any {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }

        if (needsPermission) {
            permissionLauncher.launch(permissions)
        } else {
            loadMusic()
        }
    }

    private fun loadMusic() {
        songs = MusicRepository.getSongs(contentResolver)
    }
}

@Composable
fun BlackWalkMusicScreen(
    songs: List<Song>
) {

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color.Black,
            surface = Color(0xFF101010),
            primary = Color.White
        )
    ) {

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {

                Text(
                    text = "BLACKWALK",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "MUSIC",
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(25.dp))

                Text(
                    text = "Canciones",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (songs.isEmpty()) {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(70.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "No encontramos canciones",
                                color = Color.White
                            )

                            Text(
                                text = "Agrega música al teléfono y vuelve a abrir la aplicación",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }

                } else {

                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {

                        items(
                            items = songs,
                            key = { it.id }
                        ) { song ->

                            SongItem(song)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongItem(song: Song) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(55.dp)
                .background(Color(0xFF202020)),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.Gray
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = song.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = song.artist,
                color = Color.Gray,
                fontSize = 13.sp
            )
        }
    }
}
