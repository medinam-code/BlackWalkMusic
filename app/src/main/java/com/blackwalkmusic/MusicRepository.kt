package com.blackwalkmusic

import android.content.ContentResolver
import android.provider.MediaStore

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: String,
    val albumId: Long
)

object MusicRepository {

    fun getSongs(contentResolver: ContentResolver): List<Song> {

        val songs = mutableListOf<Song>()

        val collection =
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val sortOrder =
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->

            val idColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)

            val titleColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)

            val artistColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

            val albumColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)

            val durationColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            val albumIdColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {

                val id = cursor.getLong(idColumn)

                songs.add(
                    Song(
                        id = id,
                        title = cursor.getString(titleColumn) ?: "Sin título",
                        artist = cursor.getString(artistColumn) ?: "Artista desconocido",
                        album = cursor.getString(albumColumn) ?: "Álbum desconocido",
                        duration = cursor.getLong(durationColumn),
                        uri = "${MediaStore.Audio.Media.EXTERNAL_CONTENT_URI}/$id",
                        albumId = cursor.getLong(albumIdColumn)
                    )
                )
            }
        }

        return songs
    }
}
