package com.blackwalkmusic

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var controllerFuture: ListenableFuture<MediaController>

    private var controller: MediaController? = null

    private var songs by mutableStateOf<List<Song>>(emptyList())

    /*
     * ESTA ES LA ÚNICA FUENTE DE VERDAD
     * PARA LA CANCIÓN QUE ESTÁ SONANDO.
     */
    private var currentSong by mutableStateOf<Song?>(null)

    /*
     * Cola visual sincronizada con Media3.
     *
     * IMPORTANTE:
     * NO existe getCurrentQueue().
     * Así evitamos el conflicto JVM que apareció antes.
     */
    private var currentQueue by mutableStateOf<List<Song>>(emptyList())

    private var isPlaying by mutableStateOf(false)

    private var currentPosition by mutableLongStateOf(0L)

    private var duration by mutableLongStateOf(0L)

    private var repeatMode by mutableIntStateOf(
        Player.REPEAT_MODE_OFF
    )

    private var shuffleEnabled by mutableStateOf(false)

    private var favoriteIds by mutableStateOf<Set<Long>>(emptySet())

    /*
     * ============================================================
     * PERMISOS
     * ============================================================
     */

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            loadMusic()
        }

    /*
     * ============================================================
     * PLAYER LISTENER
     * ============================================================
     *
     * Media3 informa aquí cuando realmente cambia el elemento
     * que está siendo reproducido.
     *
     * Esto es especialmente importante con Shuffle.
     */
    private val playerListener =
        object : Player.Listener {

            override fun onIsPlayingChanged(
                isPlayingNow: Boolean
            ) {
                isPlaying = isPlayingNow
            }

            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int
            ) {
                /*
                 * Este es el punto principal.
                 *
                 * La canción actual se obtiene directamente
                 * del MediaController.
                 */
                syncPlayerState()
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                /*
                 * Se ejecuta al hacer seek, siguiente,
                 * anterior, transición automática, etc.
                 */
                syncPlayerState()
            }

            override fun onTimelineChanged(
                timeline: androidx.media3.common.Timeline,
                reason: Int
            ) {
                syncPlayerState()
            }

            override fun onPlaybackStateChanged(
                playbackState: Int
            ) {
                updateProgress()
            }

            override fun onRepeatModeChanged(
                mode: Int
            ) {
                repeatMode = mode
            }

            override fun onShuffleModeEnabledChanged(
                shuffleModeEnabledNow: Boolean
            ) {
                shuffleEnabled = shuffleModeEnabledNow

                /*
                 * Al activar/desactivar Shuffle cambia la
                 * navegación de la cola.
                 */
                updateCurrentQueue()
            }
        }

    /*
     * ============================================================
     * ON CREATE
     * ============================================================
     */

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        favoriteIds =
            FavoritesManager.getFavorites(this)

        setContent {

            var showFullPlayer by remember {
                mutableStateOf(false)
            }

            /*
             * Actualización ligera del progreso.
             *
             * 500 ms es suficiente para que el slider y el
             * contador se vean fluidos sin hacer trabajo
             * excesivo en equipos modestos.
             */
            LaunchedEffect(Unit) {

                while (true) {

                    updateProgress()

                    delay(500)
                }
            }

            if (
                showFullPlayer &&
                currentSong != null
            ) {

                FullPlayerScreen(
                    song = currentSong!!,

                    isPlaying = isPlaying,

                    currentPosition = currentPosition,

                    duration = duration,

                    repeatMode = repeatMode,

                    shuffleEnabled = shuffleEnabled,

                    isFavorite =
                        favoriteIds.contains(
                            currentSong!!.id
                        ),

                    queue = currentQueue,

                    onBack = {
                        showFullPlayer = false
                    },

                    onPlayPause = {
                        playPause()
                    },

                    onNext = {
                        nextSong()
                    },

                    onPrevious = {
                        previousSong()
                    },

                    onSeek = { position ->

                        controller?.seekTo(position)

                        updateProgress()
                    },

                    onShuffle = {
                        toggleShuffle()
                    },

                    onRepeat = {
                        toggleRepeat()
                    },

                    onFavorite = {

                        currentSong?.let {
                            toggleFavorite(it)
                        }
                    },

                    onQueueSongClick = { queueSong ->

                        playSongFromCurrentQueue(
                            queueSong
                        )
                    }
                )

            } else {

                BlackWalkMusicScreen(
                    songs = songs,

                    currentSong = currentSong,

                    isPlaying = isPlaying,

                    favoriteIds = favoriteIds,

                    onSongClick = {
                        playSong(it)
                    },

                    onPlayPause = {
                        playPause()
                    },

                    onNext = {
                        nextSong()
                    },

                    onPrevious = {
                        previousSong()
                    },

                    onShuffle = {
                        shuffleSongs()
                    },

                    onOpenPlayer = {

                        if (currentSong != null) {
                            showFullPlayer = true
                        }
                    },

                    onFavorite = {
                        toggleFavorite(it)
                    }
                )
            }
        }

        requestMusicPermission()

        connectToMusicService()
    }

    /*
     * ============================================================
     * MEDIA CONTROLLER
     * ============================================================
     */

    private fun connectToMusicService() {

        val sessionToken =
            SessionToken(
                this,
                ComponentName(
                    this,
                    MusicService::class.java
                )
            )

        controllerFuture =
            MediaController.Builder(
                this,
                sessionToken
            ).buildAsync()

        controllerFuture.addListener(
            {

                controller =
                    controllerFuture.get()

                controller?.addListener(
                    playerListener
                )

                syncPlayerState()

            },
            ContextCompat.getMainExecutor(this)
        )
    }

    /*
     * ============================================================
     * SINCRONIZACIÓN CENTRAL
     * ============================================================
     *
     * TODAS las modificaciones importantes del reproductor
     * pasan por aquí.
     *
     * Esto evita que:
     *
     * - currentSong diga una canción
     * - Media3 reproduzca otra
     * - la cola muestre otra
     *
     * Todo se actualiza desde el MediaController real.
     */

    private fun syncPlayerState() {

        val mediaController =
            controller ?: return

        /*
         * Estado de reproducción.
         */
        isPlaying =
            mediaController.isPlaying

        /*
         * Modos.
         */
        repeatMode =
            mediaController.repeatMode

        shuffleEnabled =
            mediaController.shuffleModeEnabled

        /*
         * Canción realmente reproducida.
         */
        val mediaItem =
            mediaController.currentMediaItem

        val currentUri =
            mediaItem
                ?.localConfiguration
                ?.uri
                ?.toString()

        val newSong =
            songs.firstOrNull {
                it.uri == currentUri
            }

        /*
         * IMPORTANTE:
         *
         * Actualizamos currentSong incluso cuando cambia
         * automáticamente por transición.
         */
        if (newSong?.id != currentSong?.id) {

            currentSong = newSong
        }

        /*
         * Cola.
         */
        updateCurrentQueue()

        /*
         * Tiempo.
         */
        updateProgress()
    }

    /*
     * ============================================================
     * COLA ACTUAL
     * ============================================================
     *
     * No existe getCurrentQueue().
     *
     * La lista se construye únicamente desde los MediaItems
     * que realmente tiene Media3.
     */

    private fun updateCurrentQueue() {

        val mediaController =
            controller ?: return

        if (songs.isEmpty()) {

            currentQueue = emptyList()

            return
        }

        val count =
            mediaController.mediaItemCount

        if (count <= 0) {

            currentQueue = emptyList()

            return
        }

        /*
         * Índice rápido URI -> Song.
         */
        val songsByUri =
            songs.associateBy {
                it.uri
            }

        val result =
            ArrayList<Song>(count)

        /*
         * IMPORTANTE:
         *
         * Aquí usamos el orden real de la playlist que
         * mantiene Media3.
         *
         * No intentamos crear nuestro propio shuffle.
         */
        for (index in 0 until count) {

            val mediaItem =
                mediaController.getMediaItemAt(index)

            val uri =
                mediaItem
                    .localConfiguration
                    ?.uri
                    ?.toString()
                    ?: continue

            songsByUri[uri]?.let {
                result.add(it)
            }
        }

        currentQueue = result
    }

    /*
     * ============================================================
     * PROGRESO
     * ============================================================
     */

    private fun updateProgress() {

        val mediaController =
            controller ?: return

        currentPosition =
            mediaController.currentPosition
                .coerceAtLeast(0L)

        duration =
            mediaController.duration
                .takeIf {
                    it > 0L
                }
                ?: currentSong?.duration
                ?: 0L
    }

    /*
     * ============================================================
     * REPRODUCIR UNA CANCIÓN
     * ============================================================
     */

    private fun playSong(
        song: Song
    ) {

        val mediaController =
            controller ?: return

        if (songs.isEmpty()) {
            return
        }

        val mediaItems =
            songs.map {
                MediaItem.fromUri(it.uri)
            }

        val startIndex =
            songs.indexOfFirst {
                it.uri == song.uri
            }.coerceAtLeast(0)

        /*
         * Playlist normal.
         */
        mediaController.setMediaItems(
            mediaItems,
            startIndex,
            0L
        )

        /*
         * Al seleccionar una canción manualmente,
         * comenzamos sin shuffle.
         *
         * El usuario puede activarlo después.
         */
        mediaController.shuffleModeEnabled =
            false

        mediaController.prepare()

        mediaController.play()

        /*
         * Sincronización inmediata.
         *
         * No esperamos al siguiente frame.
         */
        syncPlayerState()
    }

    /*
     * ============================================================
     * REPRODUCIR DESDE COLA
     * ============================================================
     */

    private fun playSongFromCurrentQueue(
        song: Song
    ) {

        val mediaController =
            controller ?: return

        val count =
            mediaController.mediaItemCount

        if (count <= 0) {
            return
        }

        /*
         * Buscamos la canción dentro de la playlist REAL
         * de Media3.
         */
        for (index in 0 until count) {

            val mediaItem =
                mediaController.getMediaItemAt(index)

            val uri =
                mediaItem
                    .localConfiguration
                    ?.uri
                    ?.toString()

            if (uri == song.uri) {

                mediaController.seekTo(
                    index,
                    0L
                )

                mediaController.play()

                /*
                 * Actualización inmediata.
                 */
                syncPlayerState()

                return
            }
        }
    }

    /*
     * ============================================================
     * PLAY / PAUSE
     * ============================================================
     */

    private fun playPause() {

        val mediaController =
            controller ?: return

        if (mediaController.isPlaying) {

            mediaController.pause()

        } else {

            mediaController.play()
        }

        /*
         * Reflejar inmediatamente el estado.
         */
        isPlaying =
            mediaController.isPlaying
    }

    /*
     * ============================================================
     * SIGUIENTE
     * ============================================================
     *
     * Media3 decide automáticamente cuál es el siguiente
     * elemento cuando Shuffle está activo.
     */

    private fun nextSong() {

        val mediaController =
            controller ?: return

        if (
            mediaController.hasNextMediaItem()
        ) {

            mediaController.seekToNextMediaItem()

            mediaController.play()

        } else if (
            mediaController.repeatMode ==
            Player.REPEAT_MODE_ALL
        ) {

            /*
             * Media3 normalmente gestiona esto,
             * pero mantenemos una protección.
             */
            mediaController.seekToNextMediaItem()

            mediaController.play()
        }

        /*
         * La transición real volverá a llamar
         * onMediaItemTransition().
         */
        syncPlayerState()
    }

    /*
     * ============================================================
     * ANTERIOR
     * ============================================================
     */

    private fun previousSong() {

        val mediaController =
            controller ?: return

        /*
         * Si llevamos más de 3 segundos,
         * volvemos al inicio de la canción.
         */
        if (
            mediaController.currentPosition >
            3000L
        ) {

            mediaController.seekTo(0L)

            syncPlayerState()

            return
        }

        if (
            mediaController.hasPreviousMediaItem()
        ) {

            mediaController.seekToPreviousMediaItem()

            mediaController.play()
        }

        syncPlayerState()
    }

    /*
     * ============================================================
     * SHUFFLE
     * ============================================================
     *
     * IMPORTANTE:
     *
     * Ya NO reconstruimos la playlist con songs.shuffled().
     *
     * Media3 ya posee un ShuffleOrder propio.
     *
     * Esto evita que la cola visual se desincronice.
     */

    private fun shuffleSongs() {

        val mediaController =
            controller ?: return

        if (songs.isEmpty()) {
            return
        }

        /*
         * Si no existe una playlist,
         * creamos una normal.
         */
        if (
            mediaController.mediaItemCount <= 0
        ) {

            val mediaItems =
                songs.map {
                    MediaItem.fromUri(it.uri)
                }

            mediaController.setMediaItems(
                mediaItems,
                0,
                0L
            )

            mediaController.prepare()
        }

        /*
         * Activamos el shuffle nativo de Media3.
         */
        mediaController.shuffleModeEnabled =
            true

        shuffleEnabled = true

        mediaController.play()

        syncPlayerState()
    }

    /*
     * ============================================================
     * TOGGLE SHUFFLE
     * ============================================================
     */

    private fun toggleShuffle() {

        val mediaController =
            controller ?: return

        val newValue =
            !mediaController.shuffleModeEnabled

        mediaController.shuffleModeEnabled =
            newValue

        shuffleEnabled = newValue

        /*
         * No modificamos la playlist.
         *
         * Media3 conserva el elemento actual y cambia
         * solamente la navegación aleatoria.
         */
        updateCurrentQueue()

        syncPlayerState()
    }

    /*
     * ============================================================
     * REPEAT
     * ============================================================
     */

    private fun toggleRepeat() {

        val mediaController =
            controller ?: return

        val newMode =
            when (
                mediaController.repeatMode
            ) {

                Player.REPEAT_MODE_OFF ->
                    Player.REPEAT_MODE_ALL

                Player.REPEAT_MODE_ALL ->
                    Player.REPEAT_MODE_ONE

                else ->
                    Player.REPEAT_MODE_OFF
            }

        mediaController.repeatMode =
            newMode

        repeatMode =
            newMode
    }

    /*
     * ============================================================
     * FAVORITOS
     * ============================================================
     */

    private fun toggleFavorite(
        song: Song
    ) {

        FavoritesManager.toggleFavorite(
            this,
            song.id
        )

        favoriteIds =
            FavoritesManager.getFavorites(
                this
            )
    }

    /*
     * ============================================================
     * PERMISOS
     * ============================================================
     */

    private fun requestMusicPermission() {

        val permissions =
            if (
                android.os.Build.VERSION.SDK_INT >= 33
            ) {

                arrayOf(
                    android.Manifest.permission.READ_MEDIA_AUDIO,
                    android.Manifest.permission.POST_NOTIFICATIONS
                )

            } else {

                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }

        permissionLauncher.launch(
            permissions
        )
    }

    /*
     * ============================================================
     * CARGAR MÚSICA
     * ============================================================
     */

    private fun loadMusic() {

        songs =
            MusicRepository.getSongs(
                contentResolver
            )

        favoriteIds =
            FavoritesManager.getFavorites(
                this
            )

        /*
         * Si el MediaController ya está conectado,
         * intentamos recuperar inmediatamente la canción
         * que estaba reproduciendo.
         */
        syncPlayerState()
    }

    /*
     * ============================================================
     * DESTROY
     * ============================================================
     */

    override fun onDestroy() {

        controller?.removeListener(
            playerListener
        )

        if (
            ::controllerFuture.isInitialized
        ) {

            MediaController.releaseFuture(
                controllerFuture
            )
        }

        controller = null

        super.onDestroy()
    }
}
