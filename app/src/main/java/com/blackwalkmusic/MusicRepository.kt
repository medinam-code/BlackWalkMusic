package com.blackwalkmusic

import android.content.ContentResolver
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import java.io.File

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: String,
    val albumId: Long,
    val dateAdded: Long,

    // Carpeta donde está almacenada la canción
    val folderPath: String = "",
    val folderName: String = ""
)

object MusicRepository {

    /**
     * Obtiene todas las canciones disponibles
     * en el almacenamiento del dispositivo.
     *
     * Se utiliza MediaStore para mantener compatibilidad
     * con las diferentes versiones de Android.
     */
    fun getSongs(
        contentResolver: ContentResolver
    ): List<Song> {

        val songs =
            mutableListOf<Song>()

        val collection =
            Media.EXTERNAL_CONTENT_URI

        val projection =
            arrayOf(
                Media._ID,
                Media.TITLE,
                Media.ARTIST,
                Media.ALBUM,
                Media.DURATION,
                Media.ALBUM_ID,
                Media.DATE_ADDED,
                Media.DATA
            )

        val selection =
            "${Media.IS_MUSIC} != 0"

        val sortOrder =
            "${Media.TITLE} COLLATE NOCASE ASC"

        contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->

            val idColumn =
                cursor.getColumnIndex(
                    Media._ID
                )

            val titleColumn =
                cursor.getColumnIndex(
                    Media.TITLE
                )

            val artistColumn =
                cursor.getColumnIndex(
                    Media.ARTIST
                )

            val albumColumn =
                cursor.getColumnIndex(
                    Media.ALBUM
                )

            val durationColumn =
                cursor.getColumnIndex(
                    Media.DURATION
                )

            val albumIdColumn =
                cursor.getColumnIndex(
                    Media.ALBUM_ID
                )

            val dateAddedColumn =
                cursor.getColumnIndex(
                    Media.DATE_ADDED
                )

            val dataColumn =
                cursor.getColumnIndex(
                    Media.DATA
                )

            while (
                cursor.moveToNext()
            ) {

                val id =
                    if (idColumn >= 0) {
                        cursor.getLong(
                            idColumn
                        )
                    } else {
                        continue
                    }

                val title =
                    if (titleColumn >= 0) {
                        cursor.getString(
                            titleColumn
                        )
                    } else {
                        null
                    }

                val artist =
                    if (artistColumn >= 0) {
                        cursor.getString(
                            artistColumn
                        )
                    } else {
                        null
                    }

                val album =
                    if (albumColumn >= 0) {
                        cursor.getString(
                            albumColumn
                        )
                    } else {
                        null
                    }

                val duration =
                    if (durationColumn >= 0) {
                        cursor.getLong(
                            durationColumn
                        )
                    } else {
                        0L
                    }

                val albumId =
                    if (albumIdColumn >= 0) {
                        cursor.getLong(
                            albumIdColumn
                        )
                    } else {
                        0L
                    }

                val dateAdded =
                    if (dateAddedColumn >= 0) {
                        cursor.getLong(
                            dateAddedColumn
                        )
                    } else {
                        0L
                    }

                val filePath =
                    if (dataColumn >= 0) {
                        cursor.getString(
                            dataColumn
                        ) ?: ""
                    } else {
                        ""
                    }

                val folderPath =
                    getFolderPath(
                        filePath
                    )

                val folderName =
                    getFolderName(
                        folderPath
                    )

                val uri =
                    "${Media.EXTERNAL_CONTENT_URI}/$id"

                songs.add(
                    Song(

                        id =
                            id,

                        title =
                            title
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: "Sin título",

                        artist =
                            artist
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: "Artista desconocido",

                        album =
                            album
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: "Álbum desconocido",

                        duration =
                            duration.coerceAtLeast(
                                0L
                            ),

                        uri =
                            uri,

                        albumId =
                            albumId,

                        dateAdded =
                            dateAdded,

                        folderPath =
                            folderPath,

                        folderName =
                            folderName
                    )
                )
            }
        }

        return songs
    }

    /**
     * Obtiene la ruta de la carpeta.
     */
    private fun getFolderPath(
        filePath: String
    ): String {

        if (
            filePath.isBlank()
        ) {
            return ""
        }

        return try {

            File(
                filePath
            )
                .parent
                ?: ""

        } catch (
            _: Exception
        ) {

            ""
        }
    }

    /**
     * Obtiene solamente el nombre de la carpeta.
     */
    private fun getFolderName(
        folderPath: String
    ): String {

        if (
            folderPath.isBlank()
        ) {
            return "Desconocida"
        }

        return try {

            File(
                folderPath
            )
                .name
                .takeIf {
                    it.isNotBlank()
                }
                ?: "Almacenamiento"

        } catch (
            _: Exception
        ) {

            "Desconocida"
        }
    }

    /**
     * Devuelve las carpetas existentes,
     * sin repetirlas.
     */
    fun getFolders(
        songs: List<Song>
    ): List<String> {

        return songs
            .map {
                it.folderPath
            }
            .filter {
                it.isNotBlank()
            }
            .distinct()
            .sortedBy {
                it.lowercase()
            }
    }

    /**
     * Obtiene las canciones pertenecientes
     * a una determinada carpeta.
     */
    fun getSongsFromFolder(
        songs: List<Song>,
        folderPath: String
    ): List<Song> {

        return songs
            .filter {
                it.folderPath == folderPath
            }
            .sortedBy {
                it.title.lowercase()
            }
    }
}
