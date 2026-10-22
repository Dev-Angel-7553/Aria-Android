package ru.muz.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.muz.yandex.YandexClient

class LyricsRepository(
    private val yandex: YandexClient,
    private val http: OkHttpClient = OkHttpClient(),
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = object : LinkedHashMap<String, Lyrics>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Lyrics>?) = size > 30
    }

    suspend fun load(
        track: YandexClient.Track,
        refresh: Boolean = false,
        onStage: (String) -> Unit = {},
    ): Result<Lyrics> = runCatching {
        if (!refresh) cache[track.id]?.let { return@runCatching it }
        onStage("Проверяем Яндекс Музыку…")
        val yandexResult = yandex.lyrics(track)
        yandexResult.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        val yandexText = yandexResult.getOrNull()
        currentCoroutineContext().ensureActive()
        if (!yandexText.isNullOrBlank()) {
            LrcParser.parse(yandexText).takeIf { it.lines.any { line -> line.text.isNotBlank() } }?.let {
                cache[track.id] = it
                return@runCatching it
            }
        }

        onStage("Ищем в LRCLIB…")
        val result = searchLrcLib(track) ?: error("Текст для этого трека не найден")
        val synced = result["syncedLyrics"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val plain = result["plainLyrics"]?.jsonPrimitive?.contentOrNull.orEmpty()
        LrcParser.parse(synced.ifBlank { plain }).takeIf { it.lines.isNotEmpty() }
            ?: error("Текст для этого трека пуст")
    }.onSuccess {
        cache[track.id] = it
    }.onFailure {
        if (it is CancellationException) throw it
    }

    private suspend fun searchLrcLib(track: YandexClient.Track): JsonObject? = withContext(Dispatchers.IO) {
        val exactUrl = "https://lrclib.net/api/get".toHttpUrl().newBuilder()
            .addQueryParameter("track_name", clean(track.title))
            .addQueryParameter("artist_name", track.artist)
            .apply {
                if (track.album.isNotBlank()) addQueryParameter("album_name", track.album)
                if (track.durationMs > 0) addQueryParameter("duration", (track.durationMs / 1000).toString())
            }
            .build()
        request(exactUrl.toString())?.let { body ->
            val exact = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
            if (exact != null && exact.hasText()) return@withContext exact
        }

        val searchUrl = "https://lrclib.net/api/search".toHttpUrl().newBuilder()
            .addQueryParameter("track_name", clean(track.title))
            .addQueryParameter("artist_name", track.artist)
            .build()
        val array = request(searchUrl.toString())?.let { body ->
            runCatching { json.parseToJsonElement(body) as? JsonArray }.getOrNull()
        }
        array?.map { it.jsonObject }?.filter { it.hasText() }?.minByOrNull {
            val duration = it["duration"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0
            kotlin.math.abs(duration - track.durationMs / 1000.0)
        }
    }

    private fun JsonObject.hasText(): Boolean =
        !this["syncedLyrics"]?.jsonPrimitive?.contentOrNull.isNullOrBlank() ||
            !this["plainLyrics"]?.jsonPrimitive?.contentOrNull.isNullOrBlank()

    private fun request(url: String): String? {
        val request = Request.Builder().url(url).header("User-Agent", "Aria/0.18 (Android)").build()
        return http.newCall(request).execute().use { response ->
            if (response.isSuccessful) response.body?.string() else null
        }
    }

    private fun clean(value: String): String = value
        .replace(Regex("\\s*[\\[(].*?[\\])]\\s*"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
