package ru.muz.lyrics

import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.muz.net.readText
import ru.muz.yandex.YandexClient

internal class InternetLyrics(private val http: OkHttpClient) {
    suspend fun find(track: YandexClient.Track): String? = withContext(Dispatchers.IO) {
        val url = "https://api.lrcmux.dev/get".toHttpUrl().newBuilder()
            .addQueryParameter("title", track.title)
            .addQueryParameter("artist", track.artists.firstOrNull()?.name ?: track.artist)
            .addQueryParameter("level", "line").addQueryParameter("format", "json")
            .apply {
                if (track.album.isNotBlank()) addQueryParameter("album", track.album)
                if (track.durationMs > 0) addQueryParameter("duration", (track.durationMs / 1000).toString())
            }.build()
        val response = http.newCall(Request.Builder().url(url).header("User-Agent", "Aria Android").build()).readText()
        when {
            response.code == 404 -> null
            !response.isSuccessful -> throw IOException("Резервный поиск временно недоступен")
            else -> decode(response.body, track)
        }
    }

    companion object {
        fun decode(body: String, track: YandexClient.Track): String? {
            val obj = Json.parseToJsonElement(body).jsonObject
            val metadata = obj["track"] as? JsonObject ?: return null
            fun field(key: String) = metadata[key]?.jsonPrimitive?.contentOrNull.orEmpty()
            val title = field("title")
            val artist = field("artist")
            if (normalized(title) != normalized(track.title)) return null
            val artists = (track.artists.map { it.name } + track.artist).filter { it.isNotBlank() }
            if (artists.none { normalized(it) == normalized(artist) }) return null
            val duration = field("duration").toDoubleOrNull() ?: 0.0
            if (track.durationMs > 0 && duration > 0 && kotlin.math.abs(duration - track.durationMs / 1000.0) > 12) return null
            val meta = obj["meta"] as? JsonObject
            if (meta?.get("instrumental")?.jsonPrimitive?.booleanOrNull == true) return null
            val synced = meta?.get("level")?.jsonPrimitive?.contentOrNull in setOf("line", "word")
            val lines = obj["lines"] as? JsonArray ?: return null
            val rendered = lines.mapNotNull { element ->
                val line = element as? JsonObject ?: return@mapNotNull null
                val text = line["text"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val start = (line["start"]?.jsonPrimitive?.longOrNull ?: 0L).coerceAtLeast(0)
                if (synced) String.format(Locale.ROOT, "[%02d:%02d.%03d]%s", start / 60000, start / 1000 % 60, start % 1000, text)
                else text
            }.joinToString("\n")
            return rendered.takeIf { LrcParser.parse(it).lines.any { line -> line.text.isNotBlank() } }
        }

        private fun normalized(value: String): String = value.lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
    }
}
