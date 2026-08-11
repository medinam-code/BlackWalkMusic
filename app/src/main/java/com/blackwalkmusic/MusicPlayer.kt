package com.blackwalkmusic

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer

object MusicPlayer {

    private var player: ExoPlayer? = null

    fun getPlayer(context: Context): ExoPlayer {

        if (player == null) {
            player = ExoPlayer.Builder(context.applicationContext)
                .build()
        }

        return player!!
    }

    fun release() {
        player?.release()
        player = null
    }
}
