package ru.muz.player

import kotlinx.serialization.json.*
import ru.muz.yandex.PlaySource

internal data class ResumeSnapshot(
    val tracks: List<PlaybackController.YandexTrack>,
    val sources: List<PlaySource>,
    val index: Int,
    val positionMs: Long,
)

internal object ResumeCodec {
    fun encode(snapshot: ResumeSnapshot): String = buildJsonObject {
        put("index", snapshot.index.coerceAtLeast(0))
        put("position", snapshot.positionMs.coerceAtLeast(0))
        put("items", buildJsonArray {
            snapshot.tracks.forEachIndexed { index, track ->
                val source = snapshot.sources.getOrNull(index) ?: PlaySource.other()
                add(buildJsonObject {
                    put("id", track.id)
                    put("title", track.title)
                    put("artist", track.artist)
                    put("cover", track.coverUrl.orEmpty())
                    put("album", track.albumId)
                    put("duration", track.durationMs)
                    put("liked", track.liked)
                    if (!track.loudness.isNaN()) put("loudness", track.loudness)
                    put("artists", ArtistMetadata.encode(track.artists))
                    put("from", source.from)
                    put("playlist", source.playlistId)
                    put("wave", source.wave)
                    put("session", source.sessionId)
                    put("station", source.station)
                })
            }
        })
    }.toString()

    fun decode(raw: String): ResumeSnapshot? = runCatching {
        val root = Json.parseToJsonElement(raw).jsonObject
        val items = (root["items"] as? JsonArray).orEmpty().map { it.jsonObject }
        fun JsonObject.text(key: String) = this[key]?.jsonPrimitive?.content.orEmpty()
        if (items.isEmpty() || items.any { it.text("id").isBlank() }) return null
        ResumeSnapshot(
            tracks = items.map {
                PlaybackController.YandexTrack(it.text("id"), it.text("title"), it.text("artist"),
                    it.text("cover").ifBlank { null }, it.text("album"), it.text("duration").toLongOrNull() ?: 0L,
                    it.text("liked") == "true", it.text("loudness").toFloatOrNull() ?: Float.NaN,
                    ArtistMetadata.decode(it.text("artists")))
            },
            sources = items.map {
                PlaySource(from = it.text("from").ifBlank { PlaySource.other().from }, playlistId = it.text("playlist"),
                    wave = it.text("wave") == "true", station = it.text("station").ifBlank { PlaySource.WAVE_STATION },
                    sessionId = it.text("session").ifBlank { PlaySource.other().sessionId })
            },
            index = (root.text("index").toIntOrNull() ?: 0).coerceIn(items.indices),
            positionMs = (root.text("position").toLongOrNull() ?: 0L).coerceAtLeast(0),
        )
    }.getOrNull()
}
