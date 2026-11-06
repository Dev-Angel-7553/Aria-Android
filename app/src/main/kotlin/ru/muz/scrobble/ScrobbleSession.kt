package ru.muz.scrobble

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
internal enum class ScrobbleService(val title: String) { LASTFM("Last.fm"), LISTENBRAINZ("ListenBrainz") }

@Serializable
internal data class ScrobbleTrack(
    val id: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val album: String = "",
    val artists: List<String> = emptyList(),
)

@Serializable
internal data class ScrobblePlay(val id: String, val track: ScrobbleTrack, val startedAt: Long)

internal sealed interface ScrobbleEvent {
    data class NowPlaying(val play: ScrobblePlay) : ScrobbleEvent
    data class Submit(val play: ScrobblePlay) : ScrobbleEvent
}

internal class ScrobbleSession(private val newId: () -> String = { UUID.randomUUID().toString() }) {
    private var track: ScrobbleTrack? = null
    private var play: ScrobblePlay? = null
    private var listenedMs = 0L
    private var submitted = false
    private var suppressed = false

    fun start(value: ScrobbleTrack?) {
        track = value
        play = null
        listenedMs = 0
        submitted = false
        suppressed = false
    }

    fun suppress() { suppressed = true }

    fun tick(deltaMs: Long, durationMs: Long, epochSeconds: Long, privateSession: Boolean): List<ScrobbleEvent> {
        if (privateSession) suppress()
        if (suppressed) return emptyList()
        val value = track ?: return emptyList()
        if (value.title.isBlank() || value.artist.isBlank()) return emptyList()
        val events = mutableListOf<ScrobbleEvent>()
        if (play == null) {
            play = ScrobblePlay(newId(), value, epochSeconds - deltaMs.coerceIn(0, 5_000) / 1000)
            events += ScrobbleEvent.NowPlaying(play!!)
        }
        val duration = durationMs.takeIf { it > 0 } ?: value.durationMs
        listenedMs += deltaMs.coerceIn(0, 5_000)
        if (!submitted && duration > 30_000 && listenedMs >= minOf(duration / 2, 240_000)) {
            submitted = true
            events += ScrobbleEvent.Submit(play!!.copy(track = value.copy(durationMs = duration)))
        }
        return events
    }
}
