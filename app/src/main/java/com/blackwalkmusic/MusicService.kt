package com.blackwalkmusic

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val player = MusicPlayer.getPlayer(applicationContext)

        mediaSession = MediaSession.Builder(
            this,
            player
        ).build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {

        mediaSession?.release()
        mediaSession = null

        MusicPlayer.release()

        super.onDestroy()
    }
}
