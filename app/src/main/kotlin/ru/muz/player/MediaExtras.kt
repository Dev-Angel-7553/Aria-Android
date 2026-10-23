package ru.muz.player

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import ru.muz.yandex.PlaySource
import ru.muz.yandex.YandexClient

internal object MediaExtras {
    const val ALBUM_ID = "aria.albumId"
    const val DURATION = "aria.durationMs"
    const val FROM = "aria.from"
    const val PLAYLIST = "aria.playlistId"
    const val WAVE = "aria.wave"
    const val SESSION = "aria.session"
    const val LIKED = "aria.liked"
    const val STATION = "aria.station"
    const val LOUDNESS = "aria.loudness"

    const val CMD_TOGGLE_LIKE = "aria.toggleLike"
    const val CMD_SET_LIKED = "aria.setLiked"
    const val CMD_LIKE_CHANGED = "aria.likeChanged"
    const val CMD_SLEEP = "aria.sleep"
    const val CMD_SLEEP_STATE = "aria.sleepState"
    const val CMD_SKIP_SILENCE = "aria.skipSilence"
    const val ARG_MINUTES = "minutes"
    const val ARG_ENDS_AT = "endsAt"
    const val ARG_ENABLED = "enabled"
    const val SLEEP_END_OF_TRACK = -1
    const val SLEEP_QUERY = -2
    const val ARG_TRACK = "track"
    const val ARG_LIKED = "liked"

    fun bundle(track: PlaybackController.YandexTrack, source: PlaySource): Bundle = Bundle().apply {
        putString(ALBUM_ID, track.albumId)
        putLong(DURATION, track.durationMs)
        putBoolean(LIKED, track.liked)
        if (!track.loudness.isNaN()) putFloat(LOUDNESS, track.loudness)
        putString(FROM, source.from)
        putString(PLAYLIST, source.playlistId)
        putBoolean(WAVE, source.wave)
        putString(SESSION, source.sessionId)
        putString(STATION, source.station)
    }

    fun source(extras: Bundle?): PlaySource {
        if (extras == null) return PlaySource.other()
        val session = extras.getString(SESSION).orEmpty()
        val base = PlaySource(
            from = extras.getString(FROM) ?: PlaySource.other().from,
            playlistId = extras.getString(PLAYLIST).orEmpty(),
            wave = extras.getBoolean(WAVE, false),
            station = extras.getString(STATION) ?: PlaySource.WAVE_STATION,
        )
        return if (session.isBlank()) base else base.copy(sessionId = session)
    }

    fun uri(trackId: String): Uri = Uri.parse("yamusic://track/$trackId")

    fun item(track: PlaybackController.YandexTrack, source: PlaySource, mediaId: String = track.id): MediaItem =
        MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(uri(track.id))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setArtworkUri(track.coverUrl?.let(Uri::parse))
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setExtras(bundle(track, source))
                    .build(),
            )
            .build()

    fun track(value: YandexClient.Track, liked: Boolean = false) = PlaybackController.YandexTrack(
        id = value.id,
        title = value.title,
        artist = value.artist,
        coverUrl = value.coverUrl,
        albumId = value.albumId,
        durationMs = value.durationMs,
        liked = liked,
        loudness = value.loudness,
    )

    fun trackId(item: MediaItem): String =
        item.localConfiguration?.uri?.takeIf { it.scheme == "yamusic" }?.lastPathSegment
            ?: item.mediaId.substringAfterLast('#')
}
