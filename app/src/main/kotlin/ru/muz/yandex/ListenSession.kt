package ru.muz.yandex

import java.util.UUID

data class PlaySource(
    val from: String,
    val playlistId: String = "",
    val wave: Boolean = false,
    val sessionId: String = UUID.randomUUID().toString(),
) {
    companion object {
        const val WAVE_STATION = "user:onyourwave"

        fun liked() = PlaySource("mobile-own_tracks-default")
        fun wave() = PlaySource("mobile-radio-user-onyourwave", wave = true)
        fun chart() = PlaySource("mobile-chart-default")
        fun search() = PlaySource("mobile-search-default")
        fun album() = PlaySource("mobile-album-default")
        fun artist() = PlaySource("mobile-artist-default")
        fun playlist(ownerUid: String, kind: String) = PlaySource(
            from = "mobile-playlist-default",
            playlistId = if (ownerUid.isNotBlank() && kind.isNotBlank()) "$ownerUid:$kind" else "",
        )
        fun other() = PlaySource("mobile-other-default")
    }
}

sealed interface ListenEvent {
    data class WaveStarted(val from: String) : ListenEvent

    data class WaveTrackStarted(val trackId: String, val albumId: String) : ListenEvent

    data class WaveTrackEnded(
        val trackId: String,
        val albumId: String,
        val playedSeconds: Double,
        val skipped: Boolean,
    ) : ListenEvent

    data class PlayAudio(
        val trackId: String,
        val albumId: String,
        val from: String,
        val playlistId: String,
        val playId: String,
        val trackLengthSeconds: Double,
        val playedSeconds: Double,
        val endPositionSeconds: Double,
    ) : ListenEvent
}

class ListenSession(
    private val minReportMs: Long = 30_000,
    private val newPlayId: () -> String = { UUID.randomUUID().toString() },
) {
    private class Current(
        val trackId: String,
        val albumId: String,
        var durationMs: Long,
        val source: PlaySource,
        val playId: String,
        var playedMs: Long = 0,
        var positionMs: Long = 0,
    )

    private var current: Current? = null
    private var announcedWaveSession: String? = null

    val currentTrackId: String? get() = current?.trackId

    fun start(trackId: String, albumId: String, durationMs: Long, source: PlaySource): List<ListenEvent> {
        val events = finish().toMutableList()
        val numericId = trackId.substringBefore(':')
        if (numericId.isBlank()) return events
        val album = albumId.ifBlank { trackId.substringAfter(':', "") }
        if (source.wave && announcedWaveSession != source.sessionId) {
            announcedWaveSession = source.sessionId
            events += ListenEvent.WaveStarted(source.from)
        }
        current = Current(numericId, album, durationMs.coerceAtLeast(0), source, newPlayId())
        if (source.wave) events += ListenEvent.WaveTrackStarted(numericId, album)
        return events
    }

    fun tick(playedDeltaMs: Long, positionMs: Long, durationMs: Long = 0): List<ListenEvent> {
        val track = current ?: return emptyList()
        if (durationMs > 0) track.durationMs = durationMs
        val loopedBack = track.durationMs > 0 &&
            track.positionMs >= track.durationMs - END_TOLERANCE_MS &&
            positionMs < LOOP_START_MS
        if (loopedBack) {
            val restarted = start(track.trackId, track.albumId, track.durationMs, track.source)
            current?.let {
                it.positionMs = positionMs.coerceAtLeast(0)
                it.playedMs = playedDeltaMs.coerceAtLeast(0)
            }
            return restarted
        }
        track.playedMs += playedDeltaMs.coerceAtLeast(0)
        track.positionMs = positionMs.coerceAtLeast(0)
        return emptyList()
    }

    fun finish(): List<ListenEvent> {
        val track = current ?: return emptyList()
        current = null
        val reachedEnd = track.durationMs > 0 && (
            track.positionMs >= track.durationMs - END_TOLERANCE_MS ||
                track.playedMs >= track.durationMs * 9 / 10
            )
        val events = mutableListOf<ListenEvent>()
        if (track.playedMs >= minReportMs || (reachedEnd && track.playedMs > 0)) {
            events += ListenEvent.PlayAudio(
                trackId = track.trackId,
                albumId = track.albumId,
                from = track.source.from,
                playlistId = track.source.playlistId,
                playId = track.playId,
                trackLengthSeconds = track.durationMs / 1000.0,
                playedSeconds = track.playedMs / 1000.0,
                endPositionSeconds = track.positionMs / 1000.0,
            )
        }
        if (track.source.wave) {
            events += ListenEvent.WaveTrackEnded(
                trackId = track.trackId,
                albumId = track.albumId,
                playedSeconds = track.playedMs / 1000.0,
                skipped = !reachedEnd,
            )
        }
        return events
    }

    private companion object {
        const val END_TOLERANCE_MS = 4_000L
        const val LOOP_START_MS = 3_000L
    }
}
