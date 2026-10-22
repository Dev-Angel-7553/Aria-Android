package ru.muz.player

import android.content.Context
import androidx.media3.common.MediaItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import ru.muz.yandex.PlaySource

internal class ResumeStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("playback_resume", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    data class Snapshot(
        val tracks: List<PlaybackController.YandexTrack>,
        val sources: List<PlaySource>,
        val index: Int,
        val positionMs: Long,
    )

    fun save(items: List<MediaItem>, index: Int, positionMs: Long) {
        if (items.isEmpty() || index !in items.indices) return
        val from = (index - MAX_ITEMS / 2).coerceAtLeast(0)
        val window = items.subList(from, (from + MAX_ITEMS).coerceAtMost(items.size))
        val value = buildJsonObject {
            put("index", index - from)
            put("position", positionMs.coerceAtLeast(0))
            put("items", buildJsonArray {
                window.forEach { item ->
                    val metadata = item.mediaMetadata
                    val extras = metadata.extras
                    val source = MediaExtras.source(extras)
                    add(buildJsonObject {
                        put("id", MediaExtras.trackId(item))
                        put("title", metadata.title?.toString().orEmpty())
                        put("artist", metadata.artist?.toString().orEmpty())
                        put("cover", metadata.artworkUri?.toString().orEmpty())
                        put("album", extras?.getString(MediaExtras.ALBUM_ID).orEmpty())
                        put("duration", extras?.getLong(MediaExtras.DURATION, 0L) ?: 0L)
                        put("liked", extras?.getBoolean(MediaExtras.LIKED, false) ?: false)
                        put("from", source.from)
                        put("playlist", source.playlistId)
                        put("wave", source.wave)
                        put("session", source.sessionId)
                    })
                }
            })
        }
        prefs.edit().putString(KEY, value.toString()).apply()
    }

    fun savePosition(positionMs: Long) {
        val raw = prefs.getString(KEY, null) ?: return
        val root = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return
        val updated = JsonObject(root.toMutableMap().apply { put("position", kotlinx.serialization.json.JsonPrimitive(positionMs)) })
        prefs.edit().putString(KEY, updated.toString()).apply()
    }

    fun read(): Snapshot? = runCatching {
        val root = json.parseToJsonElement(prefs.getString(KEY, null) ?: return null).jsonObject
        val items = (root["items"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
        if (items.isEmpty()) return null
        fun JsonObject.text(key: String) = this[key]?.jsonPrimitive?.content.orEmpty()
        val tracks = items.map {
            PlaybackController.YandexTrack(
                id = it.text("id"),
                title = it.text("title"),
                artist = it.text("artist"),
                coverUrl = it.text("cover").ifBlank { null },
                albumId = it.text("album"),
                durationMs = it.text("duration").toLongOrNull() ?: 0L,
                liked = it.text("liked") == "true",
            )
        }
        val sources = items.map {
            PlaySource(
                from = it.text("from").ifBlank { PlaySource.other().from },
                playlistId = it.text("playlist"),
                wave = it.text("wave") == "true",
                sessionId = it.text("session").ifBlank { PlaySource.other().sessionId },
            )
        }
        Snapshot(
            tracks = tracks,
            sources = sources,
            index = (root["index"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0).coerceIn(tracks.indices),
            positionMs = root["position"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
        )
    }.getOrNull()

    private companion object {
        const val KEY = "queue"
        const val MAX_ITEMS = 100
    }
}
