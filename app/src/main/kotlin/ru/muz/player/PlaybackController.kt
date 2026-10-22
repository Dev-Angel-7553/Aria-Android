package ru.muz.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import android.os.Bundle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import ru.muz.yandex.PlaySource
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackController(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state
    private var controller: MediaController? = null
    private var pendingQueue: Triple<List<YandexTrack>, Int, PlaySource>? = null
    private var currentSource = PlaySource.other()
    private val _likeChanges = MutableSharedFlow<Pair<String, Boolean>>(extraBufferCapacity = 16)
    val likeChanges: SharedFlow<Pair<String, Boolean>> = _likeChanges
    private val _sleepEndsAt = MutableStateFlow(0L)
    val sleepEndsAt: StateFlow<Long> = _sleepEndsAt
    private val sessionListener = object : MediaController.Listener {
        override fun onCustomCommand(
            controller: MediaController,
            command: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (command.customAction == MediaExtras.CMD_SLEEP_STATE) {
                _sleepEndsAt.value = args.getLong(MediaExtras.ARG_ENDS_AT, 0L)
            }
            if (command.customAction == MediaExtras.CMD_LIKE_CHANGED) {
                val id = args.getString(MediaExtras.ARG_TRACK).orEmpty()
                if (id.isNotBlank()) _likeChanges.tryEmit(id to args.getBoolean(MediaExtras.ARG_LIKED, false))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }
    private var positionJob: Job? = null
    private var playbackError = ""
    private var discontinuity = 0L
    private var released = false

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
        }

        override fun onPlayerError(error: PlaybackException) {
            playbackError = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                    "Нет соединения с Яндекс Музыкой"
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                    "Яндекс отклонил аудиопоток. Попробуйте запустить трек ещё раз"
                else -> "Не удалось воспроизвести трек"
            }
            controller?.let(::publish)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            playbackError = ""
        }

        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
            discontinuity++
        }
    }

    init {
        val token = SessionToken(context, ComponentName(context, MuzPlayerService::class.java))
        val future = MediaController.Builder(context, token).setListener(sessionListener).buildAsync()
        future.addListener({
            runCatching { future.get() }.onSuccess { connected ->
                if (released) {
                    connected.release()
                    return@onSuccess
                }
                controller = connected
                connected.addListener(listener)
                publish(connected)
                sendSleep(MediaExtras.SLEEP_QUERY)
                pendingQueue?.also { (tracks, index, source) ->
                    pendingQueue = null
                    playQueue(tracks, index, source)
                }
                positionJob = scope.launch {
                    while (isActive) {
                        publish(connected)
                        delay(250)
                    }
                }
            }
        }, MoreExecutors.directExecutor())
    }

    fun play(track: YandexTrack) {
        playQueue(listOf(track), 0)
    }

    fun playQueue(tracks: List<YandexTrack>, startIndex: Int, source: PlaySource = PlaySource.other()) {
        if (tracks.isEmpty()) return
        playbackError = ""
        currentSource = source
        val activeController = controller
        if (activeController == null) {
            pendingQueue = Triple(tracks, startIndex.coerceIn(tracks.indices), source)
            return
        }
        val items = tracks.map(::mediaItem)
        activeController.setMediaItems(items, startIndex.coerceIn(items.indices), 0L)
        activeController.prepare()
        activeController.play()
        val track = tracks[startIndex.coerceIn(tracks.indices)]
        _state.value = PlayerState(
            mediaId = track.id,
            title = track.title,
            artist = track.artist,
            coverUrl = track.coverUrl,
            isBuffering = true,
            playWhenReady = true,
            discontinuity = discontinuity,
        )
    }

    fun togglePlayPause() {
        controller?.let {
            if (it.playerError == null && it.playWhenReady && it.playbackState != Player.STATE_ENDED) it.pause() else {
                playbackError = ""
                if (it.playbackState == Player.STATE_IDLE) it.prepare()
                if (it.playbackState == Player.STATE_ENDED) it.seekToDefaultPosition()
                it.play()
            }
            publish(it)
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.let {
            it.seekTo(positionMs.coerceIn(0, it.duration.takeIf { duration -> duration > 0 } ?: Long.MAX_VALUE))
            publish(it)
        }
    }
    fun skipNext() { controller?.seekToNextMediaItem() }
    fun skipPrevious() { controller?.seekToPreviousMediaItem() }
    fun skipTo(index: Int) {
        controller?.takeIf { index in 0 until it.mediaItemCount }?.seekToDefaultPosition(index)
    }
    fun playNext(track: YandexTrack) {
        controller?.let { it.addMediaItem((it.currentMediaItemIndex + 1).coerceAtLeast(0), mediaItem(track)) }
    }
    fun addToQueue(track: YandexTrack) { controller?.addMediaItem(mediaItem(track)) }
    fun setLiked(trackId: String, liked: Boolean) {
        val args = Bundle().apply {
            putString(MediaExtras.ARG_TRACK, trackId)
            putBoolean(MediaExtras.ARG_LIKED, liked)
        }
        controller?.sendCustomCommand(SessionCommand(MediaExtras.CMD_SET_LIKED, Bundle.EMPTY), args)
    }

    fun setSleepTimer(minutes: Int) = sendSleep(minutes)

    private fun sendSleep(minutes: Int) {
        val args = Bundle().apply { putInt(MediaExtras.ARG_MINUTES, minutes) }
        controller?.sendCustomCommand(SessionCommand(MediaExtras.CMD_SLEEP, Bundle.EMPTY), args)
    }

    fun setSpeed(speed: Float) {
        controller?.let {
            it.setPlaybackSpeed(speed.coerceIn(0.5f, 2f))
            publish(it)
        }
    }

    fun setSkipSilence(enabled: Boolean) {
        val args = Bundle().apply { putBoolean(MediaExtras.ARG_ENABLED, enabled) }
        controller?.sendCustomCommand(SessionCommand(MediaExtras.CMD_SKIP_SILENCE, Bundle.EMPTY), args)
    }

    fun setShuffle(enabled: Boolean) { controller?.shuffleModeEnabled = enabled }
    fun setRepeat(mode: RepeatMode) {
        controller?.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    fun release() {
        released = true
        positionJob?.cancel()
        positionJob = null
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        pendingQueue = null
        scope.cancel()
    }

    private fun publish(player: androidx.media3.common.Player) {
        val metadata = player.mediaMetadata
        _state.value = PlayerState(
            mediaId = player.currentMediaItem?.mediaId.orEmpty(),
            title = metadata.title?.toString().orEmpty(),
            artist = metadata.artist?.toString().orEmpty(),
            coverUrl = metadata.artworkUri?.toString(),
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.takeIf { it > 0 } ?: 0,
            isPlaying = player.isPlaying,
            repeat = when (player.repeatMode) {
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                else -> RepeatMode.OFF
            },
            shuffle = player.shuffleModeEnabled,
            error = playbackError,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            playWhenReady = player.playWhenReady,
            bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0),
            sampledAtMs = android.os.SystemClock.elapsedRealtime(),
            playbackSpeed = player.playbackParameters.speed,
            discontinuity = discontinuity,
        )
    }

    private fun mediaItem(track: YandexTrack): MediaItem = MediaExtras.item(track, currentSource)

    data class YandexTrack(
        val id: String,
        val title: String,
        val artist: String,
        val coverUrl: String?,
        val albumId: String = "",
        val durationMs: Long = 0,
        val liked: Boolean = false,
    )
}
