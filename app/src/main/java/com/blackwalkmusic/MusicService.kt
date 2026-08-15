package com.blackwalkmusic

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class MusicService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        /*
         * ========================================================
         * EXOPLAYER
         * ========================================================
         *
         * Este es el ÚNICO ExoPlayer de la aplicación.
         *
         * No creamos otro reproductor en MainActivity.
         */

        val audioAttributes =
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

        player =
            ExoPlayer.Builder(this)
                .setAudioAttributes(
                    audioAttributes,
                    true
                )
                .setHandleAudioBecomingNoisy(true)
                .build()

        /*
         * ========================================================
         * MEDIA SESSION
         * ========================================================
         */

        val sessionIntent =
            Intent(
                this,
                MainActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        val sessionActivity =
            PendingIntent.getActivity(
                this,
                0,
                sessionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        mediaSession =
            MediaSession.Builder(
                this,
                player!!
            )
                .setSessionActivity(
                    sessionActivity
                )
                .build()
    }

    /*
     * ============================================================
     * MEDIA CONTROLLER
     * ============================================================
     */

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? {

        return mediaSession
    }

    /*
     * ============================================================
     * DESTROY
     * ============================================================
     */

    override fun onDestroy() {

        /*
         * Primero liberamos MediaSession.
         */
        mediaSession?.release()
        mediaSession = null

        /*
         * Después liberamos ExoPlayer.
         */
        player?.release()
        player = null

        super.onDestroy()
    }
}
