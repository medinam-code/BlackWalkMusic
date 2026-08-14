package com.blackwalkmusic

import android.content.Context

object FavoritesManager {

    private const val PREFS_NAME = "terere_music_preferences"
    private const val FAVORITES_KEY = "favorite_song_ids"

    private fun preferences(context: Context) =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun getFavorites(context: Context): Set<Long> {

        return preferences(context)
            .getStringSet(
                FAVORITES_KEY,
                emptySet()
            )
            ?.mapNotNull {
                it.toLongOrNull()
            }
            ?.toSet()
            ?: emptySet()
    }

    fun isFavorite(
        context: Context,
        songId: Long
    ): Boolean {

        return getFavorites(context)
            .contains(songId)
    }

    fun toggleFavorite(
        context: Context,
        songId: Long
    ): Boolean {

        val current =
            getFavorites(context).toMutableSet()

        val nowFavorite: Boolean

        if (current.contains(songId)) {

            current.remove(songId)
            nowFavorite = false

        } else {

            current.add(songId)
            nowFavorite = true
        }

        preferences(context)
            .edit()
            .putStringSet(
                FAVORITES_KEY,
                current.map {
                    it.toString()
                }.toSet()
            )
            .apply()

        return nowFavorite
    }
}
