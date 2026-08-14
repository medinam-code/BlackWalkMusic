package com.blackwalkmusic

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val player = MusicPlayer.getPlayer(this)

        /*
         * Abrimos MainActivity mediante su nombre completo.
         *
         * Esto evita que MusicService tenga que importar o
         * resolver directamente la clase MainActivity.
         */
        val activityComponent = ComponentName(
            packageName,
            "com.blackwalkmusic.MainActivity"
        )

        val sessionIntent = Intent().apply {
            component = activityComponent

            addFlags(
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        }

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            sessionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(
            this,
            player
        )
            .setSessionActivity(sessionActivity)
            .build()
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
