package com.blackwalkmusic

import android.content.Context

object FavoritesManager {

    private const val PREFS_NAME =
        "terere_music_preferences"

    private const val FAVORITES_KEY =
        "favorite_song_ids"

    private fun preferences(
        context: Context
    ) =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun getFavorites(
        context: Context
    ): Set<Long> {

        val stored =
            preferences(context)
                .getStringSet(
                    FAVORITES_KEY,
                    emptySet()
                )
                ?: emptySet()

        if (stored.isEmpty()) {
            return emptySet()
        }

        return stored
            .mapNotNull { value ->
                value.toLongOrNull()
            }
            .toSet()
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

        val favorites =
            getFavorites(context)
                .toMutableSet()

        val isNowFavorite =
            if (favorites.contains(songId)) {

                favorites.remove(songId)
                false

            } else {

                favorites.add(songId)
                true
            }

        saveFavorites(
            context,
            favorites
        )

        return isNowFavorite
    }

    private fun saveFavorites(
        context: Context,
        favorites: Set<Long>
    ) {

        val values =
            favorites
                .map(Long::toString)
                .toSet()

        preferences(context)
            .edit()
            .putStringSet(
                FAVORITES_KEY,
                values
            )
            .apply()
    }

    fun clearFavorites(
        context: Context
    ) {

        preferences(context)
            .edit()
            .remove(FAVORITES_KEY)
            .apply()
    }
}
