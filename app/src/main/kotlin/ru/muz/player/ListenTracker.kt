package ru.muz.player

import android.content.Context
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.muz.auth.TokenStore
import ru.muz.settings.AppPreferences
import ru.muz.stats.ListeningStats
import ru.muz.yandex.ListenEvent
import ru.muz.yandex.ListenSession
import ru.muz.yandex.YandexClient

internal class ListenTracker(context: Context, private val player: Player) : Player.Listener {
    private val app = context.applicationContext
    private val session = ListenSession()
    private val stats = ListeningStats.shared(app)
    private val main = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var ticker: Job? = null
    private var currentId = ""
    private var title = ""
    private var artist = ""
    private var client: YandexClient? = null
    private var clientToken: String? = null

    fun start() {
        player.addListener(this)
        open(player.currentMediaItem, force = true)
        updateTicker()
    }

    fun release() {
        ticker?.cancel()
        player.removeListener(this)
        send(session.finish())
        stats.flush()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        open(mediaItem, force = reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        updateTicker()
        if (!isPlaying) stats.flush()
    }

    private fun open(item: MediaItem?, force: Boolean) {
        val id = item?.let(MediaExtras::trackId).orEmpty()
        if (id == currentId && !force) return
        currentId = id
        if (id.isBlank() || item == null) {
            send(session.finish())
            return
        }
        val metadata = item.mediaMetadata
        val extras = metadata.extras
        title = metadata.title?.toString().orEmpty()
        artist = metadata.artist?.toString().orEmpty()
        val duration = extras?.getLong(MediaExtras.DURATION, 0L)?.takeIf { it > 0 }
            ?: player.duration.takeIf { it > 0 } ?: 0L
        send(
            session.start(
                trackId = id,
                albumId = extras?.getString(MediaExtras.ALBUM_ID).orEmpty(),
                durationMs = duration,
                source = MediaExtras.source(extras),
            ),
        )
        stats.recordStart(id, title, artist, metadata.artworkUri?.toString())
    }

    private fun updateTicker() {
        ticker?.cancel()
        if (!player.isPlaying) return
        ticker = main.launch {
            var last = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(1_000)
                val now = SystemClock.elapsedRealtime()
                val delta = (now - last).coerceIn(0, 5_000)
                last = now
                if (currentId.isBlank()) continue
                stats.addListening(delta, currentId, title, artist)
                send(session.tick(delta, player.currentPosition, player.duration.coerceAtLeast(0)))
            }
        }
    }

    private fun send(events: List<ListenEvent>) {
        if (events.isEmpty()) return
        if (!AppPreferences(app).load().reportListening) return
        val token = TokenStore(app).token ?: return
        val api = client?.takeIf { clientToken == token } ?: YandexClient(token).also {
            client = it
            clientToken = token
        }
        io.launch {
            events.forEach { event ->
                when (event) {
                    is ListenEvent.PlayAudio -> api.reportPlay(event)
                    else -> api.waveFeedback(event)
                }
            }
        }
    }
}
